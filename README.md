# BaiTap10 - Demo JWT với Spring Boot 3 – Security 6

Project ví dụ theo bài giảng **Json Web Token** (Lập trình Web – WEBPR330479, ThS. Nguyễn Hữu Trung).
Người dùng đăng ký tài khoản, đăng nhập để nhận JWT, rồi gửi JWT trong header
`Authorization: Bearer <token>` để truy cập các API được bảo vệ.

- Thư viện JWT: `io.jsonwebtoken` (jjwt) 0.12.6, ký bằng HMAC‑SHA256 (HS256)
- Spring Boot 3.5, Spring Security 6, Spring Data JPA, Thymeleaf, MySQL, Lombok
- Java 17 trở lên

## Cấu trúc project

```
src/main/java/vn/iotstar
├── entity/User.java                    # Bước 2: Entity (implements UserDetails)
├── models/                             # Bước 3: LoginResponse, LoginUserModel, RegisterUserModel
├── repository/UserRepository.java      # Bước 4: Repository
├── services/                           # Bước 4: UserService, AuthenticationService, JwtService
├── config/ApplicationConfiguration.java# Bước 5: UserDetailsService, PasswordEncoder, AuthenticationProvider
├── filter/JwtAuthenticationFilter.java # Bước 6: Filter đọc và kiểm tra JWT
├── config/SecurityConfiguration.java   # Bước 7: SecurityFilterChain + CORS
├── controller/                         # Bước 8: AuthenticationController, UserController; Bước 10: AuthController (view)
└── exceptions/GlobalExceptionHandler.java # Bước 10: Ném Exception
src/main/resources
├── templates/login.html, profile.html  # Bước 10: Render bằng Ajax
├── static/js/mainjs.js
└── application.properties
```

## Cấu hình

Sửa `src/main/resources/application.properties` cho đúng MySQL của bạn
(database `jwt_springboot3` sẽ được tự tạo nếu chưa có):

```properties
server.port=8005
spring.datasource.url=jdbc:mysql://localhost:3306/jwt_springboot3?createDatabaseIfNotExist=true&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false
spring.datasource.username=root
spring.datasource.password=1234567@a$
security.jwt.secret-key=3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b
# 1h in millisecond
security.jwt.expiration-time=3600000
```

## Chạy ứng dụng

```bash
mvn spring-boot:run
```

Chạy test (dùng H2 trong bộ nhớ, không cần MySQL):

```bash
mvn test
```

## Thử với Postman / curl

1. Đăng ký tài khoản

```bash
curl -X POST http://localhost:8005/auth/signup -H "Content-Type: application/json" \
  -d '{"email":"trung@hcmute.edu.vn","password":"123456","fullName":"Nguyễn Hữu Trung","images":"/images/u1.jpg"}'
```

2. Đăng nhập để sinh JWT

```bash
curl -X POST http://localhost:8005/auth/login -H "Content-Type: application/json" \
  -d '{"email":"trung@hcmute.edu.vn","password":"123456"}'
# => {"token":"eyJhbGciOiJIUzI1NiJ9....","expiresIn":3600000}
```

3. Gọi API được bảo vệ, gửi token với kiểu Authorization là Bearer Token

```bash
curl http://localhost:8005/users/me -H "Authorization: Bearer <token>"
curl http://localhost:8005/users/   -H "Authorization: Bearer <token>"
```

4. Giao diện Ajax: mở `http://localhost:8005/login`, đăng nhập, trang sẽ chuyển sang
`/user/profile` và hiển thị họ tên, ảnh của người dùng lấy từ API `/users/me`.

## Xử lý lỗi (Bước 10)

| Lỗi xác thực | Ngoại lệ | Mã HTTP |
|---|---|---|
| Thông tin đăng nhập không hợp lệ | BadCredentialsException | 401 |
| Tài khoản bị khóa | AccountStatusException | 403 |
| Không được phép truy cập tài nguyên | AccessDeniedException | 403 |
| JWT không hợp lệ (sai chữ ký, sai định dạng) | SignatureException / JwtException | 401 |
| JWT đã hết hạn | ExpiredJwtException | 401 |
