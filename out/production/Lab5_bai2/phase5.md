# PHASE 5 – LOGIN (ĐĂNG NHẬP DỰA TRÊN HỆ THỐNG THƯ MỤC DỮ LIỆU)

## 1. Mục tiêu
- Xây dựng cơ chế Đăng nhập (Authentication) cho Client.
- Server xác thực tài khoản dựa trên việc kiểm tra sự tồn tại của thư mục cá nhân `mail_data/<username>` trên ổ đĩa Server.

## 2. Chức năng đạt được
- Client gửi `LOGIN <username>`.
- Server kiểm tra:
  - Nếu `username` trống -> Trả về `LOGIN_FAILED - Username dang nhap khong duoc de rong!`.
  - Nếu thư mục `mail_data/<username>` tồn tại & là directory -> Trả về `LOGIN_SUCCESS - Dang nhap thanh cong...`.
  - Nếu thư mục không tồn tại -> Trả về `LOGIN_FAILED - Tai khoan khong ton tai...`.

## 3. Kiến thức đã học
- **Storage-based Authentication**: Đăng nhập tối giản không cần CSDL hay mật khẩu, kiểm tra sự tồn tại của đường dẫn dữ liệu.
- **Type Safety Check**: Kết hợp `File.exists()` và `File.isDirectory()` để đảm bảo đường dẫn là thư mục tài khoản hợp lệ.
- **Phân biệt thao tác đĩa**: `CREATE_USER` thực hiện thao tác ghi (`mkdirs()`), trong khi `LOGIN` chỉ thực hiện thao tác đọc/kiểm tra.

## 4. Code & Giải thích chi tiết

### Server Code: `server/MailServer.java`
```java
File userFolder = new File("mail_data", username);
if (userFolder.exists() && userFolder.isDirectory()) {
    return "LOGIN_SUCCESS - Dang nhap thanh cong voi tai khoan [" + username + "]!";
} else {
    return "LOGIN_FAILED - Tai khoan [" + username + "] khong ton tai tren Server!";
}
```

### Client Code: `client/MailClient.java`
```java
dos.writeUTF("LOGIN " + username);
dos.flush();
String response = dis.readUTF();
```

## 5. Lỗi phổ biến & Debug
- **Tự động tạo folder khi Login**: Gọi nhầm `mkdirs()` trong luồng `LOGIN` khiến tài khoản chưa tạo cũng báo thành công.
- **Khoảng trắng dư thừa**: Không dùng `.trim()` khiến `"king "` không tìm thấy folder `king`.
