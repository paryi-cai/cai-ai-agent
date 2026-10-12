# cai-ai-agent

基于 **Spring AI** 与 **Spring AI Alibaba** 构建的 AI 超级智能体项目：覆盖大模型接入、RAG 知识库、工具调用、MCP 协议、自主智能体与流式服务化的完整实践。

## 功能特性

### 菜狗探店（FoodApp）
- AI 美食探店博主人设：深圳各区好店推荐、菜系讲解、避雷指南
- 多轮对话与对话记忆（内存 / 文件持久化）
- 结构化输出（AI 回答自动转换为探店报告对象）
- Prompt 模板与自定义 Advisor（日志、重读增强）

### RAG 知识库
- 本地知识库：深圳美食文档加载、切片、向量化（PostgreSQL + PGVector）
- 云知识库：阿里云百炼云知识库对接
- 检索增强：查询重写 / 翻译 / 压缩 / 多查询扩展、**区域元数据过滤**、空上下文兜底

### 工具调用
文件操作、联网搜索、网页抓取、终端操作、资源下载、PDF 生成、用户档案查询、时间工具、任务终止工具（AI 自主决策调用）

### MCP 协议
- 图片搜索 MCP 服务（独立模块 `cai-image-search-mcp-server`，支持 stdio / SSE 双传输）
- 高德地图 MCP 服务（地理编码、周边搜索、路线规划等 12 个工具）

### AI 智能体（CaiManus）
ReAct 模式 + 手动工具调用循环：自主规划、多步执行复杂任务（例如"搜索约会地点 + 结合图片生成 PDF 计划"）

### AI 服务化
- REST 同步接口 + SSE 流式接口（打字机效果）
- 智能体分步流式输出接口
- Vue3 前端（独立目录 `cai-ai-agent-frontend`）

## 技术栈

| 类别 | 技术 |
|---|---|
| 语言 / 框架 | Java 21、Spring Boot 3.4.4 |
| AI 框架 | Spring AI 1.0.0、Spring AI Alibaba 1.0.0.2 |
| 大模型 | 通义千问（DashScope） |
| 向量库 | PostgreSQL + PGVector（阿里云 RDS） |
| MCP | Spring AI MCP（stdio / SSE） |
| 工具库 | Hutool、Lombok、iText |
| 接口文档 | SpringDoc、Knife4j |
| 前端 | Vue3、Vite（独立项目） |

## 项目结构

```
cai-ai-agent/
├── src/main/java/com/cai/caiaiagent/
│   ├── agent/            # AI 智能体（BaseAgent / ReActAgent / ToolCallAgent / CaiManus）
│   ├── app/              # 核心应用（FoodApp 菜狗探店、AiController）
│   ├── advisor/          # 自定义 Advisor
│   ├── chatmemory/       # 对话记忆（文件持久化）
│   ├── config/           # 全局配置（跨域等）
│   ├── controller/       # 接口层（AiController / HealthController）
│   ├── rag/              # RAG 知识库（深圳美食文档加载 / 检索增强）
│   ├── tools/            # 工具集（9 个工具 + 工具注册）
│   └── demo/             # 各章节示例代码
├── cai-image-search-mcp-server/   # 图片搜索 MCP 服务（独立模块）
├── cai-ai-agent-frontend/         # Vue3 前端（独立项目）
├── Dockerfile                     # 后端容器构建
└── pom.xml
```

## 快速开始

### 环境要求
- JDK 21、Maven 3.9+
- Node.js（图片搜索 MCP 依赖 npx；前端开发需要）

### 1. 配置密钥
```yaml
spring:
  datasource:            # PostgreSQL（PGVector）连接信息
    url: jdbc:postgresql://<host>:5432/<database>
    username: <username>
    password: <password>
  ai:
    dashscope:
      api-key: <阿里云百炼 API Key>
search-api:
  api-key: <SearchAPI Key>
```

### 2. 打包图片搜索 MCP 服务（智能体/工具链依赖）
```bash
cd cai-image-search-mcp-server
mvn package -DskipTests
```

### 3. 启动后端
```bash
mvn spring-boot:run
```
- 服务地址：http://localhost:8123/api
- 接口文档：http://localhost:8123/api/doc.html

### 4. 启动前端（可选）
```bash
cd cai-ai-agent-frontend
npm install
npm run dev
```

## 主要接口

| 路径（含 /api 前缀） | 说明 |
|---|---|
| GET /api/ai/food_app/chat/sync | 菜狗探店 · 同步对话（完整返回） |
| GET /api/ai/food_app/chat/sse | 菜狗探店 · SSE 流式（打字机效果） |
| GET /api/ai/manus/chat | AI 智能体 · SSE 分步流式 |

流式接口测试示例：

```bash
curl -N "http://localhost:8123/api/ai/food_app/chat/sse?message=hello&chatId=1"
```

## 部署

- 后端：`Dockerfile` 支持两种构建方式（运行时打包 / 预打包），生产配置见 `src/main/resources/application-prod.yml`
- 前端：构建产物 `npm run build` 后部署至静态托管或容器

## 说明

- 本项目为个人学习实践项目
