# JetBrains Recorder Plugin 完整测试指南

## 🎯 测试步骤

### 1. 启动录制
- **方法1**: 在IDE底部状态栏找到 "⏺ Start Recording" 按钮，点击开始录制
- **方法2**: 使用菜单 Tools → Toggle Recording
- 录制开始后，按钮应变为 "⏹ Recording (0)"

### 2. 执行文件操作

#### 创建新文件
1. 右键项目根目录 → New → File
2. 创建 `test-file.txt`
3. 添加一些内容：
```
Hello World!
This is a test file for the recorder plugin.
```

#### 修改现有文件
1. 打开 `README.md`
2. 添加一行：`<!-- Test modification -->`
3. 保存文件 (Ctrl+S)

#### 删除文件
1. 右键 `test-file.txt`
2. 选择 Delete
3. 确认删除

### 3. 执行终端命令 (新功能!)

#### 打开终端
1. 使用 Alt+F12 或者 View → Tool Windows → Terminal
2. 确保终端窗口是活跃状态

#### 执行各种命令
```bash
# 基本命令
ls -la
pwd
cd src

# Git 命令
git status
git log --oneline
git diff

# 开发命令
npm install
python --version
java -version

# 文件操作命令
mkdir test-dir
touch new-file.txt
rm new-file.txt
```

### 4. 停止录制
- 点击状态栏的 "⏹ Recording (X)" 按钮
- 按钮应变回 "⏺ Start Recording"

### 5. 检查结果
- 在项目根目录查找 `operation.json` 文件
- 文件应包含类似以下的JSON数据：

```json
[
  {
    "timestamp": 1640995200000,
    "type": "FILE_CREATE",
    "path": "test-file.txt",
    "data": ""
  },
  {
    "timestamp": 1640995201000,
    "type": "FILE_CONTENT",
    "path": "test-file.txt",
    "data": "Hello World!\nThis is a test file for the recorder plugin.\n"
  },
  {
    "timestamp": 1640995202000,
    "type": "FILE_DIFF",
    "path": "README.md",
    "data": "diff --git a/README.md b/README.md\n..."
  },
  {
    "timestamp": 1640995203000,
    "type": "COMMAND",
    "command": "ls -la",
    "output": ""
  },
  {
    "timestamp": 1640995204000,
    "type": "COMMAND",
    "command": "git status",
    "output": ""
  },
  {
    "timestamp": 1640995205000,
    "type": "FILE_DELETE",
    "path": "test-file.txt",
    "data": ""
  }
]
```

## ✅ 预期行为

### 状态栏按钮
- 未录制时显示: "⏺ Start Recording"
- 录制时显示: "⏹ Recording (操作计数)"
- 点击可切换录制状态

### 文件操作捕获
- ✅ 文件创建 (FILE_CREATE)
- ✅ 文件内容 (FILE_CONTENT) - 对于新文件
- ✅ Git差异 (FILE_DIFF) - 对于已跟踪文件
- ✅ 文件删除 (FILE_DELETE)

### 终端命令捕获 (新功能!)
- ✅ 基本命令 (COMMAND) - ls, cd, pwd等
- ✅ Git命令 - git status, git commit等
- ✅ 开发工具命令 - npm, python, java等
- ✅ 文件操作命令 - mkdir, touch, rm等
- ✅ 复杂命令 - 带参数和选项的命令

### Git集成
- 自动检测文件是否被Git跟踪
- 对已跟踪文件生成diff
- 对新文件保存完整内容
- 遵循.gitignore规则

## 🐛 故障排除

### 如果看不到录制按钮
1. 检查插件是否正确加载
2. 查看IDE日志中的错误信息
3. 重启IDE

### 如果没有生成operation.json
1. 确保有文件操作发生
2. 检查项目根目录写入权限
3. 查看IDE日志中的错误信息

### 如果Git diff不工作
1. 确保项目在Git仓库中
2. 确保文件已被Git跟踪
3. 检查Git命令是否可用

## 📝 注意事项

1. 插件只在录制状态下捕获操作
2. 遵循.gitignore文件的过滤规则
3. 终端命令监听功能为简化版本
4. 大量文件操作可能影响性能
