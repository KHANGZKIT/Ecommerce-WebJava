# KẾ HOẠCH THỰC HIỆN DỰ ÁN & PHÂN CÔNG CÔNG VIỆC
## HỆ THỐNG THƯƠNG MẠI ĐIỆN TỬ MICROSERVICES QUAN SÁT TOÀN DIỆN (TECHGEAR E-COMMERCE)

---

### 1. KIẾN TRÚC HỆ THỐNG & CÔNG NGHỆ (TECH STACK)

#### 1.1. Lựa chọn công nghệ chi tiết
* **Nền tảng cốt lõi (Core Stack):** Java 17 (LTS), Spring Boot 3.2.x, Spring Cloud 2023.0.x (Spring Cloud Gateway, OpenFeign, Resilience4j Circuit Breaker).
* **Cơ sở dữ liệu & Phân tán (Database per Service):** PostgreSQL 16 (4 database tách biệt hoàn toàn cho từng service: `user_db`, `product_db`, `order_db`, `payment_db`), đảm bảo tính đóng gói và độc lập dữ liệu.
* **Bộ nhớ đệm & Giới hạn tải (Cache & Rate Limiting):** Redis 7 (Redis Cache L2 cho danh mục/chi tiết sản phẩm, lưu trữ giỏ hàng tạm, phân tán Redis Rate Limiter tại Gateway).
* **Cổng thanh toán & Tích hợp đối tác (Payment & Webhook):** Tích hợp MoMo Payment Gateway v3 (chuẩn `captureWallet` - All-in-One One-Time Payment, xác thực chữ ký điện tử HMAC-SHA256, Server-to-Server Asynchronous IPN Webhook) và phương thức COD (Cash on Delivery).
* **Giao diện người dùng (Frontend):** Next.js 16 (App Router), React 19, TypeScript, Tailwind CSS 4, Radix UI & Shadcn components theo triết lý thiết kế Swiss Minimalism, Sonner Toast thông báo, Recharts phân tích số liệu.
* **Hạ tầng DevOps & Khả năng quan sát (DevOps & Observability):** Docker & Docker Compose container hóa toàn bộ môi trường (áp dụng giới hạn tài nguyên RAM/CPU cho từng container), Spring Boot Actuator, Micrometer Prometheus metrics, Distributed Tracing (Micrometer Tracing / OpenTelemetry, correlation ID qua HTTP headers và log có cấu trúc).

#### 1.2. Danh sách các Microservices trong hệ thống
1. **API Gateway (Port 8080):** Spring Cloud Gateway làm điểm tiếp nhận duy nhất (Single Point of Entry), định tuyến reverse-proxy `/api/v1/*`, kiểm soát CORS tập trung, xử lý xác thực/giải mã JWT, rate limiting qua Redis.
2. **User & Auth Service (Port 8081):** Spring Security 6, quản lý người dùng, mã hóa mật khẩu BCrypt, phân quyền vai trò (Role: `CUSTOMER`, `ADMIN`), cấp phát và xác thực JWT token, hồ sơ tài khoản (Profile CRUD).
3. **Product & Inventory Service (Port 8082):** Quản lý danh mục thiết bị công nghệ (Laptop, Phụ kiện, Âm thanh, Linh kiện), thương hiệu, quản lý tồn kho, cơ chế giữ hàng tạm (Stock Hold / Release / Deduct) và Redis Cache hiệu năng cao.
4. **Order & Cart Service (Port 8084):** Quản lý giỏ hàng người dùng (Cart & Cart Items), điều phối Checkout Saga, quản lý vòng đời đơn hàng (`PENDING` -> `PAID` / `CANCELLED` -> `SHIPPED`), xử lý Idempotency Key chống trùng lặp đơn hàng.
5. **Payment Service (Port 8085):** Quản lý giao dịch thanh toán COD và MoMo Sandbox v3 API, tạo liên kết thanh toán (`payUrl`), QR code, tiếp nhận và kiểm tra chữ ký số HMAC-SHA256 cho IPN Webhook bất đồng bộ, xử lý giả lập hoàn tiền (Refund).

---

### 2. PHÂN CHIA VAI TRÒ & PHẠM VI TRÁCH NHIỆM

