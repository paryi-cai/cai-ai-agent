# ============================================================
# AI 超级智能体 - 后端 Dockerfile
# 方式①运行时打包：构建镜像时在容器内执行 Maven 打包（无需本地预编译，推荐）
# 构建上下文需要：pom.xml、src/、settings.xml、本文件
# ============================================================

# ---------- 构建阶段：Maven + JDK21 ----------
FROM maven:3.9-amazoncorretto-21 AS build
WORKDIR /app

# 使用阿里云 Maven 镜像加速依赖下载（国内构建必需，否则拉依赖极慢甚至失败）
COPY settings.xml /root/.m2/settings.xml

# 只复制构建必需的文件（不要把 target、.idea、教程等无关内容拷进镜像）
COPY pom.xml .
COPY src ./src

# 执行打包（跳过测试）
RUN mvn clean package -DskipTests

# ---------- 运行阶段：只带 JRE，镜像更小 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# 复制构建产物（jar 名 = artifactId-版本号.jar）
COPY --from=build /app/target/cai-ai-agent-0.0.1-SNAPSHOT.jar app.jar

# 暴露后端端口（与 server.port 一致）
EXPOSE 8123

# 以 prod 配置启动（加载 application-prod.yml，其中已禁用本地 MCP 子进程）
CMD ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]

# ============================================================
# 方式②预打包（备选）：本地先执行 mvn package，把 jar 直接拷进镜像
# 优点：构建快（不用拉 Maven 依赖）；缺点：每次部署需要手动打 jar，容易忘记更新
# 使用方法：把上面两段 FROM 全部替换为以下内容即可
# ------------------------------------------------------------
# FROM eclipse-temurin:21-jre
# WORKDIR /app
# COPY target/cai-ai-agent-0.0.1-SNAPSHOT.jar app.jar
# EXPOSE 8123
# CMD ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
# ============================================================
