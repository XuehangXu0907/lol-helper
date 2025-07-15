# Auto Accept Ban Pick 功能说明文档

## 概述
Auto Accept Ban Pick 是英雄联盟自动接受工具的核心功能，它不仅能自动接受对局，还支持在英雄选择(Champion Select)阶段自动ban英雄和选择英雄。该功能使用官方的LCU(League Client Update) API，安全可靠。

## 核心功能

### 1. 自动接受对局 (Auto Accept)
- **功能描述**: 自动检测并接受准备检查(Ready Check)
- **触发条件**: 当系统检测到准备检查激活时
- **实现方式**: 调用 `/lol-matchmaking/v1/ready-check/accept` API端点
- **状态监控**: 持续监控 `ReadyCheck` 游戏流程阶段

### 2. 自动Ban英雄 (Auto Ban)
- **功能描述**: 在Ban阶段自动禁用预设的英雄
- **触发条件**: 检测到处于ban阶段且轮到用户ban英雄时
- **实现方式**: 
  - 获取英雄选择会话信息 (`/lol-champ-select/v1/session`)
  - 找到当前用户的ban操作
  - 通过PUT请求完成ban操作 (`/lol-champ-select/v1/session/actions/{actionId}`)
- **英雄ID映射**: 内置英雄名称到ID的完整映射表

### 3. 自动Pick英雄 (Auto Pick)
- **功能描述**: 在Pick阶段自动选择预设的英雄
- **触发条件**: 检测到处于pick阶段且轮到用户选择英雄时
- **实现方式**:
  - 获取英雄选择会话信息
  - 找到当前用户的pick操作
  - 通过PUT请求完成pick操作
- **冲突处理**: 如果预设英雄已被ban或被其他玩家选择，会显示相应提示

## 技术架构

### 核心组件
1. **LCUApi** (`lcu_api.py`): LCU API连接和认证处理
2. **MatchMonitor** (`match_monitor.py`): 对局状态监控和操作执行
3. **ChampionSelector** (`champion_selector.py`): 英雄选择界面组件
4. **GUI界面** (`lol_auto_accept.py`): 用户配置和状态显示

### API端点使用
- `/lol-gameflow/v1/gameflow-phase`: 获取当前游戏流程阶段
- `/lol-matchmaking/v1/ready-check`: 准备检查状态
- `/lol-champ-select/v1/session`: 英雄选择会话
- `/lol-champ-select/v1/session/actions/{id}`: 执行ban/pick操作

## 使用方法

### GUI界面操作
1. **启动应用**: 运行 `lol_auto_accept.py` 或 `start.bat`
2. **配置设置**:
   - 勾选"自动接受对局"启用自动接受功能
   - 在英雄选择区域设置ban和pick的英雄
3. **选择英雄**:
   - 点击"选择英雄"按钮打开英雄选择器
   - 可通过搜索快速找到英雄
   - 支持中英文名称搜索
4. **开始监控**: 点击"开始监控"按钮

### 命令行操作
```bash
# 基本使用
python auto_accept.py

# 仅接受对局
python auto_accept.py --auto-accept

# 测试模式(拒绝对局)
python auto_accept.py --auto-decline

# 设置检查间隔
python auto_accept.py --interval 0.5
```

### 配置文件
配置保存在 `config.json` 文件中：
```json
{
  "auto_accept": true,
  "auto_decline": false,
  "check_interval": 1,
  "champion_select": {
    "auto_ban_enabled": true,
    "auto_pick_enabled": true,
    "ban_champion": {
      "name_cn": "艾克",
      "name_en": "Ekko",
      "key": "Ekko"
    },
    "pick_champion": {
      "name_cn": "金克丝",
      "name_en": "Jinx", 
      "key": "Jinx"
    }
  }
}
```

## 工作流程

### 1. 连接建立
- 扫描 `LeagueClientUx.exe` 进程
- 从命令行参数提取LCU端口和密码
- 建立HTTPS连接并进行Basic Auth认证

### 2. 状态监控循环
1. **检查连接状态**: 确保与LCU的连接正常
2. **检测对局准备**: 监控是否有新的准备检查
3. **执行自动接受**: 如果启用且检测到准备检查，自动接受
4. **检测英雄选择**: 监控是否进入英雄选择阶段
5. **执行Ban/Pick**: 根据配置自动执行ban和pick操作

### 3. 状态管理
- **对局状态标记**: 防止重复操作同一场对局
- **Ban/Pick状态追踪**: 确保每个阶段只操作一次
- **错误恢复**: 连接断开时自动重连

## 英雄选择器功能

### 可视化界面
- **英雄头像**: 显示所有英雄的头像图片
- **搜索功能**: 支持中英文名称搜索
- **网格布局**: 每行6个英雄，便于浏览
- **滚动支持**: 鼠标滚轮浏览所有英雄

### 英雄数据
- **完整英雄库**: 包含所有当前版本英雄
- **本地缓存**: 头像图片本地缓存，提高加载速度
- **多语言支持**: 中英文名称对照
- **实时更新**: 支持搜索结果实时筛选

## 安全特性

### API安全
- **官方API**: 仅使用拳头游戏官方LCU API
- **本地连接**: 所有API调用都是本地连接
- **只读/写API**: 不修改游戏文件，仅调用允许的API端点

### 数据安全
- **无外部连接**: 除英雄头像下载外，无其他外部网络连接
- **本地配置**: 所有配置保存在本地文件
- **进程扫描**: 仅扫描League Client进程信息

## 故障排除

### 常见问题
1. **连接失败**: 
   - 确保英雄联盟客户端已启动并登录
   - 检查是否有多个League Client进程

2. **Ban/Pick失败**:
   - 确认英雄名称正确
   - 检查是否轮到自己的回合
   - 验证英雄是否已被ban或选择

3. **界面显示问题**:
   - 使用标准tkinter组件，兼容性更好
   - 检查系统Python和tkinter安装

### 调试功能
- **详细日志**: 控制台输出详细操作日志
- **状态显示**: GUI界面实时显示当前状态
- **错误提示**: 操作失败时显示具体错误信息

## 性能优化

### 监控效率
- **智能轮询**: 根据游戏状态调整检查频率
- **状态缓存**: 避免重复API调用
- **异步加载**: 英雄头像异步加载，不阻塞主界面

### 资源管理
- **内存控制**: 图片缓存管理，防止内存泄漏
- **连接复用**: 复用HTTP连接，减少开销
- **优雅退出**: 正确清理资源和线程

## 扩展功能

### 高级配置
- **多英雄配置**: 可配置备选ban/pick英雄列表
- **位置相关**: 根据选择的位置自动调整英雄选择
- **时间策略**: 配置不同时间段的不同策略

### 统计功能
- **操作记录**: 记录自动操作的成功率
- **对局统计**: 统计自动接受的对局数量
- **性能监控**: 监控API响应时间和成功率

---

该工具专为英雄联盟玩家设计，提供安全、稳定、易用的自动化功能，让您能更专注于游戏本身而非重复的点击操作。