# 茶药通后端镜像
# 构建前提：宿主机/CI 已执行 mvn package 产出 target/chayetong-server-1.0.0.jar
# 构建：docker build -t chayetong-server:latest .
FROM eclipse-temurin:17-jre

# 容器时区对齐业务（订单日期按 Asia/Shanghai 分组）
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app
COPY target/chayetong-server-1.0.0.jar app.jar

# 端口仅容器内使用，云上由 nginx 反代（CYT_PORT 与之一致即可）
EXPOSE 8080

# 数据源/JWT/上传目录全部走 CYT_* 环境变量（见 deploy/docker-compose.yml）
ENTRYPOINT ["java", "-jar", "app.jar"]
