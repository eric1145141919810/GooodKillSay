# Goood KillSay - Fabric 1.21.4

A Fabric 1.21.4 Mod that automatically sends messages in Minecraft PVP

## 功能特点

- **刷屏模式** - 每 N 秒自动发送一条随机消息（N 可自定义，默认 15 秒）
- **击杀模式** - 仅在击杀时发送消息
- **混合模式** - 同时开启刷屏和击杀模式
- **自定义按键** - 可在游戏设置中自定义按键绑定
- **多语言支持** - 支持中文、英文、日语、韩语等多种语言
- **灵活的击杀检测** - 通过配置文件支持多种 PVP 服务器的击杀格式
- **UTF-8 编码** - 支持中文和其他 Unicode 字符
- **配置保存** - 自动保存开关状态、模式和时间间隔
- **防刷屏自己** - 刷屏时不会选择自己作为目标

## 安装

1. 下载 Fabric Loader 和 Fabric API
2. 将 `killsay-1.0.0.jar` 放入 `.minecraft/mods/` 文件夹
3. 将 `killsay.txt` 和 `killpatterns.json` 放入游戏根目录（与 mods 文件夹同级）
4. 启动游戏即可使用

## 配置

### killsay.txt

存放消息内容，每行一条。支持 `{name}` 占位符，会自动替换为被击杀玩家或随机玩家的名字。

示例：
```
GG {name}!
Nice try {name}!
Better luck next time {name}!
```

### killpatterns.json

配置击杀检测的正则表达式。

示例：
```json
[
  {
    "name": "示例",
    "pattern": "^(.*) 被 (.*) 击败$",
    "killerGroup": 2,
    "victimGroup": 1
  }
]
```

### killsayconfig.json

自动保存的配置文件，也可以手动编辑：
```json
{
  "enabled": true,
  "mode": 0,
  "spamInterval": 300
}
```
- `spamInterval` 单位为游戏刻（20刻 = 1秒），默认 300 = 15秒

## 按键绑定

默认按键：
- **K** - 开启/关闭 Goood KillSay
- **M** - 切换工作模式
- **R** - 重新加载配置文件

可在游戏设置中的"按键设置" → "Goood KillSay"分类中自定义按键。

## 工作模式

1. **刷屏模式** - 每 N 秒自动发送一条消息，使用服务器在线随机玩家名字替换 `{name}`（不会选择自己）
2. **击杀模式** - 仅在检测到击杀时发送消息
3. **混合模式** - 同时开启刷屏和击杀模式

## 构建

使用 Gradle 构建：
```bash
gradlew build
```

编译好的 jar 文件在 `build/libs/` 文件夹中。

## 开发者

- **作者**：Eric
- **网站**：https://github.com/eric1145141919810/GooodKillSay

## 许可证

MIT License - 详见 LICENSE 文件
