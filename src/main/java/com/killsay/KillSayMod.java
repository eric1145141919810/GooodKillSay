package com.killsay;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Goood KillSay - Minecraft Fabric Mod
 * 一个用于在PVP服务器自动发送击杀消息和刷屏消息的模组
 * 
 * 功能特点：
 * - 刷屏模式：每15秒自动发送一条消息
 * - 击杀模式：仅在击杀时发送消息
 * - 混合模式：同时开启刷屏和击杀模式
 * - 自定义按键：可在游戏设置中自定义按键绑定
 * - 配置保存：自动保存开关状态和模式选择
 * - 多语言支持
 * - 灵活的击杀检测：通过配置文件支持多种服务器击杀格式
 */
public class KillSayMod implements ClientModInitializer {
    // 模组ID和名称
    public static final String MOD_ID = "killsay";
    public static final String MOD_NAME = "Goood KillSay";
    // 日志记录器
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    // 存储消息列表（从killsay.txt读取）
    private static final List<String> messages = new ArrayList<>();
    // 存储击杀模式列表（从killpatterns.json读取）
    private static final List<KillPattern> killPatterns = new ArrayList<>();
    // 随机数生成器
    private static final Random random = new Random();
    
    // 配置文件路径
    private static final File killsayFile = new File("killsay.txt");
    private static final File patternsFile = new File("killpatterns.json");
    private static final File configFile = new File("killsayconfig.json");
    
    // GSON用于JSON序列化/反序列化
    private static final Gson gson = new Gson();
    // Minecraft客户端实例
    private static MinecraftClient client;
    
    /**
     * 模组配置类
     * 用于存储开关状态和当前模式
     */
    private static class ModConfig {
        boolean enabled = true;  // 模组是否开启
        int mode = 0;            // 当前模式：0=刷屏，1=击杀，2=混合
    }
    
    // 刷屏计数器和间隔（300刻 = 15秒）
    private static int tickCounter = 0;
    private static final int SPAM_INTERVAL = 300;
    
    // 模组状态变量
    private static boolean modEnabled = true;
    private static int currentMode = 0;
    
    // 按键绑定
    private static KeyBinding toggleKey;      // 开关按键（默认K）
    private static KeyBinding modeSwitchKey;  // 模式切换按键（默认M）
    private static KeyBinding reloadKey;      // 重载配置按键（默认R）
    
    // 模式名称
    private static final String[] MODE_NAMES = {"刷屏模式", "击杀模式", "混合模式"};
    
    /**
     * 击杀模式配置类
     * 用于定义如何识别击杀消息
     */
    private static class KillPattern {
        String name;              // 模式名称
        String pattern;           // 正则表达式
        int killerGroup;          // 击杀者在正则中的分组编号
        int victimGroup;          // 被击杀者在正则中的分组编号
        
        transient Pattern compiledPattern;  // 编译后的正则表达式（不参与序列化）
    }

    /**
     * 模组初始化方法
     * 在客户端启动时调用
     */
    @Override
    public void onInitializeClient() {
        // 获取Minecraft客户端实例
        client = MinecraftClient.getInstance();
        
        // 加载各项配置
        loadConfig();
        loadMessages();
        loadKillPatterns();
        
        // 注册按键绑定
        registerKeyBindings();

        // 注册游戏刻事件监听器
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        // 注册游戏消息接收事件监听器
        ClientReceiveMessageEvents.GAME.register(this::onGameMessage);
    }
    
