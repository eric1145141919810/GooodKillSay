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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KillSayMod implements ClientModInitializer {
    public static final String MOD_ID = "killsay";
    public static final String MOD_NAME = "Goood KillSay";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final List<String> messages = new ArrayList<>();
    private static final List<KillPattern> killPatterns = new ArrayList<>();
    private static final Random random = new Random();
    private static final File killsayFile = new File("killsay.txt");
    private static final File patternsFile = new File("killpatterns.json");
    private static final Gson gson = new Gson();
    private static MinecraftClient client;
    
    private static int tickCounter = 0;
    private static final int SPAM_INTERVAL = 300;
    
    private static boolean modEnabled = true;
    private static int currentMode = 0;
    
    private static KeyBinding toggleKey;
    private static KeyBinding modeSwitchKey;
    private static KeyBinding reloadKey;
    
    private static final String[] MODE_NAMES = {"刷屏模式", "击杀模式", "混合模式"};
    
    private static class KillPattern {
        String name;
        String pattern;
        int killerGroup;
        int victimGroup;
        
        transient Pattern compiledPattern;
    }

    @Override
    public void onInitializeClient() {
        client = MinecraftClient.getInstance();
        loadMessages();
        loadKillPatterns();
        registerKeyBindings();

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ClientReceiveMessageEvents.GAME.register(this::onGameMessage);
    }
    
    private void registerKeyBindings() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.killsay.toggle",
                GLFW.GLFW_KEY_K,
                "category.killsay"
        ));
        
        modeSwitchKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.killsay.mode",
                GLFW.GLFW_KEY_M,
                "category.killsay"
        ));
        
        reloadKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.killsay.reload",
                GLFW.GLFW_KEY_R,
                "category.killsay"
        ));
    }

    private void onClientTick(MinecraftClient mc) {
        handleKeyBindings();
        
        if (!modEnabled || mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }

        if (currentMode == 0 || currentMode == 2) {
            tickCounter++;
            if (tickCounter >= SPAM_INTERVAL) {
                tickCounter = 0;
                sendSpamMessage();
            }
        }
    }
    
    private void handleKeyBindings() {
        while (toggleKey.wasPressed()) {
            modEnabled = !modEnabled;
            if (modEnabled) {
                showMessage("§a§lKillsay 已开启§r - 当前模式: §b" + MODE_NAMES[currentMode]);
            } else {
                showMessage("§c§lKillsay 已关闭§r");
            }
        }
        
        while (modeSwitchKey.wasPressed()) {
            currentMode = (currentMode + 1) % 3;
            String modeDescription = getModeDescription(currentMode);
            showMessage("§d§l模式切换§r - 当前模式: §b" + MODE_NAMES[currentMode] + "§r (" + modeDescription + ")");
        }
        
        while (reloadKey.wasPressed()) {
            loadMessages();
            loadKillPatterns();
            showMessage("§e§l配置已重新加载§r - §a" + messages.size() + "§r 条消息, §a" + killPatterns.size() + "§r 种击杀模式");
        }
    }
    
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
    
    private void showMessage(String text) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§7[§6Goood KillSay§7]§r " + text), false);
        }
        LOGGER.info(text);
    }
    
    public static void loadKillPatterns() {
        killPatterns.clear();
        if (!patternsFile.exists()) {
            LOGGER.info("killpatterns.json not found, using default patterns");
            addDefaultPatterns();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(patternsFile), StandardCharsets.UTF_8))) {
            Type listType = new TypeToken<List<KillPattern>>() {}.getType();
            List<KillPattern> loadedPatterns = gson.fromJson(reader, listType);
            if (loadedPatterns != null) {
                for (KillPattern kp : loadedPatterns) {
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
    
    private static void addDefaultPatterns() {
        KillPattern kp1 = new KillPattern();
        kp1.name = "原版击杀";
        kp1.pattern = "^(.*) killed (.*)$";
        kp1.killerGroup = 1;
        kp1.victimGroup = 2;
        kp1.compiledPattern = Pattern.compile(kp1.pattern);
        killPatterns.add(kp1);
        
        KillPattern kp2 = new KillPattern();
        kp2.name = "原版击杀2";
        kp2.pattern = "^(.*) was killed by (.*)$";
        kp2.killerGroup = 2;
        kp2.victimGroup = 1;
        kp2.compiledPattern = Pattern.compile(kp2.pattern);
        killPatterns.add(kp2);
    }
    
    private void onGameMessage(Text message, boolean overlay) {
        if (overlay || !modEnabled || (currentMode != 1 && currentMode != 2)) {
            return;
        }
        if (client.player == null) {
            return;
        }

        String messageText = message.getString();
        String playerName = client.player.getName().getString();
        
        String victimName = getVictimName(messageText, playerName);
        if (victimName != null) {
            sendKillMessage(victimName);
        }
    }
    
    private String getVictimName(String message, String killerName) {
        for (KillPattern kp : killPatterns) {
            try {
                Matcher matcher = kp.compiledPattern.matcher(message);
                if (matcher.matches()) {
                    if (matcher.group(kp.killerGroup).equals(killerName)) {
                        return matcher.group(kp.victimGroup);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to match pattern: {}", kp.name);
            }
        }
        return null;
    }

    private static String getRandomPlayerName() {
        if (client.getNetworkHandler() == null) {
            return "Player";
        }

        List<PlayerListEntry> playerList = new ArrayList<>(client.getNetworkHandler().getPlayerList());
        if (playerList.isEmpty()) {
            return "Player";
        }

        PlayerListEntry randomPlayer = playerList.get(random.nextInt(playerList.size()));
        return randomPlayer.getProfile().getName();
    }

    public static void loadMessages() {
        messages.clear();
        if (!killsayFile.exists()) {
            LOGGER.info("killsay.txt not found, creating default file");
            try {
                killsayFile.createNewFile();
                messages.add("GG {name}!");
                messages.add("Nice try {name}!");
                messages.add("Better luck next time {name}!");
            } catch (IOException e) {
                LOGGER.error("Failed to create killsay.txt", e);
            }
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(killsayFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    messages.add(line.trim());
                }
            }
            LOGGER.info("Loaded {} messages", messages.size());
        } catch (IOException e) {
            LOGGER.error("Failed to read killsay.txt", e);
        }
    }

    public static String getRandomMessage(String playerName) {
        if (messages.isEmpty()) {
            return "GG {name}!".replace("{name}", playerName);
        }
        String message = messages.get(random.nextInt(messages.size()));
        return message.replace("{name}", playerName);
    }

    public static void sendSpamMessage() {
        if (client.player == null || client.getNetworkHandler() == null) {
            return;
        }

        String randomPlayerName = getRandomPlayerName();
        String message = getRandomMessage(randomPlayerName);
        LOGGER.info("Sending spam message: {}", message);
        client.getNetworkHandler().sendChatMessage(message);
    }
    
    public static void sendKillMessage(String victimName) {
        if (!modEnabled || (currentMode != 1 && currentMode != 2)) {
            return;
        }
        if (client.player == null || client.getNetworkHandler() == null) {
            return;
        }

        String message = getRandomMessage(victimName);
        LOGGER.info("Sending kill message: {}", message);
        client.getNetworkHandler().sendChatMessage(message);
    }
}
