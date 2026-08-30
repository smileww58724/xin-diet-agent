# ---- 构建阶段：Maven 编译打包 ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 先复制构建描述文件，利用 Docker 层缓存：pom 不变时跳过依赖下载
COPY pom.xml .mvn-online-settings.xml ./
RUN mvn -s .mvn-online-settings.xml -B -q dependency:go-offline

COPY src ./src
RUN mvn -s .mvn-online-settings.xml -B -q package -DskipTests

# ---- 运行阶段：仅带 JRE 的精简镜像 ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/diet-agent-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
