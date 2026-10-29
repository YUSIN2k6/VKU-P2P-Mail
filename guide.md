# HƯỚNG DẪN SỬ DỤNG HỆ THỐNG VKU MAIL APP
*(Giao thức TCP Socket & SMTP/POP3 Jakarta Mail - Mô hình MVC)*

---

## I. CÁCH KHỞI ĐỘNG HỆ THỐNG

Bạn có thể chạy hệ thống bằng 1 trong 2 cách sau:

### Cách 1: Chạy bằng file kịch bản (Khuyên dùng trên Windows)
1. **Khởi động Server:** Nhấp đúp chuột vào file `run_server.bat` (Cửa sổ TCP Mail Server sẽ hiện lên và tự động chạy).
2. **Khởi động Client:** Nhấp đúp chuột vào file `run_client.bat` (Mở cửa sổ Đăng nhập). Có thể chạy nhiều lần file này để mở nhiều Client gửi thư qua lại.

### Cách 2: Chạy trực tiếp từ VS Code / IDE
* **Chạy Server:** Mở file `com.vku.app.ServerApp.java` $\rightarrow$ Nhấn nút **Run** (hoặc `F5`).
* **Chạy Client:** Mở file `com.vku.app.ClientApp.java` $\rightarrow$ Nhấn nút **Run**.

---

## II. CHI TIẾT CÁC GIAO DIỆN VÀ CHỨC NĂNG CÁC NÚT BẤM

---

### 1. Giao diện Máy chủ (VKU TCP Mail Server)
Cửa sổ điều khiển Server trung tâm phục vụ việc xác thực tài khoản và gửi/nhận thư kèm tệp tin nội bộ qua TCP Socket.

* **Ô nhập `Cổng (Port TCP)`:** Mặc định là cổng `8888`.
* **Nút `[Khởi động Server]`:** 
  * Kích hoạt ServerSocket TCP lắng nghe các kết nối từ các máy khách (Client). 
  * *(Hệ thống được thiết lập tự động bấm nút này ngay khi mở giao diện)*.
* **Nút `[Dừng Server]`:** Tạm dừng máy chủ và đóng toàn bộ kết nối của các client đang online.
* **Khung Nhật ký (Log Area):** Hiển thị chi tiết theo thời gian thực: client nào vừa kết nối, đăng ký, đăng nhập, gửi thư cho ai, số lượng file đính kèm gửi qua mạng TCP.

---

### 2. Giao diện Đăng nhập (AuthGUI)
Màn hình đầu tiên xuất hiện khi bạn mở ứng dụng Client (`ClientApp`).

* **Các ô nhập dữ liệu:**
  * **`Địa chỉ Email:`** Nhập email bạn đã đăng ký (Ví dụ: `tuanbmt3355@gmail.com`).
  * **`Mật khẩu:`** Nhập mật khẩu tài khoản của bạn (Hiển thị văn bản bình thường để dễ kiểm tra, không bị ẩn).
* **Các nút bấm:**
  * **Nút `[Đăng Nhập]`:** 
    * Kiểm tra thông tin tài khoản qua TCP Server (hoặc kho lưu trữ cục bộ).
    * Nếu chính xác: Đóng màn hình đăng nhập và chuyển sang giao diện chính (`MailClientGUI`).
    * Nếu sai email hoặc mật khẩu: Hiện hộp thoại cảnh báo lỗi.
  * **Nút `[Tạo tài khoản mới]`:** Tạm ẩn màn hình đăng nhập và mở màn hình Đăng ký tài khoản (`RegisterGUI`).

---

### 3. Giao diện Đăng ký tài khoản (RegisterGUI)
Dùng để tạo mới một tài khoản người dùng trên hệ thống.

* **Các ô nhập dữ liệu:**
  * **`Tên đăng nhập:`** Nhập tên hiển thị của bạn (Ví dụ: `Nguyễn Hiển Tuấn`).
  * **`Địa chỉ Email:`** Nhập email của bạn (Ví dụ: `tuanbmt3355@gmail.com`).
  * **`Mật khẩu:`** Nhập mật khẩu bạn muốn đặt cho tài khoản.
* **Các nút bấm:**
  * **Nút `[Đăng Ký Ngay]`:** 
    * Kiểm tra định dạng email và tính duy nhất.
    * Tạo tài khoản trên Server và tự động tạo thư mục hòm thư cá nhân theo tiền tố email: `storage/<tên_trước_ký_tự_@>/` (Ví dụ: `storage/tuanbmt3355/`).
    * Tạo sẵn file `account.txt` lưu thông tin người dùng.
    * Thông báo thành công và tự động quay về màn hình Đăng nhập.
  * **Nút `[Quay lại Đăng nhập]`:** Hủy thao tác đăng ký và quay về màn hình Đăng nhập trước đó.

