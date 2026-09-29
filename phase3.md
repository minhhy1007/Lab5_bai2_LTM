# PHASE 3 – CREATE USER + FOLDER + FILE (TẠO TÀI KHOẢN VÀ THƯ MỤC THẬT)

## 1. Mục tiêu
- Xây dựng chức năng Tạo tài khoản người dùng thật trên Server.
- Khởi tạo thư mục `mail_data/<username>/` và file `new_email.txt` trên ổ đĩa Server khi Client gửi lệnh `CREATE_USER <username>`.

## 2. Chức năng đạt được
- Client gửi `CREATE_USER king`.
- Server kiểm tra:
  - Nếu `username` trống -> Trả về `CREATE_USER_FAIL - Ten user khong duoc de rong!`.
  - Nếu thư mục `mail_data/king` đã tồn tại -> Trả về `CREATE_USER_FAIL - User [king] da ton tai!`.
  - Nếu chưa tồn tại -> Tạo folder `mail_data/king`, tạo file `new_email.txt`, ghi dòng chào mừng và trả về `CREATE_USER_SUCCESS`.

## 3. Kiến thức đã học
- **`java.io.File`**: Thao tác kiểm tra và tạo file/folder trong Java (`exists()`, `mkdirs()`, `createNewFile()`).
- **`BufferedWriter` / `FileWriter`**: Ghi văn bản vào file có bộ đệm.
- **Phân định kiến thức**: Socket truyền nhận Request/Response ở tầng mạng, File System lưu trữ dữ liệu vĩnh viễn trên đĩa cứng Server.

## 4. Code & Giải thích chi tiết

### Server Code: `server/MailServer.java`
```java
File userFolder = new File("mail_data", username);
if (userFolder.exists()) {
    return "CREATE_USER_FAIL - User [" + username + "] da ton tai!";
}
userFolder.mkdirs();
File newEmailFile = new File(userFolder, "new_email.txt");
newEmailFile.createNewFile();
```

### Client Code: `client/MailClient.java`
```java
dos.writeUTF("CREATE_USER king");
dos.flush();
String response = dis.readUTF();
```

## 5. Lỗi phổ biến & Debug
- **Ký tự cấm Windows**: Đặt tên username chứa `?`, `*`, `:` gây ra `IOException`.
- **Lỗi đè dữ liệu**: Không kiểm tra `exists()` dẫn tới việc ghi đè hoặc tạo lại tài khoản đã có.
