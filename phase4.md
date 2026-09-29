# PHASE 4 – SEND EMAIL (GỬI EMAIL VÀ TỰ ĐỘNG LƯU TRỮ VÀO FOLDER NGƯỜI NHẬN)

## 1. Mục tiêu
- Xây dựng chức năng gửi email giữa các người dùng trong hệ thống.
- Tự động kiểm tra người nhận và khởi tạo file thư (`mail_001.txt`, `mail_002.txt`...) lưu vào thư mục người nhận trên Server.

## 2. Chức năng đạt được
- Client chọn menu gửi mail, truyền 3 thông tin: `FROM`, `TO`, `BODY`.
- Server nhận dữ liệu:
  - Kiểm tra người nhận `mail_data/<TO>` có tồn tại hay không.
  - Nếu không tồn tại -> Trả về `SEND_MAIL_FAIL - Nguoi nhan [...] khong ton tai tren Server!`.
  - Nếu tồn tại -> Sinh tên file dạng `mail_001.txt`, `mail_002.txt` tránh ghi đè, ghi nội dung thư và trả về `SEND_MAIL_SUCCESS`.

## 3. Kiến thức đã học
- **Multi-part Data Transmission**: Gửi/nhận nhiều dữ liệu nối tiếp qua Socket Stream (`writeUTF` / `readUTF` theo đúng thứ tự).
- **Recipient Validation**: Kiểm tra thư mục người nhận bằng `File.exists()`.
- **Sequential File Generator**: Thuật toán vòng lặp `while(true)` kết hợp `String.format("mail_%03d.txt", index)` để tạo tên file động không trùng lặp.
- **Mail Format Standard**: Định dạng thư gồm Header (`FROM`, `TO`) và `BODY`.

## 4. Code & Giải thích chi tiết

### Server Code: `server/MailServer.java`
```java
File recipientFolder = new File("mail_data", toUser);
if (!recipientFolder.exists()) {
    return "SEND_MAIL_FAIL - Nguoi nhan khong ton tai!";
}
int index = 1;
File mailFile;
while (true) {
    String fileName = String.format("mail_%03d.txt", index);
    mailFile = new File(recipientFolder, fileName);
    if (!mailFile.exists()) break;
    index++;
}
mailFile.createNewFile();
```

### Client Code: `client/MailClient.java`
```java
dos.writeUTF("SEND_MAIL");
dos.writeUTF(fromUser);
dos.writeUTF(toUser);
dos.writeUTF(body);
dos.flush();
```

## 5. Lỗi phổ biến & Debug
- **Lệch thứ tự Stream**: Client gửi `FROM` -> `TO` -> `BODY` nhưng Server đọc `TO` -> `FROM` -> `BODY`.
- **Ghi đè file thư**: Không sử dụng thuật toán sinh tên file động làm mất các email cũ.
