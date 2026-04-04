# Football Tournament Web (Spring Boot + Thymeleaf)

Ứng dụng web quản lý giải đấu bóng đá theo mô hình Spring MVC (Controller → Service → Repository → Entity), giao diện dựng bằng Thymeleaf + static assets.

## Tính năng

### Người dùng
- Trang chủ / giới thiệu / liên hệ, hiển thị tin thể thao.
- Danh sách giải đấu, xem chi tiết giải: đội tham gia, lịch thi đấu, bảng đấu (group stage), bracket (knockout), thống kê & biểu đồ.
- Đăng ký tham gia giải (chọn đội có sẵn hoặc tạo đội), upload logo.
- Hồ sơ cá nhân, lịch sử đăng ký, lịch sử giao dịch ví.
- Nạp tiền ví (MoMo sandbox), dùng số dư để đóng phí đăng ký giải.
- Thông tin đội: lịch đấu, kết quả, phân tích hiệu suất đội theo giải; phân tích cầu thủ (top ghi bàn & thẻ, có avatar).

### Admin
- Dashboard, quản lý giải đấu (thêm/sửa/xóa), upload ảnh giải.
- Quản lý đội/đăng ký tham gia giải, cập nhật lịch thi đấu, đội hình, sự kiện trận đấu.
- Quản lý người dùng và giao dịch/hóa đơn.

## Công nghệ
- Java 17
- Spring Boot 4 (Web, Security, Validation)
- Spring Data JPA + MySQL
- Thymeleaf
- Chart.js (biểu đồ trên UI)
- Maven Wrapper

## Yêu cầu
- JDK 17
- MySQL 8+ (hoặc MySQL tương thích)

## Cấu hình

File cấu hình: [application.properties]

### Database
Ứng dụng đọc user/password DB từ biến môi trường:
- `DB_USERNAME` (mặc định `root`)
- `DB_PASSWORD` (mặc định rỗng)

Chuỗi kết nối mặc định:
```
jdbc:mysql://localhost:3306/TournamentDB?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

### MoMo (sandbox)
Project có cấu hình tham số MoMo trong `application.properties`. Khuyến nghị khi deploy/đẩy repo:
- Không commit key thật
- Override bằng biến môi trường hoặc secret manager

Các thông tin khác (api-url/return-url/notify-url/partner-code/request-type) cấu hình trong `application.properties`.

## Chạy ứng dụng

### Windows (PowerShell)
```powershell
.\mvnw spring-boot:run
```

### macOS/Linux
```bash
./mvnw spring-boot:run
```

Mặc định chạy tại: http://localhost:8080

## Tài khoản demo (seed)
Khi khởi động, hệ thống seed dữ liệu mẫu trong [DataSeeder.java](file:///c:/Users/acer/Desktop/java-football-tournament-front-end/src/main/java/com/example/football_tourament_web/config/DataSeeder.java):
- Admin: `admin@example.com` / `admin123`
- User: `a@example.com` / `user123`

## Routing chính (tham khảo nhanh)

### Public
- `/` hoặc `/home`: trang chủ
- `/tin-tuc`: tin tức
- `/lien-he`: liên hệ

### Auth
- `/dang-nhap`: đăng nhập
- `/dang-ky`: đăng ký
- `/dang-xuat`: đăng xuất

### Người dùng
- `/user/tournament`: danh sách giải
- `/user/tournament/match-schedule?id=...`: chi tiết giải & lịch
- `/user/tournament/sign-up?id=...`: đăng ký giải (yêu cầu đăng nhập)
- `/thong-tin-doi?teamId=...&tab=analysis&tournamentId=...`: trang đội của tôi (có phân tích)

### Admin
- `/admin/**`: khu vực quản trị (yêu cầu ROLE_ADMIN)

## Upload & static
- Static assets: `src/main/resources/static/assets`
- Upload (logo/ảnh): `src/main/resources/static/uploads`
- Ứng dụng phục vụ upload qua `/uploads/**` bằng cấu hình [WebConfig.java](file:///c:/Users/acer/Desktop/java-football-tournament-front-end/src/main/java/com/example/football_tourament_web/config/WebConfig.java).

Lưu ý: cách lưu upload hiện tại phù hợp chạy local/demo; khi deploy thực tế nên chuyển sang storage ngoài (S3, Cloud Storage, volume riêng…).

## Bảo mật
- Phân quyền cấu hình tại [SecurityConfig.java](file:///c:/Users/acer/Desktop/java-football-tournament-front-end/src/main/java/com/example/football_tourament_web/config/SecurityConfig.java)
  - `/admin/**` yêu cầu `ROLE_ADMIN`
  - Các trang hồ sơ/đội/chi trả yêu cầu `ROLE_USER` (một số route public theo cấu hình)

## Test
```bash
./mvnw test
```

## Cấu trúc thư mục
- `src/main/java/.../controller`: route & xử lý request
- `src/main/java/.../service`: nghiệp vụ (core/user/admin/common)
- `src/main/java/.../repository`: JPA queries
- `src/main/java/.../model/entity`: entity JPA
- `src/main/resources/templates`: Thymeleaf templates (admin/user)
- `src/main/resources/static`: CSS/JS/ảnh

## Troubleshooting
- IDE báo đỏ nhưng Maven build được: thử `./mvnw clean compile` và “Reload Maven Project”.
- Lỗi do cache build: “Rebuild project” hoặc “Invalidate caches & restart” (IntelliJ).