| Thành viên | Vai trò chính | Phạm vi phụ trách (Scope of Ownership) |
| :--- | :--- | :--- |
| **Lê Việt Anh** *(Trưởng nhóm)* | **Tech Lead & System Architect / Payment Engineer** | • Thiết kế kiến trúc tổng thể Microservices, Service Contracts và chuẩn hóa RESTful API v1.<br>• Xây dựng Spring Cloud Gateway (Reverse-proxy routing, Redis Rate Limiting, JWT Validation Filter, CORS).<br>• Phát triển Payment Service: Tích hợp MoMo Sandbox v3 API (`captureWallet`), xác thực chữ ký bảo mật HMAC-SHA256, xử lý Server-to-Server IPN Webhook.<br>• Điều phối tiến độ, rà soát mã nguồn (Code Review), giải quyết xung đột tích hợp và quản lý chất lượng kỹ thuật. |
| **Đàm Trung Anh** | **Core Backend & DevOps / Data Engineer** | • Thiết kế lược đồ 4 cơ sở dữ liệu quan hệ PostgreSQL độc lập và viết `docker-compose.yml` (chuẩn hóa network, healthcheck, RAM limit).<br>• Phát triển User & Auth Service (Spring Security 6, JWT Login/Register, phân quyền RBAC).<br>• Phát triển Product & Inventory Service: CRUD danh mục/sản phẩm, bộ lọc đa tiêu chí, cơ chế kho (Hold/Release/Deduct) và Redis Cache.<br>• Phát triển Order Service: Giỏ hàng, điều phối luồng Checkout Saga liên service qua WebClient và kiểm thử nghiệp vụ. |
| **Phạm Công Anh** | **Frontend Lead & UX/UI Engineer** | • Thiết kế & hiện thực hóa toàn bộ giao diện Web Next.js 16 / React 19 theo phong cách Swiss Minimalism hiện đại, Responsive đa màn hình.<br>• Xây dựng các trang nghiệp vụ: Trang chủ, Danh mục sản phẩm & Bộ lọc thông số kỹ thuật, Chi tiết sản phẩm.<br>• Xây dựng luồng Giỏ hàng (Cart Sheet / Page), luồng Đặt hàng (Checkout Form) và trang kết quả xử lý thanh toán MoMo Return URL.<br>• Xây dựng bảng điều khiển quản trị viên (Admin Dashboard) với biểu đồ doanh thu (Recharts) và quản lý đơn hàng.<br>• Tích hợp API Gateway, lưu trữ state người dùng, JWT token và xử lý thông báo phản hồi (Sonner Toast). |

---

### 3. KẾ HOẠCH TRIỂN KHAI 4 TUẦN (SPRINT ROADMAP)

#### Tuần 1: Khởi tạo Hạ tầng Đa Cơ sở dữ liệu, API Gateway & Dịch vụ Xác thực Người dùng
* **Mục tiêu Sprint:** Thiết lập hoàn chỉnh môi trường container hóa với 4 Database PostgreSQL riêng biệt và Redis; đưa API Gateway vào hoạt động; hoàn thiện toàn diện dịch vụ User & Auth Service cấp phát JWT.
* **Kết quả bàn giao (Milestone T1):**
  - File `docker-compose.yml` kích hoạt ổn định 4 PostgreSQL databases (`user_db`, `product_db`, `order_db`, `payment_db`) và Redis 7 kèm healthcheck.
  - API Gateway cấu hình routing reverse-proxy thông suốt đến các service nội bộ.
  - User Service cung cấp đầy đủ API Đăng ký (`/register`), Đăng nhập (`/login`), trả về JWT Token và mã hóa mật khẩu an toàn.
  - Khung ứng dụng Frontend (Next.js 16 + Tailwind CSS 4) dựng xong cấu trúc thư mục, Header, Navigation, Footer và hệ thống Design Tokens.

