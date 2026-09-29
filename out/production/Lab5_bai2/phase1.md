# PHASE 1 – TCP CONNECTION CƠ BẢN

## 1. Mục tiêu
- Xây dựng kết nối TCP thành công giữa MailClient và MailServer qua IP `localhost` và Port `2345`.
- Nắm vững cách thức hoạt động của Socket ở phía Server và phía Client.

## 2. Chức năng đạt được
- Server mở cổng 2345 và lắng nghe kết nối.
- Client kết nối tới Server, cả 2 bên hiển thị IP và Port của nhau.
- Tự động đóng Socket sau khi hoàn tất.

## 3. Kiến thức đã học
- **Client - Server Model**: Server lắng nghe, Client khởi tạo kết nối.
- **TCP Protocol**: Giao thức hướng kết nối, đảm bảo dữ liệu toàn vẹn qua Bắt tay 3 bước (3-Way Handshake).
- **IP & Localhost**: `127.0.0.1` trỏ về chính máy hiện tại.
- **Port**: Cổng tĩnh Server (2345) và Cổng động Client (Ephemeral Port).
- **ServerSocket**: Cổng lắng nghe (bảo vệ / lễ tân).
- **Socket**: Kênh giao tiếp 1-1 riêng biệt.
- **accept()**: Hàm đồng bộ chặn (Blocking call) chờ Client.

## 4. Code & Giải thích chi tiết

### Server Code: `server/MailServer.java`
```java
ServerSocket serverSocket = new ServerSocket(2345);
Socket clientSocket = serverSocket.accept();
clientSocket.close();
```
- `ServerSocket(2345)`: Đăng ký mở port 2345 với OS để chờ kết nối.
- `accept()`: Tạm dừng chương trình chờ Client. Khi Client kết nối, hoàn tất 3-way handshake và trả về `clientSocket`.
- `clientSocket.close()`: Đóng socket giải phóng tài nguyên.

### Client Code: `client/MailClient.java`
```java
Socket socket = new Socket("localhost", 2345);
```
- `new Socket(...)`: Xin OS cấp port ngẫu nhiên, chủ động gửi gói SYN tới Server port 2345 để bắt tay 3 bước.

## 5. Lỗi phổ biến & Debug
- `ConnectException: Connection refused`: Do Client chạy trước Server hoặc sai Port.
- `BindException: Address already in use`: Do Server đang chạy trùng port 2345.
