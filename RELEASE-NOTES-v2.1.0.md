# LOL Helper v2.1.0 Release Notes

## 🚀 主要更新

### ✨ 新功能

1. **智能弹窗抑制系统** - 完全重写的弹窗抑制机制
   - 改为每个session抑制一次，而不是持续抑制
   - 基于champion select session ID和action ID的精确去重
   - 准备检查弹窗基于时间戳进行session识别
   - 自动在游戏阶段变化时重置抑制状态

2. **自动连接与重连** - 全新的连接管理系统
   - **启动时自动连接**：应用启动后2秒自动检测并连接LOL客户端
   - **断线自动重连**：连接断开时自动启动重连任务，默认每10秒检测一次
   - **用户可配置**：提供"自动连接"和"断线重连"开关选项
   - **智能连接状态监听**：自动处理断线和重连

3. **完善多语言支持** - 国际化系统全面升级
   - 新增自动连接相关翻译 (autoConnect.*)
   - 新增弹窗抑制相关翻译 (popupSuppression.*)
   - 新增技能详情相关翻译 (skill.*)
   - 新增错误消息翻译 (error.*)
   - 新增状态消息翻译 (status.*)
   - 所有硬编码文字已替换为国际化

### 🐛 Bug修复

1. **英雄ID映射错误修复**
   - 修复了LocalDataManager中id/key字段映射错误
   - 修复了BilingualChampionDataManager中字段映射错误
   - 确保ban/pick功能使用正确的英雄ID

### ⚙️ 技术改进

1. **PopupSuppressionManager优化**
   - 添加了ConcurrentHashMap来跟踪已抑制的actions
   - 使用唯一的session和action标识符防止重复抑制
   - 改进的阶段变化处理，自动清理过期状态

2. **AutoAcceptController增强**
   - 添加了Timeline autoReconnectTimeline进行重连管理
   - 实现了智能连接状态监听
   - 新增事件处理器：onAutoConnectToggled(), onAutoReconnectToggled()

3. **配置管理扩展**
   - AutoAcceptConfig中新增自动连接相关配置项
   - 包含合理的默认值和边界检查
   - 支持持久化保存用户设置

## 📱 用户体验改善

- **无需手动干预**：启用自动连接后，用户不再需要每次手动点击连接按钮
- **智能重连**：网络波动或客户端重启时自动恢复连接
- **精确弹窗控制**：弹窗抑制更加精确，避免过度干扰
- **完整多语言**：界面文字支持中英文完全切换
- **配置灵活**：用户可以根据需要启用或禁用各项自动功能

## 🔧 配置选项

新增配置项（在auto-accept-config.json中）：
```json
{
  "autoConnectEnabled": true,      // 启动时自动连接
  "autoReconnectEnabled": true,    // 断线自动重连
  "reconnectIntervalSeconds": 10   // 重连检测间隔(秒)
}
```

## 📦 构建信息

- **版本**: 2.1.0
- **Java要求**: JDK 17+
- **JavaFX版本**: 17.0.2
- **构建工具**: Maven 3.6+

## 🛠️ 开发者信息

### 构建命令
```bash
# 快速构建
mvn clean package -DskipTests

# 创建安装包
build-installer.bat
```

### 生成文件
- `target/lol-auto-ban-pick-tool-2.1.0-shaded.jar` - Fat JAR（推荐）
- `target/lol-auto-ban-pick-tool-2.1.0.jar` - 普通JAR
- `dist/LOLHelper-2.1.0.msi` - Windows安装包

## 🎯 下一版本计划

- 更多游戏阶段的智能检测
- 英雄选择策略优化
- 性能监控和优化
- 更多自定义配置选项

---

**感谢使用 LOL Helper！**

如有问题或建议，请通过GitHub Issues反馈。