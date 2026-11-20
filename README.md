<img src="src/main/resources/BlockZ_logo.png" alt="BlockZ Logo" width="420">

[简体中文](README.md) | [English](README_en_us.md)

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-62B47A?style=flat-square)
![Forge](https://img.shields.io/badge/Forge-47.x-orange?style=flat-square)
![License](https://img.shields.io/badge/License-GPL--3.0-blue?style=flat-square)
![Version](https://img.shields.io/badge/Version-1.1.6--Beta--fix-lightgrey?style=flat-square)

BlockZ 是一个面向 Minecraft 1.20.1（Forge）的硬核末日生存模组，灵感来自 DayZ 与 DayM。它以 DayZ 风格库存、占格物品、医疗状态、战术姿态与联机同步为核心，重建更慢、更压抑的生存节奏。

从网格背包、尸体搜刮，到体温、口渴、疾病、流血、骨折与负重，每一次外出都是一次风险评估。安装、配置、命令与兼容说明请见 Wiki。

## 文档与社区

* [Wiki 文档站](https://yitian77.github.io/)

<a href="https://discord.gg/3GktAbyfYt"><img src="src/main/resources/assets/blockz/textures/gui/mainmenu/poster_0.png" alt="BlockZ Discord 服务器" width="320"></a>

[加入 BlockZ Discord 社区](https://discord.gg/3GktAbyfYt)

## 贡献

欢迎提交 Issue 与 Pull Request。

* 使用 Java 17 与 Forge 1.20.1 官方映射，遵循现有分包与命名。
* `if` 语句使用花括号，常量使用全大写下划线命名，适当添加 `@Override`。
* 修改配置时同步 Cloth Config 界面与 `zh_cn.json` / `en_us.json`。
* 修改网络包字段时同步客户端与服务端，并提升 `NetworkHandler.PROTOCOL`。
* 保持类的职责单一，不提交无关的构建产物、日志或临时文件。

## 许可

BlockZ 使用 **GNU General Public License v3.0**，详见 [LICENSE](LICENSE)。允许在整合包、视频与直播中使用并注明出处；修改或二次分发需继续使用 GPL-3.0。

## 鸣谢

GeckoLib、Minecraft Forge、Curios API、TACZ、Thirst Was Taken、Cloth Config、JEI、DayZ / DayM，以及所有参与测试、反馈与赞助的玩家。

## 赞助

<a href="https://ifdian.net/a/yitianys"><img src="src/main/resources/assets/blockz/textures/gui/mainmenu/poster_2.png" alt="爱发电：支持我的创作" width="420"></a>

[支持我的创作，让热爱持续发电](https://ifdian.net/a/yitianys)