    /**
     * 加载模组配置
     * 从killsayconfig.json读取上次保存的配置
     */
    private void loadConfig() {
        // 如果配置文件不存在，创建默认配置
        if (!configFile.exists()) {
            saveConfig();
            return;
        }

        // 尝试读取配置文件
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8))) {
            ModConfig config = gson.fromJson(reader, ModConfig.class);
            if (config != null) {
                // 恢复配置
                modEnabled = config.enabled;
                currentMode = config.mode;
                LOGGER.info("Loaded config: enabled={}, mode={}", modEnabled, currentMode);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to read config, using defaults", e);
        }
    }
    
    /**
     * 保存模组配置
     * 将当前状态写入killsayconfig.json
     */
    private void saveConfig() {
        ModConfig config = new ModConfig();
        config.enabled = modEnabled;
        config.mode = currentMode;

        // 写入配置文件
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile, StandardCharsets.UTF_8))) {
            gson.toJson(config, writer);
            LOGGER.info("Saved config: enabled={}, mode={}", modEnabled, currentMode);
        } catch (Exception e) {
            LOGGER.error("Failed to save config", e);
        }
    }
    
    /**
     * 注册按键绑定
     * 将自定义按键注册到游戏设置中
     */
    private void registerKeyBindings() {
        // 开关按键 - K键
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.killsay.toggle",
                GLFW.GLFW_KEY_K,
                "category.killsay"
        ));
        
        // 模式切换按键 - M键
        modeSwitchKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.killsay.mode",
                GLFW.GLFW_KEY_M,
                "category.killsay"
        ));
        
        // 重载配置按键 - R键
        reloadKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.killsay.reload",
                GLFW.GLFW_KEY_R,
                "category.killsay"
        ));
    }

    /**
     * 游戏刻事件处理
     * 每个游戏刻都会调用此方法
     */
    private void onClientTick(MinecraftClient mc) {
        // 处理按键输入
        handleKeyBindings();
        
        // 如果模组未开启、玩家不存在或网络处理器不存在，直接返回
        if (!modEnabled || mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }

        // 如果是刷屏模式或混合模式，进行刷屏计时
        if (currentMode == 0 || currentMode == 2) {
            tickCounter++;
            if (tickCounter >= SPAM_INTERVAL) {
                tickCounter = 0;
                sendSpamMessage();
            }
        }
    }
    
    /**
     * 处理按键输入
     * 检测并响应各种按键操作
     */
    private void handleKeyBindings() {
        // 开关按键被按下
        while (toggleKey.wasPressed()) {
            modEnabled = !modEnabled;
            saveConfig();
            if (modEnabled) {
                showMessage("§a§lKillsay 已开启§r - 当前模式: §b" + MODE_NAMES[currentMode]);
            } else {
                showMessage("§c§lKillsay 已关闭§r");
            }
        }
        
        // 模式切换按键被按下
        while (modeSwitchKey.wasPressed()) {
            currentMode = (currentMode + 1) % 3;
            saveConfig();
            String modeDescription = getModeDescription(currentMode);
            showMessage("§d§l模式切换§r - 当前模式: §b" + MODE_NAMES[currentMode] + "§r (" + modeDescription + ")");
        }
        
        // 重载配置按键被按下
        while (reloadKey.wasPressed()) {
            loadConfig();
            loadMessages();
            loadKillPatterns();
            showMessage("§e§l配置已重新加载§r - §a" + messages.size() + "§r 条消息, §a" + killPatterns.size() + "§r 种击杀模式");
        }
    }
    
    /**
     * 获取模式描述
     */
    private String getModeDescription(int mode) {
        switch (mode) {
            case 0:
                return "每15秒自动发送一条消息";
            case 1:
                return "仅在击杀时发送消息";
            case 2:
                return "同时开启刷屏和击杀模式";
            default:
                return "";
        }
    }
    
    /**
     * 显示消息
     * 在游戏聊天栏和日志中都显示消息
     */
    private void showMessage(String text) {
        if (client.player != null) {
            // 在游戏聊天栏显示彩色消息
            client.player.sendMessage(Text.literal("§7[§6Goood KillSay§7]§r " + text), false);
        }
        // 在日志中记录
        LOGGER.info(text);
    }
    
    /**
     * 加载击杀模式配置
     * 从killpatterns.json读取击杀识别规则
     */
    public static void loadKillPatterns() {
        killPatterns.clear();
        if (!patternsFile.exists()) {
            LOGGER.info("killpatterns.json not found, using default patterns");
            addDefaultPatterns();
            return;
        }

        // 读取并解析JSON配置
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(patternsFile), StandardCharsets.UTF_8))) {
            Type listType = new TypeToken<List<KillPattern>>() {}.getType();
            List<KillPattern> loadedPatterns = gson.fromJson(reader, listType);
            if (loadedPatterns != null) {
                for (KillPattern kp : loadedPatterns) {
                    // 编译正则表达式
                    kp.compiledPattern = Pattern.compile(kp.pattern);
                    killPatterns.add(kp);
                }
            }
            LOGGER.info("Loaded {} kill patterns", killPatterns.size());
        } catch (Exception e) {
            LOGGER.error("Failed to read killpatterns.json, using default patterns", e);
            killPatterns.clear();
            addDefaultPatterns();
        }
    }
    
    /**
     * 添加默认击杀模式
     */
    private static void addDefaultPatterns() {
        // 由 killpatterns.json 配置
    }
    
    /**
     * 游戏消息接收事件处理
     * 检测击杀消息并触发击杀响应
     */
    private void onGameMessage(Text message, boolean overlay) {
        // 如果是覆盖消息、模组未开启、或不是击杀/混合模式，直接返回
        if (overlay || !modEnabled || (currentMode != 1 && currentMode != 2)) {
            return;
        }
        if (client.player == null) {
            return;
        }

        // 获取消息文本和玩家名称
        String messageText = message.getString();
        String playerName = client.player.getName().getString();
        
        // 尝试识别击杀消息
        String victimName = getVictimName(messageText, playerName);
        if (victimName != null) {
            sendKillMessage(victimName);
        }
    }
    
    /**
     * 从消息中获取被击杀者名称
     * 遍历所有击杀模式进行匹配
     */
    private String getVictimName(String message, String killerName) {
        for (KillPattern kp : killPatterns) {
            try {
                // 使用正则表达式匹配消息
                Matcher matcher = kp.compiledPattern.matcher(message);
                if (matcher.matches()) {
                    // 检查击杀者是否是当前玩家
                    if (matcher.group(kp.killerGroup).equals(killerName)) {
                        // 返回被击杀者名称
                        return matcher.group(kp.victimGroup);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to match pattern: {}", kp.name);
            }
        }
        return null;
    }

    /**
     * 获取随机玩家名称
     * 从服务器在线玩家列表中随机选择一个
     */
    private static String getRandomPlayerName() {
        if (client.getNetworkHandler() == null) {
            return "Player";
        }

        List<PlayerListEntry> playerList = new ArrayList<>(client.getNetworkHandler().getPlayerList());
        if (playerList.isEmpty()) {
            return "Player";
        }

        // 随机选择一个玩家
        PlayerListEntry randomPlayer = playerList.get(random.nextInt(playerList.size()));
        return randomPlayer.getProfile().getName();
    }

    /**
     * 加载消息列表
     * 从killsay.txt读取消息内容
     */
    public static void loadMessages() {
        messages.clear();
        if (!killsayFile.exists()) {
            LOGGER.info("killsay.txt not found, creating default file");
            try {
                // 创建默认文件和示例消息
                killsayFile.createNewFile();
                messages.add("GG {name}!");
                messages.add("Nice try {name}!");
                messages.add("Better luck next time {name}!");
            } catch (IOException e) {
                LOGGER.error("Failed to create killsay.txt", e);
            }
            return;
        }

        // 读取消息文件
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(killsayFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 跳过空行
                if (!line.trim().isEmpty()) {
                    messages.add(line.trim());
                }
            }
            LOGGER.info("Loaded {} messages", messages.size());
        } catch (IOException e) {
            LOGGER.error("Failed to read killsay.txt", e);
        }
    }

    /**
     * 获取随机消息
     * 将{name}替换为指定的玩家名称
     */
    public static String getRandomMessage(String playerName) {
        if (messages.isEmpty()) {
            return "GG {name}!".replace("{name}", playerName);
        }
        // 随机选择一条消息并替换占位符
        String message = messages.get(random.nextInt(messages.size()));
        return message.replace("{name}", playerName);
    }

    /**
     * 发送刷屏消息
     * 随机选择一个玩家并发送消息
     */
    public static void sendSpamMessage() {
        if (client.player == null || client.getNetworkHandler() == null) {
            return;
        }

        // 获取随机玩家名称并发送消息
        String randomPlayerName = getRandomPlayerName();
        String message = getRandomMessage(randomPlayerName);
        LOGGER.info("Sending spam message: {}", message);
        client.getNetworkHandler().sendChatMessage(message);
    }
    
    /**
     * 发送击杀消息
     * 在击杀玩家后调用
     */
    public static void sendKillMessage(String victimName) {
        // 检查是否应该发送消息
        if (!modEnabled || (currentMode != 1 && currentMode != 2)) {
            return;
        }
        if (client.player == null || client.getNetworkHandler() == null) {
            return;
        }

        // 发送消息
        String message = getRandomMessage(victimName);
        LOGGER.info("Sending kill message: {}", message);
        client.getNetworkHandler().sendChatMessage(message);
    }
}
