# BÁO CÁO TÍCH HỢP DỮ LIỆU LAPTOP TỪ GEARVN VÀO TECHGEAR

**Thời điểm thực hiện:** 2026-10-10  
**Môi trường thực thi:** Local Development (PostgreSQL Container `product-db`, database `product_db`, Product Service cổng `8082`, API Gateway cổng `8080`)  
**Tập tin nguồn:** `D:\CODE\WebJava\techgear-crawler\gearvn_laptops_import_ready.csv`  

---

## 1. Tổng quan số liệu thực tế

| Tiêu chí | Số lượng | Tỷ lệ (%) | Ghi chú |
|---|:---:|:---:|---|
| **Tổng số dòng sản phẩm trong CSV** | **197** | 100% | Đã rà soát không có dòng rỗng |
| **Sản phẩm đã tồn tại trước khi chạy** | **5** | 2.5% | SKU `TG-GVN-0001` .. `TG-GVN-0005` (ID 36–40) |
| **Sản phẩm mới import thành công** | **180** | 91.4% | SKU `TG-GVN-0006` .. `TG-GVN-0197` (ID 41–220) |
| **Sản phẩm bị giữ lại (Hold)** | **12** | 6.1% | Giá $\ge 100,000,000$ VND (Vượt trần `numeric(10,2)`) |
| **Sản phẩm lỗi trong quá trình import** | **0** | 0.0% | 100% request đạt HTTP 201 Created |
| **Tổng số SKU TG-GVN hiện diện trong DB** | **185** | 93.9% | Tính Idempotency được bảo toàn tuyệt đối |
| **Tổng số sản phẩm trong Danh mục Laptop (ID 1)** | **189** | - | 185 sản phẩm GearVN + 4 sản phẩm mẫu trước đó |

---

## 2. Kết quả kiểm tra đối chiếu (Verification via API & Database)

### 2.1. Kiểm tra toàn vẹn Database
- Truy vấn PostgreSQL container `product-db`:
  ```sql
  SELECT count(*) FROM products WHERE sku LIKE 'TG-GVN-%';
  -- Kết quả: 185
  SELECT count(*) FROM products WHERE category_id = 1 AND active = true;
  -- Kết quả: 189
  ```
- Kiểm tra tính duy nhất (Unique SKU): Không phát hiện bất kỳ trùng lặp SKU nào (`idx_products_sku` bảo toàn nguyên vẹn).

### 2.2. Kiểm tra định tuyến qua API Gateway (`http://localhost:8080`)
- Endpoint: `GET /api/v1/products/category/1?page=0&size=10`
  - `totalElements`: **189**
  - `totalPages`: 19 (mỗi trang 10 sản phẩm)
- Dữ liệu trả về chuẩn hóa:
  - `name`: Đã làm sạch ký tự lạ, loại bỏ filter tags lặp lại ở đuôi, bảo toàn 100% Model, CPU, GPU, RAM, SSD, Kích thước màn hình.
  - `price`: Số thực `BigDecimal`, định dạng hiển thị frontend chuẩn tiền tệ Việt Nam (`... ₫`).
  - `category.id`: `1` (Laptop & Máy tính xách tay).
  - `imageUrl`: Giữ nguyên link ảnh trực tiếp từ CDN `cdn.hstatic.net`.
  - `description`: Lưu thông tin nguồn gốc để đối chiếu: `(GearVN) Nguon: <product_url>`.
  - `active`: `true`.

---

## 3. Danh sách 12 sản phẩm giá cao ($\ge 100$ triệu VND) được giữ lại an toàn

Theo yêu cầu an toàn hệ thống, 12 sản phẩm này được tách riêng và dừng ghi để tránh lỗi tràn số (`numeric field overflow` và Bean Validation error):

