# ---- Stage build (biên dịch & đóng gói) ----
FROM maven:3.9.9-eclipse-temurin-17 AS build
#Set thư mục làm việc trong container
WORKDIR /workspace
# Copy pom trước để Docker cache bước tải dependencies
COPY pom.xml .
# Tải dependencies (bỏ qua test để build nhanh hơn)
RUN mvn -q -DskipTests dependency:go-offline
# Copy source code vào container và build file .jar Spring Boot
COPY src ./src
RUN mvn -q -DskipTests package

# ---- Stage runtime (image nhỏ hơn) ----
FROM eclipse-temurin:17-jre-jammy
#Thư mục chạy /app trong container
WORKDIR /app
# Thư mục dự án dùng để lưu file upload
RUN mkdir -p /app/src/main/resources/static/uploads
# Copy file .jar đã build từ stage build vào image runtime
COPY --from=build /workspace/target/*.jar /app/app.jar
# Port của app chạy ở 8080
EXPOSE 8080
# Tuỳ chọn JVM flags (vd: -Xms256m -Xmx512m)
ENV JAVA_OPTS=""
# Chạy ứng dụng
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
