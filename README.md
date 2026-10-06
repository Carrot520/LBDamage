# LBDamage

MC 原神字体伤害显示模组。客户端会把实体生命值变化渲染成浮动伤害数字，并对暴击使用金色高亮。

## 下载版本

请根据你的 Minecraft 版本选择对应的 Release，两个版本不能混用：

- Minecraft `26.1.2`：Fabric Loader `0.19.3`，使用 `LBDamage-2.0.0-mc26.1.2-fabric.jar`
- Minecraft `26.2`：Fabric Loader `0.19.5` 或更高，使用 `LBDamage-2.0.0-mc26.2-fabric.jar`

两个版本均为客户端模组，无需安装服务端插件。26.2 包包含针对新实体系统的修复，不能替代 26.1.2 包。

## 功能

- 普通攻击显示伤害数字
- 暴击粒子触发金色暴击数字
- 通过目标 UUID 关联本地攻击与受伤实体，减少多目标或高延迟场景下的误判
- 26.2 客户端本地 `TextDisplay` 浮字实体使用独立负数 ID，不与服务端实体冲突
- 使用自定义数字字体纹理，显示效果接近原神风格
- 仅客户端渲染；未安装模组的玩家不会看到这些数字

## 安装

1. 安装与目标 Minecraft 版本匹配的 Fabric Loader 和 Fabric API。
2. 将发布 JAR 放入客户端 `mods` 文件夹。
3. 启动游戏。首次启动后会生成 `config/lbdamage.properties`。

## 配置

配置文件：`config/lbdamage.properties`

```properties
scale=0.8
display-ticks=10
float-up=0.4
random-offset=0.3
height-offset=0.0
min-damage=0.05
decimal-places=1
crit-bold=true
```

- `scale`：数字缩放。
- `display-ticks`：数字显示时间，单位为游戏刻。
- `float-up`：数字上浮距离。
- `random-offset`：数字生成位置的随机偏移。
- `height-offset`：相对实体头顶的高度偏移。
- `min-damage`：低于该值的生命变化不显示。
- `decimal-places`：小数位数，支持 `0` 到 `2`。
- `crit-bold`：是否加粗暴击数字。

## 源码说明

仓库中的 Java 源码是根据当前 26.2 可用客户端包整理的可维护版本，`release` 目录提供已验证的构建产物。`tools` 目录保留了早期发布包的字节码修补工具，仅供历史复现和排查使用。

## 许可证

本项目采用 [MIT License](LICENSE)。你可以自由使用、复制、修改、整合和再分发本模组，包括用于整合包或二次开发，但需要保留原许可证和版权声明。
