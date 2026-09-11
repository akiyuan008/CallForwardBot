# 来电接听转发助手（CallForwardBot）

学校电话机来电 → 手机自动接听 → 自动录音 → 录音以语音消息推送到家长微信。

- 平台：Android 10+（目标机型：澎湃OS / OriginOS，Android 13/14）
- 成本：0 元（企业微信免费接口 + 手机本地处理）

## 架构

```
学校电话机 → App（前台服务常驻）
  ① 来电监听：TelephonyManager + PhoneStateListener，白名单比对（支持前缀匹配）
  ② 自动接听：AccessibilityService.dispatchGesture() 模拟上滑接听
  ③ 自动免提：AudioManager（API 31+ 用 setCommunicationDevice）
  ④ 录音监听：轮询系统通话录音目录（不自己录音），文件稳定后触发
  ⑤ 上传发送：企业微信 API（gettoken → 上传素材 → 发消息），失败重试 3 次
  ⑥ 保活：前台 Service + BOOT_COMPLETED + JobScheduler 周期唤醒
```

## 构建（GitHub Actions）

推送到 main 分支即自动构建 Debug APK，产物在 Actions 页面的 Artifacts 中下载。
打 tag `v*` 会构建 Release APK 并发布到 Release。

本地构建：用 Android Studio 打开本目录，Sync 后 Build → Build APK。

## 使用配置步骤

1. **企业微信（M1 里程碑）**
   - 注册企业微信（个人可注册），创建「自建应用」；
   - 应用详情页拿到 AgentId 和 Secret；
   - 「我的企业」页拿到 CorpId；
   - 让家长扫码关注「微信插件」并拿到家长通讯录 UserID；
   - 在 App「设置」页填入四项参数，点「发送测试文字消息」验证链路。

2. **白名单**：「白名单」页添加学校电话机号码（可多个、支持前缀如 0755）。

3. **无障碍**：系统设置 → 无障碍 → 开启「来电接听转发助手」（自动接听必需）。

4. **系统通话录音**：手机设置中开启自动通话录音（澎湃：录音机 App → 设置 → 通话录音；
   OriginOS：i管家/录音机中开启）。录音目录常见位置：
   `MIUI/sound_recorder/call_rec/`、`Music/CallRecord/` 等，自检页会显示实际命中的目录。

5. **保活**：电池设为「无限制」、允许自启动、最近任务锁定；点「申请忽略电池优化」。

## 权限说明

READ_PHONE_STATE（监听来电）、AccessibilityService（模拟接听手势）、
FOREGROUND_SERVICE/POST_NOTIFICATIONS（常驻）、RECEIVE_BOOT_COMPLETED（开机自启）、
INTERNET（企业微信 API）、READ_EXTERNAL_STORAGE/READ_MEDIA_AUDIO（读取系统录音）。

## 已知限制与后续迭代

- 接听手势为「屏幕中线上滑」，个别系统主题界面若不匹配需在 AutoAnswerService 中调整坐标；
- 录音目录按常见 ROM 路径探测，自检页可确认，缺失时可补充；
- 企业微信 voice 素材上限 2MB，超过自动降级为 file 消息（20MB 上限）；
- 通话录音请遵守当地法律法规，确保相关方知悉。

## 目录结构

```
app/src/main/java/com/autoanswer/app/
  MainActivity / SettingsFragment / WhitelistFragment / HistoryFragment / CheckFragment
  CallMonitorService   来电监听 + 自动接听调度 + 免提
  AutoAnswerService    无障碍模拟手势
  RecWatcher           录音目录轮询监听
  WeComApi/WeComSender 企业微信 API + 重试
  Prefs/Crypto         配置存储（secret 轻量混淆）
  BootReceiver/KeepAliveJob  开机自启 + 周期唤醒
```
