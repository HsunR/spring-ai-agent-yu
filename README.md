# AI超级智能体应用平台 - 前端

基于 Vue 3 + Vite 构建的 AI 智能体应用前端项目。

## 技术栈

- **框架**: Vue 3 (Composition API)
- **构建工具**: Vite
- **路由**: Vue Router 4
- **HTTP 客户端**: Axios
- **SEO 管理**: @vueuse/head

## 项目结构

```
ai-agent-frontend/
├── src/
│   ├── api/              # API 接口封装
│   │   └── index.js      # SSE 连接和 API 方法
│   ├── components/       # 公共组件
│   │   ├── ChatRoom.vue      # 聊天室组件
│   │   └── AiAvatarFallback.vue  # AI 头像组件
│   ├── views/            # 页面视图
│   │   ├── Home.vue           # 首页
│   │   ├── LoveMaster.vue     # AI恋爱大师
│   │   ├── SpringAiKnowledge.vue  # Spring AI 知识库
│   │   └── SuperAgent.vue     # AI超级智能体
│   ├── router/           # 路由配置
│   │   └── index.js
│   ├── App.vue           # 根组件
│   └── main.js           # 入口文件
├── index.html
├── package.json
└── vite.config.js
```

## 功能模块

### 1. 首页 (Home)
- 赛博朋克风格设计
- 粒子背景动画效果
- 打字机效果副标题
- 3D 卡片悬停效果
- 应用入口导航

### 2. AI超级智能体 (SuperAgent)
- 全能型 AI 助手对话
- 智能分句气泡显示
- 打字机效果回复
- SSE 流式响应

### 3. Spring AI 知识库 (SpringAiKnowledge)
- 基于 RAG 的知识检索
- 知识片段召回展示
- 相关性评分显示
- 可选择基于召回内容回答

### 4. AI恋爱大师 (LoveMaster)
- 情感顾问对话
- 恋爱问题解答
- 情感建议提供

## 核心功能特性

### 聊天室组件 (ChatRoom)
- 消息气泡展示（用户/AI）
- 长消息折叠/展开
- 打字机效果指示器
- 知识片段卡片展示
- 响应式布局适配

### SSE 实时通信
- 流式消息接收
- 连接状态管理
- 错误处理机制

### 响应式设计
- 桌面端适配
- 平板端适配
- 移动端适配

## 开发环境配置

### 安装依赖
```bash
npm install
```

### 启动开发服务器
```bash
npm run dev
```

### 构建生产版本
```bash
npm run build
```

### 预览生产构建
```bash
npm run preview
```

## API 接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/ai/love_app/chat/sse` | SSE | AI恋爱大师对话 |
| `/api/ai/manus/chat` | SSE | AI超级智能体对话 |
| `/api/ai/spring_ai/chat/sse` | SSE | Spring AI 知识库对话 |
| `/api/ai/spring_ai/retrieve` | GET | 检索相关知识片段 |

## 环境变量

- 开发环境: `http://localhost:8123/api`
- 生产环境: `/api`

## 路由列表

| 路径 | 页面 | 描述 |
|------|------|------|
| `/` | 首页 | 应用入口和导航 |
| `/super-agent` | AI超级智能体 | 全能AI助手 |
| `/spring-ai-knowledge` | Spring AI 知识库 | 技术文档问答 |
| `/love-master` | AI恋爱大师 | 情感顾问 |
