# PHASE 2 – REQUEST / RESPONSE (GIAO THỨC TẦNG ỨNG DỤNG)

## 1. Mục tiêu
- Xây dựng cơ chế gửi lệnh (Request) từ Client sang Server và nhận phản hồi (Response) từ Server.
- Chưa tạo file/folder thật trên Server, chỉ tập trung vào việc đọc/ghi dữ liệu qua Socket Stream.

## 2. Chức năng đạt được
- Client nhập lệnh từ bàn phím (ví dụ: `CREATE_USER king` hoặc `ABC`).
- Client gửi lệnh qua `DataOutputStream`.
- Server đọc lệnh từ `DataInputStream`.
- Server kiểm tra:
  - Nếu đúng cú pháp bắt đầu bằng `CREATE_USER` -> Trả về `OK - Da nhan lenh CREATE_USER`.
  - Nếu lệnh khác -> Trả về `UNKNOWN_COMMAND - Lenh khong hop le!`.
- Client nhận Response và in ra màn hình.

## 3. Kiến thức đã học
- **Stream Wrapper**: `DataInputStream` và `DataOutputStream` bọc lên `InputStream` / `OutputStream` thô.
- **`writeUTF()` & `readUTF()`**: Mã hóa và giải mã chuỗi String theo chuẩn UTF-8 đính kèm 2 byte độ dài.
- **`flush()`**: Ép bộ đệm (Buffer) đẩy dữ liệu ra đường truyền mạng ngay lập tức.
- **Application Protocol**: `CREATE_USER` hay `OK` là giao thức do ứng dụng quy ước, TCP chỉ đóng vai trò truyền byte thô.

## 4. Code & Giải thích chi tiết

### Server Code: `server/MailServer.java`
```java
DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());

String request = dis.readUTF();
String response;
if (request.startsWith("CREATE_USER")) {
    response = "OK - Da nhan lenh CREATE_USER";
} else {
    response = "UNKNOWN_COMMAND - Lenh khong hop le!";
}
dos.writeUTF(response);
dos.flush();
```

### Client Code: `client/MailClient.java`
```java
DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
DataInputStream dis = new DataInputStream(socket.getInputStream());

String command = scanner.nextLine();
dos.writeUTF(command);
dos.flush();

String response = dis.readUTF();
System.out.println("<- Nhan Response: " + response);
```

## 5. Lỗi phổ biến & Debug
- **Deadlock Stream**: Cả 2 bên cùng gọi `readUTF()` trước khi có bên nào gọi `writeUTF()`.
- **Thiếu `flush()`**: Dữ liệu nhỏ bị treo trong bộ đệm chưa tới được bên nhận.
