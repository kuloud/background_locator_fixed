
# cactus_locator
一个Flutter插件，让应用程序在后台，也可以获得位置更新。

本插件是 background_locator 包的 V3 版本，适配 Flutter 3.x，在安卓侧集成了 Cactus，提高保活性能。

```bash
flutter pub add cactus_locator
```

请参阅[wiki](https://github.com/kuloud/cactus_locator/wiki)页面获取安装和设置说明，或通过以下链接跳转到特定主题:

* [安装](https://github.com/kuloud/cactus_locator/wiki/Installation)
* [配置](https://github.com/kuloud/cactus_locator/wiki/Setup)
* [如何使用](https://github.com/kuloud/cactus_locator/wiki/How-to-use)

# ChangeLog
# 1.0.0
1. Android 集成Cactus组件，增强应用保活，移除其中OnePix功能(Android P以上不再有效)
2. Android minSdkVersion 改为28（个人开发视角来看，简化后续适配成本，有需求可以自行降版本）
3. Android 移除 Google GMS 依赖



##  License
本项目基于 MIT 开源许可 - [LICENSE](LICENSE)

## Contributor
本项目基于 Yukams 的 V2 版本定制，原贡献者&项目:
* [Cactus](https://github.com/kuloud/Cactus)(Cactus变体，适配 30+ API)
* [Cactus](https://github.com/gyf-dev/Cactus)
* [Yukams](https://github.com/Yukams) (creator of V2)
* [Rekab](https://github.com/rekabhq) (creator of V1)
* [Gerardo Ibarra](https://github.com/gpibarra)
* [RomanJos](https://github.com/RomanJos)
* [Marcelo Henrique Neppel](https://github.com/marceloneppel)