#### Tuần 2: Xây dựng Catalog Sản phẩm & Tồn kho (Product Service), Redis Caching & Khung Giao diện Web
* **Mục tiêu Sprint:** Product Service cung cấp đầy đủ API quản lý danh mục công nghệ, kho hàng, bộ lọc thuộc tính và tích hợp Redis Caching; Frontend hoàn thiện màn hình danh sách và chi tiết sản phẩm.
* **Kết quả bàn giao (Milestone T2):**
  - Product Service hỗ trợ lọc phân trang theo danh mục (Laptops, Audio, Accessories), khoảng giá, thương hiệu, trạng thái còn hàng.
  - Áp dụng Redis Cache tăng tốc truy vấn danh mục sản phẩm (Cache-Aside pattern, TTL hợp lý).
  - Khởi tạo bộ dữ liệu sản phẩm mẫu (Tech Gear Seed Data) phục vụ demo và kiểm thử.
  - Giao diện người dùng hiển thị danh sách sản phẩm thực tế, thanh tìm kiếm, bộ lọc trực quan và trang chi tiết sản phẩm kết nối qua API Gateway.

#### Tuần 3: Tích hợp Đơn hàng & Giỏ hàng (Order Service), Tích hợp Cổng Thanh toán MoMo Sandbox & Webhook IPN
* **Mục tiêu Sprint:** Hoàn thiện dịch vụ Giỏ hàng & Đơn hàng; tích hợp cổng thanh toán MoMo Sandbox vào Payment Service; đóng gói luồng Checkout Saga (Hold tồn kho -> Tạo đơn -> Thanh toán MoMo / COD).
* **Kết quả bàn giao (Milestone T3):**
  - Order Service cung cấp API quản lý giỏ hàng (`/cart`) và tạo đơn hàng với cơ chế giữ tồn kho tạm thời (Stock Hold) tại Product Service.
  - Payment Service tích hợp thành công MoMo Sandbox v3 API (`captureWallet`), sinh mã QR và `payUrl` chuyển hướng người dùng.
  - Hiện thực Webhook IPN bảo mật: Xác thực chữ ký HMAC-SHA256, kiểm tra đối soát số tiền và cập nhật trạng thái đơn hàng bất đồng bộ.
  - Giao diện Checkout tích hợp lựa chọn phương thức thanh toán (MoMo QR / COD) và trang Callback `/order/result` xử lý trạng thái giao dịch.

#### Tuần 4: Full Observability, Hoàn thiện Quản trị (Admin), Kiểm thử E2E Toàn diện & Đóng gói Tài liệu
* **Mục tiêu Sprint:** Kích hoạt hệ thống giám sát và khả năng quan sát (Observability: Actuator, Metrics, Distributed Tracing); hoàn thiện trang quản trị Admin; kiểm thử toàn diện kịch bản mua sắm; hoàn tất tài liệu đồ án.
* **Kết quả bàn giao (Milestone T4):**
  - Kích hoạt Spring Boot Actuator, Micrometer Prometheus metrics và cấu hình Trace ID / Span ID xuyên suốt các Microservices.
  - Frontend hoàn thiện bảng điều khiển Admin Dashboard: Quản lý trạng thái đơn hàng, thống kê doanh thu và quản lý danh mục hàng hóa.
  - Báo cáo kiểm thử tích hợp End-to-End (E2E Testing) không phát sinh lỗi race condition hay bất đồng bộ tồn kho.
  - Đóng gói trọn bộ tài liệu kỹ thuật: Sơ đồ kiến trúc, API Documentation v1, Hướng dẫn triển khai Docker và Slide thuyết trình demo.

---

### 4. BẢNG PHÂN CÔNG CÔNG VIỆC CHI TIẾT (WBS MATRIX)

