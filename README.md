# Goood KillSay

一个 Minecraft 1.21.4 Fabric 模组，用于在 PVP 服务器上自动发送击杀消息和刷屏消息。

## 功能特点

- **刷屏模式** - 每 15 秒自动发送一条随机消息
- **击杀模式** - 仅在击杀时发送消息
- **混合模式** - 同时开启刷屏和击杀模式
- **自定义按键** - 可在游戏设置中自定义按键绑定
- **多语言支持** - 支持中文、英文、日语、韩语等多种语言
- **灵活的击杀检测** - 通过配置文件支持多种 PVP 服务器的击杀格式
- **UTF-8 编码** - 支持中文和其他 Unicode 字符

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

## 按键绑定

默认按键：
- **K** - 开启/关闭 Goood KillSay
- **M** - 切换工作模式
- **R** - 重新加载配置文件

可在游戏设置中的"按键设置" → "Goood KillSay"分类中自定义按键。

## 工作模式

1. **刷屏模式** - 每 15 秒自动发送一条消息，使用服务器在线随机玩家名字替换 `{name}`
2. **击杀模式** - 仅在检测到击杀时发送消息
3. **混合模式** - 同时开启刷屏和击杀模式

## 开源指南

### 如何开源这个 Mod

1. **创建 GitHub 仓库**
   - 访问 https://github.com/new
   - 仓库名称填写：`GooodKillSay`
   - 设置为 Public（公开）
   - 点击 "Create repository"

2. **初始化 Git 仓库**
   ```bash
   cd d:\360MoveData\Users\syy\Desktop\killsay
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/eric1145141919810/GooodKillSay.git
   git push -u origin main
   ```

3. **发布 Release**
   - 在 GitHub 仓库页面点击 "Releases"
   - 点击 "Draft a new release"
   - 填写版本号（如：v1.0.0）
   - 上传编译好的 jar 文件（在 `build/libs/` 文件夹中）
   - 点击 "Publish release"

4. **可选：发布到 CurseForge/Modrinth**
   - 在相应平台注册账号
   - 按照平台指引提交模组

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
