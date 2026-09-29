# PHASE 6 – LẤY DANH SÁCH EMAIL (TRUY VẤN VÀ GỬI DANH SÁCH TẬP TIN DỮ LIỆU)

## 1. Mục tiêu
- Sau khi Đăng nhập thành công (`LOGIN <username>`), Server tự động duyệt qua toàn bộ thư mục cá nhân `mail_data/<username>/`.
- Server lấy ra danh sách tên tất cả các tập tin email hiện có và gửi về lại Client hiển thị.

## 2. Chức năng đạt được
- Client gửi `LOGIN king`.
- Server kiểm tra thư mục `mail_data/king`:
  - Dùng `listFiles()` lấy danh sách file.
  - Kiểm tra `file.isFile()` và lấy `file.getName()`.
  - Gom chuỗi danh sách email bằng `StringBuilder`.
- Client nhận kết quả và in ra danh sách thư dạng:
  ```text
  LOGIN SUCCESS
  Danh sach email cua king:
  1. new_email.txt
  2. mail_001.txt
  3. mail_002.txt
  ```

## 3. Kiến thức đã học
- **`listFiles()`**: Đọc danh sách tập tin/thư mục con dưới dạng mảng `File[]`.
- **`isFile()` & `getName()`**: Lọc chỉ lấy tập tin và trích xuất tên file ngắn gọn thay vì đường dẫn tuyệt đối.
- **`StringBuilder` Aggregation**: Gom toàn bộ thông điệp nhiều dòng thành 1 chuỗi UTF duy nhất để tối ưu băng thông Socket.

## 4. Code & Giải thích chi tiết

### Server Code: `server/MailServer.java`
```java
File userFolder = new File(MAIL_DATA_DIR, username);
if (!userFolder.exists() || !userFolder.isDirectory()) {
    return "LOGIN_FAILED - Tai khoan khong ton tai!";
}
File[] files = userFolder.listFiles();
StringBuilder sb = new StringBuilder("LOGIN SUCCESS\nDanh sach email cua " + username + ":\n");
int count = 1;
for (File file : files) {
    if (file.isFile()) {
        sb.append(count++).append(". ").append(file.getName()).append("\n");
    }
}
return sb.toString().trim();
```

### Client Code: `client/MailClient.java`
```java
dos.writeUTF("LOGIN " + username);
dos.flush();
String response = dis.readUTF();
System.out.println(response);
```

## 5. Lỗi phổ biến & Debug
- **Lỗi `NullPointerException`**: Không kiểm tra `files == null` khi thư mục rỗng hoặc không có quyền truy cập.
- **Lộ đường dẫn máy Server**: Dùng `getPath()` thay vì `getName()` khiến chuỗi gửi về Client chứa đường dẫn đĩa cứng tuyệt đối.
