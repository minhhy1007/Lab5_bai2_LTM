# TỔNG KẾT CHỨC NĂNG VÀ KIẾN THỨC BÀI TẬP MAIL SERVER JAVA TCP SOCKET

---

## 📌 I. TỔNG HỢP CÁC CHỨC NĂNG ĐÃ XÂY DỰNG

| STT | Chức năng | Cú pháp lệnh (Protocol) | Hành động phía Server | Kết quả đĩa cứng (`mail_data/`) |
| :---: | :--- | :--- | :--- | :--- |
| **1** | **Khởi tạo kết nối** | TCP Connection | `serverSocket.accept()` mở kênh lắng nghe port `2345`. | Không tác động đĩa |
| **2** | **Tạo tài khoản** | `CREATE_USER <username>` | Kiểm tra sự tồn tại của thư mục user. Tạo folder và file mặc định. | Tạo `mail_data/<username>/new_email.txt` |
| **3** | **Đăng nhập** | `LOGIN <username>` | Kiểm tra `userFolder.exists()` & `isDirectory()`. | Không tác động đĩa |
| **4** | **Gửi Email** | `SEND_MAIL` + `FROM` + `TO` + `BODY` | Kiểm tra người nhận, sinh tên file `mail_xxx.txt` ngẫu nhiên/nối tiếp không đè file cũ. | Ghi file `mail_data/<TO>/mail_001.txt` |
| **5** | **Xem danh sách email** | Trả về kèm `LOGIN` | Dùng `listFiles()` quét folder cá nhân, lấy `file.getName()`. | Đọc thông tin từ đĩa |

---

## 🌐 II. KIẾN THỨC MẠNG MÁY TÍNH & SOCKET DÃ ÁP DỤNG

### 1. Mô hình Kiến trúc Client - Server
- **Server**: Chạy thụ động, mở cổng cố định `2345` chờ kết nối từ các máy trạm.
- **Client**: Chủ động phát lệnh kết nối, truyền yêu cầu (Request) và nhận kết quả phản hồi (Response).

### 2. Giao thức TCP (Transmission Control Protocol)
- Là giao thức **hướng kết nối (Connection-oriented)**.
- Quá trình **Bắt tay 3 bước (3-Way Handshake)** diễn ra ngầm ở tầng OS khi Client gọi `new Socket("localhost", 2345)` và Server dừng ở `accept()`.
- Đảm bảo dữ liệu gửi đi không bị mất mát, trùng lặp hay sai thứ tự.

### 3. Địa chỉ IP & Cổng (Port)
- **IP Localhost (`127.0.0.1`)**: Định danh chính máy tính hiện tại.
- **Port tĩnh (`2345`)**: Dành riêng cho Server làm "địa chỉ nhà" để Client tìm tới.
- **Port động (Ephemeral Port)**: Do OS cấp phát ngẫu nhiên cho Client khi khởi tạo kết nối.

### 4. Lớp Socket trong Java Network API
- **`ServerSocket`**: Đóng vai trò "Lễ tân", chuyên đứng lắng nghe và chấp nhận kết nối qua `accept()`.
- **`Socket`**: Đóng vai trò "Nhân viên phục vụ 1-1", đại diện cho kênh giao tiếp riêng biệt giữa 2 máy.

### 5. Giao thức Tầng Ứng Dụng (Application Layer Protocol)
- Phân biệt rõ: **TCP chỉ truyền mảng Byte thô, TCP không hiểu `CREATE_USER` hay `LOGIN`**.
- `CREATE_USER`, `SEND_MAIL`, `LOGIN`, `OK`, `LOGIN_SUCCESS` là **chuẩn quy ước riêng của ứng dụng** do lập trình viên thiết kế để phân loại luồng xử lý.

---

## 📁 III. KIẾN THỨC LẬP TRÌNH JAVA & I/O STREAM DÃ ÁP DỤNG

### 1. Java Socket Stream I/O
- **`DataInputStream` & `DataOutputStream`**: Bộ bọc stream (Decorator Pattern) giúp truyền/nhận trực tiếp chuỗi chữ UTF-8 (`writeUTF()` / `readUTF()`) thay vì byte thô.
- **Tầm quan trọng của `flush()`**: Ép đẩy dữ liệu từ bộ nhớ đệm (Buffer) ra card mạng lập tức, tránh treo luồng.
- **Tính đồng bộ (Blocking Call)**: `readUTF()` sẽ dừng chương trình chờ cho tới khi phía bên kia `writeUTF()` thành công.

### 2. Quản lý Tập tin & Thư mục (Java File System)
- **`java.io.File`**: Quản lý đường dẫn tập tin và thư mục.
- **`exists()` & `isDirectory()`**: Xác thực sự tồn tại và loại đối tượng đĩa trước khi thao tác.
- **`mkdirs()`**: Tạo thư mục cá nhân cho user (tự động tạo toàn bộ cây thư mục cha nếu chưa có).
- **`createNewFile()`**: Khởi tạo tập tin văn bản `.txt` mới trên đĩa.
- **`listFiles()`**: Đọc mảng `File[]` đại diện cho các lá thư có trong hòm thư cá nhân.

### 3. Ghi văn bản có bộ đệm (Text Buffer Writing)
- **`BufferedWriter` & `FileWriter`**: Ghi Header (`FROM`, `TO`) và nội dung thư (`BODY`) xuống tập tin đĩa cứng một cách tối ưu hiệu năng.

### 4. Thuật toán Xử lý Chuỗi & Tên File
- **`request.split(" ", 2)`**: Tách mã lệnh và tham số đi kèm.
- **`String.format("mail_%03d.txt", index)`**: Thuật toán sinh tên file động (`mail_001.txt`, `mail_002.txt`...) để tránh ghi đè các email cũ.
- **`StringBuilder`**: Gom toàn bộ danh sách tên email thành 1 thông điệp duy nhất để gửi qua Socket.

---

## 🎯 IV. TƯ DUY VÀ KỸ NĂNG CỐT LÕI ĐẠT ĐƯỢC

1. **Phân định 2 tầng độc lập**:
   - **Tầng Mạng (Socket Stream)**: Vận chuyển thông điệp tạm thời giữa 2 máy.
   - **Tầng Lưu Trữ (File System)**: Lưu dữ liệu bền vững vĩnh viễn trên đĩa cứng Server.
2. **Quy trình Debug 6 câu hỏi chuẩn khoa học**: Xác định lỗi ở đâu (Client/Server), giai đoạn nào (Connect/Request/Process/Response), từ đó khoanh vùng xử lý thay vì sửa code ngẫu nhiên.
3. **Kỹ năng Giải trình Code (Vấn đáp)**: Trả lời tự tin 5 câu hỏi vàng cho mọi dòng code: *Nó làm gì? Tại sao cần? Dữ liệu đi đâu? Nếu bỏ đoạn này thì sao? Liên quan kiến thức mạng nào?*
