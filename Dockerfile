# ── Stage 1: Build ──────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# 先只拷贝 pom.xml 下载依赖，利用 layer 缓存
COPY pom.xml .
RUN mvn dependency:go-offline -q

# 再拷贝源码并打包
COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 创建非 root 运行用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 文件上传目录
RUN mkdir -p /app/data/uploads && chown -R appuser:appgroup /app

COPY --from=builder /app/target/projectflow-backend-*.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
