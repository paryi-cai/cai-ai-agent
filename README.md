# cai-ai-agent

「菜狗探店」——基于 **Spring AI** 与 **Spring AI Alibaba** 构建的 AI 美食探店智能体平台。

覆盖大模型接入、RAG 知识库、工具调用、MCP 协议、自主智能体与流式服务化的完整实践：后端提供对话与智能体能力，前端打造类 GPT 的流式交互体验。

## 项目简介

- **AI 应用**：以「菜狗探店」为场景的对话式 AI（深圳美食探店博主），支持多轮记忆、结构化输出与提示词模板
- **RAG 知识库**：本地 PostgreSQL + PGVector 与阿里云百炼云知识库双通道检索，支持区域元数据过滤与多种检索调优
- **工具与 MCP**：9 个自研工具 + 通过 MCP 协议接入高德地图、自研图片搜索 MCP 服务端
- **自主智能体**：CaiManus 基于 ReAct 模式自主规划、多步执行复杂任务（搜索 → 下载 → 生成 PDF）
- **服务化**：同步接口 + 多种 SSE 流式接口 + Vue3 前端（打字机效果、智能体分步可视化）

## 功能特性

### 菜狗探店（FoodApp）
- 探店博主人设：深圳各区（南山 / 福田 / 罗湖 / 宝安）好店推荐、菜系讲解、避雷指南
- 检索范围可选：前端选择区域后，后端按元数据过滤、只检索该区域知识
- 多轮对话与持久化记忆（文件存储 + 定时清理过期会话）
- 结构化输出（自动转换为探店报告对象）、Prompt 模板、自定义 Advisor（日志 / 重读增强）

### RAG 知识库
- 本地知识库：深圳美食文档 ETL（加载 / 切分 / 向量化，36 个知识切片）入库 PostgreSQL + PGVector
- 云知识库：阿里云百炼云知识库对接（文档解析、切分与索引由平台完成）
- 检索增强：区域元数据过滤、查询重写 / 翻译 / 压缩 / 多查询扩展、空上下文兜底话术

### 工具调用（9 个自研工具）

| 工具 | 说明 |
|---|---|
| FileOperationTool | 文件读写 |
| WebSearchTool | 联网搜索（SearchAPI） |
| WebScrapingTool | 网页内容抓取 |
| ResourceDownloadTool | 资源下载（图片等） |
| TerminalOperationTool | 终端命令执行 |
| PDFGenerationTool | PDF 文档生成 |
| UserProfileTool | 用户档案查询（演示 ToolContext 内部上下文） |
| TimeTool | 时间查询 |
| TerminateTool | 任务终止（供智能体判断任务完成） |

### MCP 协议
- 自研图片搜索 MCP 服务端（独立模块 `cai-image-search-mcp-server`，stdio / SSE 双传输，协议握手与工具发现实测通过）
- 高德地图 MCP 服务（地理编码、周边搜索、路线规划、天气等 12 个工具）

### AI 智能体（CaiManus）
- ReAct 模式：思考（think）→ 行动（act）循环，直到任务完成或达到步数上限
- 模板方法四层架构：BaseAgent（执行循环）/ ReActAgent（思考行动拆分）/ ToolCallAgent（手动工具调用）/ CaiManus（配置组装）
- 手动接管工具调用循环（禁用框架托管执行），每一步的思考内容与工具结果全量可观测
- 终止工具 + 步数上限双保险，防止死循环；消息上下文自主维护，支持多步自我纠错

### AI 服务化
- 后端：同步接口 + 3 种 SSE 流式实现（Flux / ServerSentEvent / SseEmitter）+ 智能体分步流式
- 前端：Vue3 + EventSource 打字机效果、智能体执行过程分步气泡、区域选择器

## 系统架构

```
┌──────────────────┐      HTTP / SSE        ┌──────────────────────────────┐
│    Vue 3 前端     │  ◄──────────────────►  │      Spring Boot 后端         │
│  · 打字机效果      │                        │   /api/ai/food_app/chat/*    │
│  · 智能体分步气泡   │                        │   /api/ai/manus/chat         │
│  · 区域选择器      │                        └───────────────┬──────────────┘
└──────────────────┘                                        │
        ┌──────────────────────┬─────────────────────────────┼──────────────────────┐
        │                      │                             │                      │
 ┌──────▼───────┐      ┌───────▼────────┐           ┌────────▼────────┐    ┌────────▼────────┐
 │  RAG 知识库    │      │   工具调用       │           │   MCP 生态       │    │  智能体 CaiManus  │
 │ · PGVector    │      │ · 9 个自研工具   │           │ · 图片搜索服务端  │    │ · ReAct 循环      │
 │ · 百炼云知识库  │      │ · 工具上下文     │           │ · 高德地图 MCP    │    │ · 自主规划执行     │
 └──────────────┘      └────────────────┘           └─────────────────┘    └─────────────────┘
                外部服务：通义千问 DashScope · 阿里云 RDS(PGVector) · SearchAPI · 图片搜索
```

## 智能体工作流程

