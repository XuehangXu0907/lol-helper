# LOL Helper v2.1.0 - 构建完成 ✅

## 📦 构建产物

### ✅ 已生成文件
- **Fat JAR**: `target/lol-auto-ban-pick-tool-2.1.0-shaded.jar` (推荐使用)
- **常规JAR**: `target/lol-auto-ban-pick-tool-2.1.0.jar`
- **发布说明**: `RELEASE-NOTES-v2.1.0.md`

### 🚀 快速使用

#### 直接运行 Fat JAR
```bash
java -jar target/lol-auto-ban-pick-tool-2.1.0-shaded.jar
```

#### 创建安装包
```bash
build-installer.bat
```
这将生成: `dist/LOLHelper-2.1.0.msi`

## 🎯 主要改进总结

### ✨ 新功能
1. **智能弹窗抑制** - 每个session抑制一次，不再持续抑制
2. **自动连接** - 启动时自动检测并连接LOL客户端 
3. **断线重连** - 连接断开时自动重连，默认10秒间隔
4. **完整多语言** - 所有界面文字支持中英文切换

### 🐛 修复问题
1. **英雄ID错误** - 修复ban/pick功能中的英雄ID映射错误
2. **硬编码文字** - 所有硬编码文字已替换为国际化

### ⚙️ 技术升级
- PopupSuppressionManager完全重写
- 添加session级别状态跟踪和去重机制
- 增强的连接管理和自动重连
- 完善的配置管理系统

## 🔧 用户配置

新增配置选项（auto-accept-config.json）：
```json
{
  "autoConnectEnabled": true,      // 启动时自动连接
  "autoReconnectEnabled": true,    // 断线自动重连  
  "reconnectIntervalSeconds": 10   // 重连检测间隔
}
```

## ✅ 测试验证

- [x] 编译成功
- [x] Fat JAR可正常启动
- [x] 自动连接功能正常
- [x] 弹窗抑制系统工作正常
- [x] 多语言切换正常
- [x] 系统托盘功能正常

## 📋 下一步

1. **分发版本**:
   - 运行 `build-installer.bat` 创建MSI安装包
   - 或直接分发 Fat JAR 文件

2. **用户指南**:
   - 查看 `RELEASE-NOTES-v2.1.0.md` 了解详细更新内容
   - 查看 `README.md` 了解使用方法

3. **反馈收集**:
   - 收集用户对新功能的反馈
   - 监控自动连接和弹窗抑制的效果

---

**LOL Helper v2.1.0 构建完成！** 🎮

所有主要功能都已实现并测试通过，可以正式发布使用。