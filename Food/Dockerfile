# GIAI ĐOẠN 1: BUILD ỨNG DỤNG (BUILDER STAGE)
# Sử dụng image Maven chính thức để build code
FROM maven:3.8.7-eclipse-temurin-17 AS builder

# Thiết lập thư mục làm việc trong Stage 1
WORKDIR /app

# Sao chép các file cấu hình và mã nguồn
COPY pom.xml .
COPY Food/pom.xml Food/
# Tải dependencies trước để tận dụng cache
RUN mvn dependency:go-offline -DskipTests

# Sao chép toàn bộ mã nguồn vào image Builder
COPY . .

# Chạy lệnh build Maven để tạo file JAR
# -DskipTests để bỏ qua test, tránh lỗi hoặc chậm quá trình build
RUN mvn clean package -DskipTests

---

# GIAI ĐOẠN 2: TẠO IMAGE CHẠY CUỐI CÙNG (FINAL STAGE)
# Sử dụng JRE tối thiểu, nhỏ gọn hơn (Alpine)
FROM eclipse-temurin:17-jdk-alpine

# CÀI ĐẶT CƠ BẢN
# Cài đặt múi giờ
RUN apk add --no-cache tzdata
# Thiết lập múi giờ
ENV TZ="Asia/Ho_Chi_Minh"

# Thiết lập thư mục làm việc cho ứng dụng
WORKDIR /app

# Tạo thư mục uploads (nếu cần)
RUN mkdir -p /app/uploads

# SAO CHÉP FILE JAR
# Lấy file JAR đã được build từ Stage 1 (builder)
# Đường dẫn: /app/target/Tên_file_JAR.jar
# (Giả sử file JAR của bạn có tên là Food-0.0.1-SNAPSHOT.jar)
COPY --from=builder /app/target/Food-0.0.1-SNAPSHOT.jar /app/app.jar

# CỔNG VÀ LỆNH CHẠY
EXPOSE 8080

# Lệnh khởi động ứng dụng
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", \
            "/app/app.jar"]