| Mã Task | Hạng mục công việc | Phân hệ | Người phụ trách | Tuần | Sản phẩm bàn giao |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **INF-01** | Cấu hình `docker-compose.yml` (4 Postgres DB độc lập, Redis 7, Healthchecks, RAM limits) | Infra / DevOps | Đàm Trung Anh | T1 | Cụm DB & Cache chạy đồng bộ bằng 1 lệnh `docker compose up -d` |
| **SYS-01** | Thiết lập Spring Cloud Gateway: Cấu hình Routing, CORS tập trung và Global Filters | API Gateway | Lê Việt Anh | T1 | Gateway reverse-proxy định tuyến thông suốt `/api/v1/*` |
| **BE-01** | Xây dựng User Service: Spring Security 6, JWT Login/Register, Role RBAC, Mã hóa BCrypt | User Service | Đàm Trung Anh | T1 | API `/api/v1/auth/register`, `/login`, `/me` cấp JWT chuẩn |
| **FE-01** | Khởi tạo dự án Next.js 16, tích hợp Tailwind CSS 4, Swiss Minimalism Design Tokens, Header/Footer | Frontend | Phạm Công Anh | T1 | Bộ khung UI chuẩn Responsive, cấu trúc App Router sạch |
| **BE-02** | Xây dựng Product Service: CRUD sản phẩm/danh mục, bộ lọc đa thuộc tính, phân trang | Product Service | Đàm Trung Anh | T2 | API RESTful `/api/v1/products`, `/categories` hoàn chỉnh |
| **BE-03** | Tích hợp Redis Caching cho Product Service (Cache-Aside, cấu hình TTL & Cache Invalidation) | Product Service | Đàm Trung Anh | T2 | Tối ưu thời gian phản hồi truy vấn sản phẩm < 50ms |
| **DAT-01** | Xây dựng dữ liệu mẫu Tech Gear (Laptops, Peripherals, Audio, Specs, hình ảnh, tồn kho) | Data / Backend | Lê Việt Anh | T2 | Bộ dữ liệu seed data chất lượng cao và script tự động import |
| **FE-02** | Xây dựng trang Danh mục sản phẩm, bộ lọc giá/hãng, trang Chi tiết sản phẩm & Thông số kỹ thuật | Frontend | Phạm Công Anh | T2 | Giao diện Catalog & Product Detail tương tác mượt mà |
| **BE-04** | Xây dựng Order Service: Quản lý giỏ hàng (`/cart`), tạo đơn hàng `PENDING`, hỗ trợ Idempotency Key | Order Service | Đàm Trung Anh | T3 | API Giỏ hàng và luồng khởi tạo đơn hàng an toàn |
| **BE-05** | Thiết lập cơ chế Two-Phase Inventory: API Stock Hold, Stock Release khi hủy, Stock Deduct | Product Service | Đàm Trung Anh | T3 | Xử lý giữ và trừ tồn kho chính xác, tránh bán vượt số lượng |
| **PAY-01** | Tích hợp MoMo Sandbox v3 API (`captureWallet`): Tạo liên kết thanh toán, sinh mã QR, Deeplink | Payment Service | Lê Việt Anh | T3 | API `/api/v1/payments` kết nối thành công MoMo Gateway |
| **PAY-02** | Xây dựng Server-to-Server IPN Webhook: Xác thực chữ ký HMAC-SHA256, đối soát số tiền | Payment Service | Lê Việt Anh | T3 | Endpoint `/momo/ipn` bảo mật zero-trust, chống giả mạo giao dịch |
| **FE-03** | Xây dựng luồng Giỏ hàng (Cart Sheet), Trang Đặt hàng (Checkout), chọn MoMo/COD | Frontend | Phạm Công Anh | T3 | Luồng thanh toán hoàn chỉnh kết nối backend qua Gateway |
| **FE-04** | Xây dựng trang MoMo Callback & Return URL (`/order/result`), kiểm tra trạng thái đơn tự động | Frontend | Phạm Công Anh | T3 | Trải nghiệm thanh toán liền mạch và phản hồi trạng thái tức thì |
| **OBS-01** | Cấu hình Observability: Kích hoạt Actuator endpoints, Prometheus Metrics, correlation tracing | Toàn hệ thống | Lê Việt Anh | T4 | Hệ thống có khả năng quan sát trạng thái, metrics và trace log |
| **FE-05** | Xây dựng giao diện Quản trị viên (Admin Dashboard): Quản lý đơn hàng, đổi trạng thái, biểu đồ | Frontend | Phạm Công Anh | T4 | Màn hình Admin quản lý doanh thu và trạng thái đơn hàng |
| **SYS-02** | Kiểm thử liên thông toàn hệ thống (End-to-End Integration Testing) và tối ưu hóa hiệu năng | QA / All | Cả 3 thành viên | T4 | Báo cáo kịch bản kiểm thử E2E đạt chuẩn, không lỗi nghẽn |
| **DOC-01** | Hoàn thiện tài liệu kiến trúc, API Documentation v1, Hướng dẫn cài đặt & Slide thuyết trình | Documentation | Lê Việt Anh | T4 | Bộ tài liệu đồ án kỹ thuật và slide bảo vệ đồ án chuyên nghiệp |

