# 构建阶段：先下载依赖再复制源码，源码变化时可复用依赖缓存层
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/ src/
# 测试和格式检查由 CI 负责，镜像构建只打包
RUN ./mvnw -B -q package -DskipTests -Dspotless.check.skip=true

# 运行阶段：只包含 JRE 和 jar，以非 root 用户运行
FROM eclipse-temurin:17-jre
RUN groupadd --system app && useradd --system --gid app app \
    && mkdir -p /data && chown app:app /data
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