```
用户任务
  → [think] 请求模型：是否调用工具、调用哪些
  → [act] 手动执行工具调用，结果写入消息上下文
  → 循环（每步可观测、可纠错）
  → 模型调用 terminate 或达到步数上限 → 汇总每步执行结果
```

## 技术栈

| 类别 | 技术 |
|---|---|
| 语言 / 框架 | Java 21、Spring Boot 3.4.4 |
| AI 框架 | Spring AI 1.0.0、Spring AI Alibaba 1.0.0.2 |
| 大模型 | 通义千问（DashScope） |
| 向量库 | PostgreSQL + PGVector（阿里云 RDS） |
| MCP | Spring AI MCP（stdio / SSE） |
| 工具库 | Hutool、Lombok、iText、Kryo（记忆序列化） |
| 接口文档 | SpringDoc、Knife4j |
| 前端 | Vue3、Vite、EventSource |
| 部署 | Docker / Serverless |

## 项目结构

```
cai-ai-agent/
├── src/main/java/com/cai/caiaiagent/
│   ├── agent/            # AI 智能体（BaseAgent / ReActAgent / ToolCallAgent / CaiManus）
│   ├── app/              # 核心应用（FoodApp 菜狗探店）
│   ├── advisor/          # 自定义 Advisor（日志、重读增强）
│   ├── chatmemory/       # 对话记忆（文件持久化 + 定时清理）
│   ├── config/           # 全局配置（跨域等）
│   ├── controller/       # 接口层（AiController / HealthController）
│   ├── rag/              # RAG（文档加载 / 向量存储 / 检索增强 / Advisor 工厂）
│   ├── tools/            # 工具集（9 个工具 + 工具注册）
│   └── demo/             # 各章节示例代码
├── cai-image-search-mcp-server/   # 图片搜索 MCP 服务（独立模块）
├── cai-ai-agent-frontend/         # Vue3 前端（独立项目）
├── Dockerfile                     # 后端容器构建（含构建加速配置）
└── pom.xml
```

## 快速开始

### 环境要求
- JDK 21、Maven 3.9+
- Node.js（高德地图 MCP 依赖 npx；前端开发与构建需要）

### 1. 配置密钥
在 `src/main/resources/` 下创建 `application-local.yml`（该文件已被 .gitignore 忽略，不会提交），参考 `application.yml` 注释填入：

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

### 2. 打包图片搜索 MCP 服务
```bash
cd cai-image-search-mcp-server
mvn package -DskipTests
```
说明：stdio 模式下，后端启动时会自动拉起该服务的 jar 子进程（图片搜索工具依赖它）。

### 3. 启动后端
```bash
mvn spring-boot:run
```
- 服务地址：http://localhost:8123/api
- 首次启动会自动加载知识库文档并向量化入库

### 4. 启动前端（可选）
```bash
cd cai-ai-agent-frontend
npm install
npm run dev
```
- 默认地址：http://localhost:5173

### 5. 验证
- 接口文档：http://localhost:8123/api/doc.html
- 流式对话：

```bash
# 全库检索
curl -N "http://localhost:8123/api/ai/food_app/chat/sse?message=hello&chatId=1"
# 按区域过滤检索
curl -N "http://localhost:8123/api/ai/food_app/chat/sse?message=南山有什么好吃的&chatId=1&region=南山"
# 智能体分步执行
curl -N "http://localhost:8123/api/ai/manus/chat?message=帮我生成一份深圳一日美食探店计划"
```

## 接口一览

| 路径（含 /api 前缀） | 说明 |
|---|---|
| GET /api/ai/food_app/chat/sync | 菜狗探店 · 同步对话（完整返回） |
| GET /api/ai/food_app/chat/sse | 菜狗探店 · SSE 流式（Flux，支持 region 过滤） |
| GET /api/ai/food_app/chat/sse2 | 菜狗探店 · SSE（自定义事件字段） |
| GET /api/ai/food_app/chat/sse3 | 菜狗探店 · SSE（SseEmitter 手动推送） |
| GET /api/ai/manus/chat | AI 智能体 · SSE 分步流式 |

参数说明：

| 参数 | 必填 | 说明 |
|---|---|---|
| message | 是 | 用户消息（中文需 URL 编码） |
| chatId | 否 | 会话 ID，用于多轮对话记忆 |
| region | 否 | 检索区域（南山 / 福田 / 罗湖 / 宝安），不传为全库检索 |

## 部署

- **后端**：仓库自带 `Dockerfile`（运行时打包 / 预打包两种方式，见文件注释），并附带 Docker 构建加速配置 `settings.xml`
- **生产配置**：`application-prod.yml`（生产环境建议将密钥改为平台环境变量注入；已处理 MCP 子进程与跨域等生产细节）
- **前端**：`npm run build` 产物可部署到任意静态托管或容器（Nginx / 云托管 / SAE 等）

## 说明

- 仓库内不包含任何真实密钥：API Key、数据库密码等均存放于 gitignore 的本地配置文件中
- 本项目为个人学习实践项目，按章节递进开发：大模型接入 → RAG → 工具调用 → MCP → 智能体 → 服务化