---

### 5. NGUYÊN TẮC PHỐI HỢP & QUẢN TRỊ RỦI RO

#### 5.1. Quy chế làm việc nhóm
* **Họp Standup định kỳ:** Duy trì trao đổi ngắn 15 phút mỗi tối qua Discord/Google Meet nhằm đồng bộ tiến độ các task trong ngày, nhận diện sớm các vướng mắc (blockers) để giải quyết ngay.
* **Nguyên tắc Contract-First:** Thống nhất định dạng JSON Request/Response (API Contracts) giữa Gateway, Backend và Frontend từ Tuần 1. Nhờ đó, Frontend có thể chủ động mock API và phát triển độc lập mà không bị phụ thuộc vào tiến độ triển khai backend.
* **Quy chuẩn Git Flow & Code Review:**
  - Nhánh `main`: Chỉ chứa phiên bản mã nguồn ổn định, đã kiểm thử tích hợp thành công.
  - Nhánh `develop`: Môi trường tích hợp mã nguồn chung.
  - Các nhánh tính năng: Đặt tên theo chuẩn `feature/ma-task` (ví dụ: `feature/PAY-01-momo-sandbox`). Mọi Pull Request trước khi hợp nhất vào `develop` bắt buộc phải có ít nhất 1 thành viên review và phê duyệt.

#### 5.2. Kế hoạch quản trị rủi ro kỹ thuật
* **Rủi ro máy cá nhân quá tải khi chạy 5 Microservices + 4 PostgreSQL DBs + Redis:**
  - *Giải pháp:* Cấu hình giới hạn bộ nhớ chặt chẽ trong `docker-compose.yml` (`limits: memory: 512M`, `reservations: memory: 256M`). Trong giai đoạn lập trình cục bộ, các thành viên có thể chỉ chạy các container hạ tầng (Databases, Redis) bằng Docker, còn service nghiệp vụ đang trực tiếp code thì chạy nhẹ nhàng bằng IDE với cấu hình JVM `-Xms256m -Xmx512m`.
* **Rủi ro sai lệch dữ liệu tồn kho (Race Condition & Over-selling):**
  - *Giải pháp:* Áp dụng mô hình giữ hàng hai giai đoạn (Two-Phase Stock Reservation): Khi khách bắt đầu Checkout, gọi `POST /products/hold` để tạm khóa số lượng tồn. Nếu thanh toán thành công (qua MoMo IPN hoặc COD xác nhận), đơn hàng chuyển sang `PAID` và tồn kho chính thức trừ (`deduct`). Nếu đơn bị hủy hoặc quá thời gian chờ (timeout), kho sẽ tự động hoàn trả (`release`).
* **Rủi ro gian lận hoặc thất lạc thông điệp Webhook thanh toán (MoMo IPN):**
  - *Giải pháp:* Tuân thủ nguyên tắc bảo mật Zero-Trust. Phía backend luôn tính toán lại chữ ký HMAC-SHA256 từ raw payload nhận được và so sánh với signature do MoMo gửi đến bằng thuật toán so sánh an toàn; kiểm tra chéo số tiền (`amount`) khớp với giá trị đơn hàng trong `order_db`; áp dụng cơ chế xử lý Idempotent để đảm bảo nhận trùng Webhook IPN nhiều lần không làm phát sinh lỗi dữ liệu.
* **Rủi ro Frontend không nhận được trạng thái tức thời do độ trễ IPN:**
  - *Giải pháp:* Tại trang Return URL (`/order/result`), Frontend không phụ thuộc vào các tham số URL truyền về mà luôn chủ động gọi API Backend có thẩm quyền để kiểm tra trạng thái đơn hàng định kỳ ngắn (polling với backoff 2s - 5s) trong khi chờ webhook IPN xử lý hoàn tất.