---

### 4. Giao diện Hộp thư & Soạn gửi thư (MailClientGUI)
Giao diện làm việc chính được thiết kế hiện đại theo **bố cục 2 cột Trái - Phải**:

#### A. Cột bên Trái: Khu vực Soạn thư & Gửi File
* **Ô nhập `Gửi tới (Email):`** Điền email người nhận.
* **Ô nhập `Tiêu đề:`** Nhập tiêu đề bức thư.
* **Ô hiển thị `Đính kèm:`** Danh sách tên file đã chọn.
* **Tùy chọn `[x] Gửi trực tiếp bằng SMTP`**: Tích để gửi ra Gmail Internet hoặc bỏ tích để gửi nội bộ tốc độ cao qua TCP Socket.
* **Khung soạn thảo nội dung:** Viết nội dung thư.
* **Hàng nút bên dưới:**
  * **`[Gửi Email & File]`**: Thực hiện gửi thư và đẩy một thẻ thông tin sang khung bên phải.
  * **`[Đính kèm File/Ảnh]`**: Chọn file hoặc hình ảnh từ máy.
  * **`[Mở Storage]`**: Mở thư mục lưu trữ cá nhân.

#### B. Cột bên Phải: Khung hiển thị các Thẻ thư (Mail Cards)
* Hiển thị danh sách các thư dưới dạng **các thẻ (Card)** riêng biệt, trực quan:
  * **Thẻ thư đã gửi (`[THƯ BẠN ĐÃ GỬI]`):** Nền xanh nhạt, hiển thị đầy đủ thời gian gửi, người nhận, tiêu đề, nội dung và các tệp đính kèm.
  * **Thẻ thư nhận được (`[THƯ NHẬN ĐƯỢC]`):** Nền trắng/xám, xuất hiện tự động theo thời gian thực khi có người gửi thư/tệp tin đến.
  * **Nút `[Tải xuống]` tại từng tệp đính kèm:** Khi thư có file/ảnh, một nút **"Tải xuống"** sẽ hiển thị ngay cạnh tên file. Bấm vào nút này, file sẽ tự động được sao chép thẳng vào thư mục `Downloads` mặc định trong ổ C (`C:\Users\<User>\Downloads`) và hiển thị tùy chọn mở thư mục Downloads ngay lập tức.
  * **Nút `[Tải thư POP3 (Gmail)]`:** Tải danh sách thư POP3 và đưa vào danh sách thẻ thư bên phải.

---

## III. CẤU TRÚC LƯU TRỮ MỚI TRONG THƯ MỤC `storage`

Hệ thống lưu trữ đã được cấu trúc lại như sau:

```text
storage/
├── tuanbmt3355.txt        <-- File txt lưu chung thông tin tài khoản và toàn bộ lịch sử thư gửi/nhận
├── tuannh.24itb.txt       <-- File của tài khoản khác
├── image/                 <-- Thư mục chứa hình ảnh/tệp tin đính kèm từ các lần gửi
└── file_temp/             <-- Thư mục chứa các tệp tin/hình ảnh mà client nhận được từ các máy khác
```

### 1. Nội dung file `storage/<tên_tài_khoản>.txt`:
Lưu trữ toàn bộ thông tin đăng ký và lịch sử thư trong cùng 1 file duy nhất:
```text
=== THÔNG TIN TÀI KHOẢN ===
Username: Nguyễn Hiển Tuấn
Email: tuanbmt3355@gmail.com
Password: Hihihaha123!
===========================

--- THƯ LỊCH SỬ ---
Sending time: 2026-10-29 14:30:15
Sender: tuanbmt3355@gmail.com
Receiver: tuannh.24itb@vku.udn.vn
Title: Xin chào bạn
Content:
Mình gửi file ảnh này cho bạn xem thử nhé!
File: anh_demo.png
-------------------
```

### 2. Thư mục `file_temp/` và Nút Tải xuống:
* Khi một máy client nhận được file/hình ảnh từ máy khác gửi qua TCP, file đó sẽ được tải trực tiếp vào `storage/file_temp/` với đúng tên gốc.
* Thẻ thư trên giao diện hiển thị tên file được lấy từ thư mục `file_temp/`.
* Khi người dùng nhấn nút **`[Tải xuống]`**, tệp tin sẽ được sao chép thẳng vào thư mục `Downloads` của máy tính (`C:\Users\<User>\Downloads`) để sử dụng bình thường.

