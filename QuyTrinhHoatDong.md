# Quy trình hoạt động của hệ thống P2P Mail Client - Server

Dưới đây là chi tiết quy trình từ lúc kết nối đến các chức năng quan trọng (gửi, nhận thư và file) dựa trên mã nguồn của dự án. Hệ thống sử dụng giao thức UDP để giao tiếp.

## 1. Khởi tạo kết nối (Connection)

### Tại Client (Người dùng)
File: `MailClientGUI.java`
- Khi người dùng nhấn nút "Kết nối", hệ thống gọi hàm `connect()` (**Dòng 155 - 188**).
- Khởi tạo đối tượng `UDPConnection`.
- Mở một luồng (thread) mới để liên tục lắng nghe tin nhắn trả về từ Server: `new Thread(this::listenIncomingMessages).start();` (**Dòng 165**).
- Gửi lệnh đăng nhập lên Server với cú pháp `LOGIN::[Tên_Client]`: `connection.send("LOGIN::" + txtName.getText().trim(), serverAddress, 9876);` (**Dòng 166**).

### Tại Server (Hệ thống trung tâm)
File: `MailServerGUI.java`
- Server chạy vòng lặp vô hạn để nhận gói tin tại hàm `startServer()` (**Dòng 44 - 56**).
- Mỗi khi có gói tin tới, Server sẽ gọi hàm `handlePacket(DatagramPacket packet)` (**Dòng 58 - 164**) để phân loại xử lý.
- Khi nhận được lệnh `LOGIN` (**Dòng 70 - 90**), Server sẽ:
    - Lưu địa chỉ IP và Port của Client vào bộ nhớ `activeClients`.
    - Tạo thư mục hòm thư cá nhân trên Server.
    - Gửi phản hồi `SYS::Đăng nhập thành công` về lại cho Client.

---

## 2. Quá trình Gửi thư và File đính kèm

### Tại Client (Người gửi)
File: `MailClientGUI.java`
- Khi người dùng soạn xong và bấm "Gửi", hàm `sendMailWithAttachments()` sẽ được thực thi (**Dòng 284 - 329**).
- **Gửi nội dung chữ:** Client gói thông tin vào chuỗi có tiền tố `SEND::` và gửi đi (**Dòng 303 - 305**).
    - Đoạn code: `String mailReq = "SEND::" + txtName.getText().trim() + "::" + to + "::" + title + "::" + msg; connection.send(...)`
- **Gửi file đính kèm:** Tiếp theo, Client dùng vòng lặp duyệt qua các file. Chuyển đổi nội dung file thành chuỗi Base64 và gửi với tiền tố `FILE::` (**Dòng 308 - 318**).
    - Đoạn code: `String fileReq = "FILE::" + txtName.getText().trim() + "::" + to + "::" + file.getName() + "::" + base64Content;`

### Tại Server (Nhận và Chuyển tiếp)
File: `MailServerGUI.java`
- **Xử lý nội dung chữ (`SEND`):** (**Dòng 98 - 133**)
    - Lưu nội dung email thành dạng file `.txt` vào thư mục của người nhận trên Server.
    - Kiểm tra xem người nhận có đang Online không. Nếu có, Server đóng gói lại bằng tiền tố `MAIL::` và chuyển tiếp đến IP/Port của người nhận (**Dòng 129 - 133**).
- **Xử lý file đính kèm (`FILE`):** (**Dòng 136 - 160**)
    - Server giải mã Base64 thành byte và lưu file vật lý vào thư mục lưu trữ của người nhận trên Server.
    - Sau đó, nếu người nhận đang Online, Server dùng tiền tố `FILE_FWD::` để chuyển tiếp nội dung file trực tiếp tới người nhận (**Dòng 155 - 159**).

---

## 3. Quá trình Nhận thư và File đính kèm

### Tại Client (Người nhận)
File: `MailClientGUI.java`
- Client liên tục chạy hàm `listenIncomingMessages()` (**Dòng 190 - 224**) để trực chờ tin nhắn.
- Phân loại tiền tố gói tin tới để xử lý hiển thị:
    - **Nhận nội dung chữ:** Nếu gói tin bắt đầu bằng `MAIL::` (**Dòng 199 - 200**), Client sẽ trích xuất và hiển thị nội dung thư lên khu vực hộp thư (Inbox).
    - **Nhận file đính kèm:** Nếu gói tin bắt đầu bằng `FILE_FWD::` (**Dòng 201 - 217**), Client sẽ tách chuỗi Base64, giải mã lại thành file thực tế, và ghi vào thư mục `storage/[Tên_Client]` trên máy khách. Cuối cùng, thông báo cho người nhận biết có file vừa được lưu.