| Dòng CSV | Mã SKU dự kiến | Giá nguồn (VND) | Tên sản phẩm nguồn |
|:---:|:---:|:---:|---|
| **31** | `TG-GVN-0031` | 179,990,000 | Laptop gaming Acer Predator Helios 18 AI PH18-I71-91UX (Ultra 9-290HX Plus/ RTX 5080 16GB/ 64GB/ 3TB/ 18" 2K+ Mini LED 250Hz/ Win 11) |
| **32** | `TG-GVN-0032` | 249,990,000 | Laptop gaming Acer Predator Helios 18 PH18-I71-975R (Ultra 9-290HX Plus/ RTX 5090 24GB/ 64GB/ 3TB/ 18" 4K+ Mini LED 120Hz/ Win 11) |
| **62** | `TG-GVN-0062` | 109,990,000 | Laptop gaming Acer Predator Helios Neo PHN16-I71-9779 (Ultra 9-290HX Plus/ RTX 5070 8GB/ 64GB/ 2TB/ 16" WQXGA IPS 180Hz/ Win 11) |
| **83** | `TG-GVN-0083` | 114,990,000 | Laptop gaming Acer Predator Helios Neo 16S PHN16S-71-95MS (Ultra 9-275HX/ RTX 5070 Ti 12GB/ 64GB/ 2TB/ 16.0" 2K+ OLED 240Hz/ Win 11) |
| **96** | `TG-GVN-0096` | 139,990,000 | Laptop gaming ASUS ROG Zephyrus GU606AW-TB052WS (Ultra 9/ RTX 5080/ 32GB/ 1TB/ 16" 2.5K OLED 240Hz/ Win 11) |
| **97** | `TG-GVN-0097` | 124,990,000 | Laptop gaming ASUS ROG Zephyrus GU606AW-TB121W (Ultra 9/ RTX 5080/ 32GB/ 1TB/ 16" 2.5K OLED 240Hz/ Win 11) |
| **98** | `TG-GVN-0098` | 169,990,000 | Laptop gaming ASUS ROG Zephyrus GU606AX-TB042WS (Ultra 9/ RTX 5090/ 64GB/ 2TB/ 16" 2.5K OLED 240Hz/ Win 11) |
| **101** | `TG-GVN-0101` | 119,990,000 | Laptop gaming ASUS ROG Zephyrus GU405AW-SY029W (Ultra 9/ RTX 5080/ 32GB/ 1TB/ 14" 3K OLED 120Hz/ Win 11) |
| **104** | `TG-GVN-0104` | 179,990,000 | Laptop gaming ASUS ROG Strix SCAR G835LXG-TQ473W (Ultra 9-290HX Plus/ RTX 5090/ 64GB/ 2TB/ 18" Mini LED 240Hz/ Win 11) |
| **161** | `TG-GVN-0161` | 104,990,000 | Laptop gaming Acer Predator Helios Neo PHN16-I71-97VK (Ultra 9-290HX Plus/ RTX 5080 16GB/ 32GB/ 512GB/ 16" WQAGA 240Hz/ Win 11) |
| **174** | `TG-GVN-0174` | 199,990,000 | Laptop gaming Asus ROG Zephyrus Duo 16 GX651AX-SR050WS (Ultra 9-386H/ 64GB/ 2TB/ 16" 3K OLED/ Win 11) |
| **182** | `TG-GVN-0182` | 115,990,000 | Laptop gaming Lenovo Legion Pro 7 16IAX10H 83F500JGVN (Ultra 9-275HX/ RTX 5070 Ti/ 32GB/ 1TB/ 16" OLED 240Hz/ Win 11) |

---

## 4. Kiểm chứng tính Idempotent (Chống trùng lặp tuyệt đối)
Đã thực thi chạy lại toàn bộ script import lần thứ 2:
- **Số sản phẩm tạo mới:** 0
- **Số sản phẩm bỏ qua (đã nhận diện qua HTTP 200 từ SKU API):** 185
- **Số sản phẩm giữ lại (giá cao):** 12
- **Số lỗi phát sinh:** 0
$\to$ Khẳng định cơ chế import là **Idempotent**, hoàn toàn an toàn khi re-run nhiều lần.

---

## 5. Đề xuất kỹ thuật & Bước tiếp theo để import 12 sản phẩm còn lại

1. **Thực thi Migration mở rộng cột giá:**
   - Database:
     ```sql
     ALTER TABLE products ALTER COLUMN price TYPE numeric(14,2);
     ```
   - Microservices liên quan:
     - `product-service`: Cập nhật `@Column(precision = 14, scale = 2)` trong [Product.java](file:///D:/CODE/WebJava/observable-microservices-ecommerce/product-service/src/main/java/com/ecommerce/productservice/model/Product.java) và `@Digits(integer = 12, fraction = 2)` trong [CreateProductRequest.java](file:///D:/CODE/WebJava/observable-microservices-ecommerce/product-service/src/main/java/com/ecommerce/productservice/dto/CreateProductRequest.java).
     - `order-service` & `payment-service`: Nâng `totalAmount`, `subtotal`, `amount` lên `numeric(14,2)` để đảm bảo khả năng xử lý đơn hàng giá trị cao.
2. **Import đợt cuối:**
   - Sau khi cập nhật schema, chạy import nốt 12 sản phẩm giá cao nói trên để đạt 197/197 sản phẩm (100% dữ liệu từ GearVN).
