# 点时 / LastTap

Android 桌面小组件记时小工具：在桌面上点一下，就把对应活动的「最近一次执行时间」更新为当前时间。不打开应用、不联网、无常驻服务。

## 功能

- 两种小组件：单活动卡片（整卡即按钮）、全部活动列表（宽屏自动两列，可自由拉伸）
- 单击即记录；可设置连击合并窗口（30 秒 / 1 分钟 / 5 分钟），窗口内重复点击只记一次，时间显示自动按窗口对齐并带秒
- 应用内管理：新建 / 重命名 / 排序 / 删除活动；历史记录可撤销最近一次、可删除任意一条
- JSON 备份导出 / 导入

## 构建

```bash
./gradlew :app:assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

需要 JDK 17+ 与 Android SDK（compileSdk 35）。若直连 `services.gradle.org` / `dl.google.com` 超时，Gradle 发行版可用腾讯镜像、依赖仓库可用阿里云镜像（maven.aliyun.com/repository/google 与 /public）。

## 状态

小功能软件，功能已完成，**不再继续开发**。

技术栈：Kotlin · Jetpack Glance · Room · Compose
