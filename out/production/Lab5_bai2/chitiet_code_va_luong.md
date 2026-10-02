# BẢN NỔI ÁNH GIẢI THÍCH CHI TIẾT DÒNG CODE, KIẾN THỨC VÀ LUỒNG HOẠT ĐỘNG

---

## MODULE 1: THIẾT LẬP KẾT NỐI TCP SOCKET (HANDSHAKE & PORT BINDING)

### 📌 1. Các dòng code chính
- **Server**: [server/MailServer.java](file:///d:/Lap_Trinh_Mang/Lab5_bai2/server/MailServer.java#L15-L21)
  ```java
  ServerSocket serverSocket = new ServerSocket(2345);
  Socket clientSocket = serverSocket.accept();
  ```
- **Client**: [client/MailClient.java](file:///d:/Lap_Trinh_Mang/Lab5_bai2/client/MailClient.java#L16)
  ```java
  Socket socket = new Socket("localhost", 2345);
  ```

### 🔍 2. Giải thích chi tiết từng dòng code

#### 🔹 Dòng 1: `ServerSocket serverSocket = new ServerSocket(2345);`
- **Nó làm gì?**: Yêu cầu Hệ điều hành mở và đăng ký cổng `2345` trên máy tính làm cổng lắng nghe kết nối mạng TCP cho ứng dụng Server.
- **Tại sao cần?**: Để các ứng dụng Client biết đường tìm đến đúng "địa chỉ nhà" (Port) của Mail Server.
- **Dữ liệu đi đâu?**: Đăng ký Socket xuống tầng Transport (TCP) của Hệ điều hành.
- **Nếu bỏ đoạn này thì sao?**: Server không có cổng mở, Client kết nối tới sẽ ném lỗi `ConnectException: Connection refused`.
- **Liên quan kiến thức mạng nào?**: Port Binding & Static Server Listening Port.

#### 🔹 Dòng 2: `Socket clientSocket = serverSocket.accept();`
- **Nó làm gì?**: Dừng tạm thời (Block) luồng thực thi của Server để chờ cho tới khi có một Client kết nối thành công tới port `2345`. Hàm trả về một đối tượng `Socket` riêng đại diện cho kênh nối 1-1 với Client đó.
- **Tại sao cần?**: `ServerSocket` chỉ đóng vai trò lắng nghe, còn `clientSocket` mới là đối tượng trực tiếp trao đổi dữ liệu với Client.
- **Dữ liệu đi đâu?**: Tiếp nhận kết quả hoàn tất thủ tục Bắt tay 3 bước (3-Way Handshake) từ OS.
- **Nếu bỏ đoạn này thì sao?**: Server chạy thẳng xuống cuối hàm `main` và thoát ngay lập tức mà chưa kịp phục vụ Client nào.
- **Liên quan kiến thức mạng nào?**: Blocking I/O & TCP 3-Way Handshake Completion.

#### 🔹 Dòng 3: `Socket socket = new Socket("localhost", 2345);`
- **Nó làm gì?**: Client xin OS cấp một Port ngẫu nhiên (Ephemeral Port), đồng thời chủ động gửi gói tin `SYN` sang IP `localhost` (`127.0.0.1`) tại Port `2345` để xin kết nối.
- **Tại sao cần?**: Khởi tạo kết nối mạng hướng Server.
- **Dữ liệu đi đâu?**: Gửi các gói tin SYN / ACK qua card mạng Loopback.
- **Nếu bỏ đoạn này thì sao?**: Client không tạo được kết nối mạng tới Server.
- **Liên quan kiến thức mạng nào?**: Active Connection Initiation & Ephemeral Client Port.

### 🔄 3. Luồng hoạt động (Execution Flow)
1. **Server** thực thi `new ServerSocket(2345)` $\rightarrow$ OS mở Port 2345 $\rightarrow$ Server chạy tới `accept()` và **tạm dừng luồng (Blocked)**.
2. **Client** thực thi `new Socket("localhost", 2345)`.
3. OS gửi gói `SYN` từ Client $\rightarrow$ Server đáp lại `SYN-ACK` $\rightarrow$ Client đáp lại `ACK` (Bắt tay 3 bước hoàn tất).
4. Hàm `accept()` ở Server ngắt trạng thái tạm dừng, trả về đối tượng `clientSocket`. Kênh TCP 1-1 được thiết lập hoàn toàn!

---

## MODULE 2: ĐÓNG GÓI VÀ TRUYỀN NHẬN STREAM DỮ LIỆU (SOCKET STREAMS & FLUSHING)

### 📌 1. Các dòng code chính
- **Server & Client**: [server/MailServer.java](file:///d:/Lap_Trinh_Mang/Lab5_bai2/server/MailServer.java#L24-L25) / [client/MailClient.java](file:///d:/Lap_Trinh_Mang/Lab5_bai2/client/MailClient.java#L21-L22)
  ```java
  DataInputStream dis = new DataInputStream(socket.getInputStream());
  DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

  dos.writeUTF(data);
  dos.flush();
  String data = dis.readUTF();
  ```

### 🔍 2. Giải thích chi tiết từng dòng code

#### 🔹 Dòng 1: `DataInputStream dis = new DataInputStream(socket.getInputStream());`
- **Nó làm gì?**: Lấy dòng nhập byte thô (`InputStream`) từ socket và bọc vào `DataInputStream`.
- **Tại sao cần?**: `InputStream` thuần chỉ đọc từng byte đơn lẻ. `DataInputStream` cung cấp hàm `readUTF()` để đọc cả chuỗi chữ `String` mã hóa UTF-8.
- **Dữ liệu đi đâu?**: Chuẩn bị luồng nhận dữ liệu từ card mạng vào bộ nhớ RAM.
- **Nếu bỏ đoạn này thì sao?**: Bạn phải tự viết code đọc từng mảng `byte[]` và ép kiểu lại thành chuỗi chữ rất phức tạp và dễ lỗi.
- **Liên quan kiến thức mạng nào?**: Stream Decorator Pattern & Character Encoding/Decoding.

#### 🔹 Dòng 2: `dos.writeUTF(data); dos.flush();`
- **Nó làm gì?**: `writeUTF()` đính kèm 2 byte độ dài vào đầu chuỗi rồi đẩy chuỗi vào bộ nhớ đệm (Buffer). `flush()` lập tức ép bộ đệm tống toàn bộ mảng byte đó ra đường truyền mạng.
- **Tại sao cần?**: Nếu không có `flush()`, dữ liệu nhỏ có thể bị trễ trong bộ đệm RAM mà chưa được gửi ngay sang bên nhận.
- **Dữ liệu đi đâu?**: Đẩy ra card mạng truyền qua cáp/wifi tới máy đối phương.
- **Nếu bỏ `flush()`**: Phía bên nhận có thể bị ngưng trệ (Lag/Treo) do đứng chờ dữ liệu mãi không tới.
- **Liên quan kiến thức mạng nào?**: Stream Buffer Flushing & TCP Push Flag.

#### 🔹 Dòng 3: `String data = dis.readUTF();`
- **Nó làm gì?**: Đọc 2 byte độ dài đầu tiên từ socket, sau đó đọc đúng số byte nội dung tương ứng để tái tạo lại chuỗi String ban đầu.
- **Tại sao cần?**: Để ứng dụng lấy lại đúng nguyên vẹn câu lệnh chữ mà phía đối diện vừa gửi.
- **Dữ liệu đi đâu?**: Chuyển mảng byte mạng thành biến `String` trong RAM.
- **Nếu bỏ đoạn này thì sao?**: Ứng dụng không thể đọc được thông điệp của đối phương.
- **Liên quan kiến thức mạng nào?**: Blocking Network Read.

### 🔄 3. Luồng hoạt động (Execution Flow)
1. Bên gửi gọi `dos.writeUTF("HELLO")` + `dos.flush()`.
2. Java chuyển chuỗi `"HELLO"` thành mảng byte UTF-8 $\rightarrow$ Ép tống ra card mạng.
3. Tầng TCP chia nhỏ mảng byte thành các gói IP gửi sang máy bên nhận.
4. Bên nhận đang dừng chờ ở `dis.readUTF()` $\rightarrow$ Nhận đủ mảng byte $\rightarrow$ Giải mã ngược lại thành chuỗi `"HELLO"`.

---

## MODULE 3: ĐĂNG KÝ TÀI KHOẢN VÀ CẤU TRÚC HÒM THƯ (`CREATE_USER`)

### 📌 1. Các dòng code chính
- **Server**: [server/MailServer.java](file:///d:/Lap_Trinh_Mang/Lab5_bai2/server/MailServer.java#L64-L92) (Hàm `handleCreateUser`)
  ```java
  File userFolder = new File(MAIL_DATA_DIR, username);
  if (userFolder.exists()) return "CREATE_USER_FAIL...";

  userFolder.mkdirs();
  File newEmailFile = new File(userFolder, "new_email.txt");
  newEmailFile.createNewFile();
  ```

### 🔍 2. Giải thích chi tiết từng dòng code

#### 🔹 Dòng 1: `File userFolder = new File("mail_data", username);`
- **Nó làm gì?**: Khởi tạo đối tượng quản lý đường dẫn `mail_data/<username>` trong RAM Server.
- **Tại sao cần?**: Đại diện cho thư mục cá nhân của user trước khi thực hiện thao tác kiểm tra hay tạo mới.
- **Dữ liệu đi đâu?**: Nằm trong bộ nhớ RAM của Server (chưa tác động đĩa).

#### 🔹 Dòng 2: `if (userFolder.exists()) return "CREATE_USER_FAIL...";`
- **Nó làm gì?**: Hỏi Hệ điều hành xem thư mục `mail_data/<username>` đã có trên đĩa cứng hay chưa.
- **Tại sao cần?**: Ngăn chặn việc đăng ký trùng tên tài khoản đã tồn tại.
- **Dữ liệu đi đâu?**: Truy vấn trực tiếp File System của đĩa cứng Server.
- **Nếu bỏ đoạn này thì sao?**: Server không phát hiện tài khoản trùng lặp.

#### 🔹 Dòng 3: `userFolder.mkdirs();`
- **Nó làm gì?**: Đăng ký với Hệ điều hành tạo thư mục `mail_data/` và thư mục con `<username>/`.
- **Tại sao cần?**: Mỗi người dùng bắt buộc phải có một hòm thư (Folder) riêng biệt để lưu trữ email.
- **Dữ liệu đi đâu?**: Xuất hiện thư mục thật 100% trên đĩa cứng Server.
- **Nếu bỏ đoạn này thì sao?**: Thao tác tạo file thư tiếp theo sẽ bị ném lỗi `FileNotFoundException`.

#### 🔹 Dòng 4: `newEmailFile.createNewFile();`
- **Nó làm gì?**: Tạo tập tin văn bản vật lý `new_email.txt` bên trong thư mục `mail_data/<username>/`.
- **Tại sao cần?**: Tạo file thư mặc định ban đầu theo đúng yêu cầu đề bài.
- **Dữ liệu đi đâu?**: Khởi tạo file trên đĩa cứng Server.

### 🔄 3. Luồng hoạt động (Execution Flow)
1. Client gửi lệnh `CREATE_USER king` qua Socket Stream.
2. Server tách lấy tên `username = "king"`.
3. Server kiểm tra `mail_data/king` trên đĩa $\rightarrow$ Nếu chưa có $\rightarrow$ Gọi `mkdirs()` tạo thư mục $\rightarrow$ Gọi `createNewFile()` tạo file `new_email.txt`.
4. Server phản hồi `CREATE_USER_SUCCESS` về lại cho Client.

---

## MODULE 4: GỬI EMAIL VÀ THUẬT TOÁN SINH TÊN FILE ĐỘNG (`SEND_MAIL`)

### 📌 1. Các dòng code chính
- **Server**: [server/MailServer.java](file:///d:/Lap_Trinh_Mang/Lab5_bai2/server/MailServer.java#L118-L157) (Hàm `handleSendMail`)
  ```java
  File recipientFolder = new File(MAIL_DATA_DIR, toUser);
  if (!recipientFolder.exists()) return "SEND_MAIL_FAIL...";

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

### 🔍 2. Giải thích chi tiết từng dòng code

#### 🔹 Dòng 1: `if (!recipientFolder.exists()) return "SEND_MAIL_FAIL...";`
- **Nó làm gì?**: Kiểm tra xem người nhận (`toUser`) đã có thư mục tài khoản trên Server chưa.
- **Tại sao cần?**: Không thể gửi email cho một người dùng không tồn tại trong hệ thống.
- **Dữ liệu đi đâu?**: Truy vấn đĩa cứng Server.

#### 🔹 Dòng 2: Vòng lặp `while (true)` & `String.format("mail_%03d.txt", index)`
- **Nó làm gì?**: Lần lượt thử các tên file `mail_001.txt`, `mail_002.txt`... Tên nào chưa có trên đĩa thì chọn tên đó làm file thư mới và thoát vòng lặp (`break`).
- **Tại sao cần?**: Giúp các email gửi tới cùng một người nhận được lưu thành các tập tin riêng biệt, **tuyệt đối không đè hỏng hay xóa mất lá thư cũ**.
- **Dữ liệu đi đâu?**: Dò tìm đường dẫn khả dụng trên đĩa cứng.
- **Nếu bỏ đoạn này**: Mọi email gửi tới `user1` sẽ bị ghi đè lên cùng 1 file, làm mất hết lịch sử thư cũ!

### 🔄 3. Luồng hoạt động (Execution Flow)
1. Client gửi 4 chuỗi dữ liệu: `"SEND_MAIL"`, `fromUser`, `toUser`, `body`.
2. Server đọc 4 chuỗi qua `dis.readUTF()`.
3. Server kiểm tra `mail_data/<toUser>` $\rightarrow$ Dò tìm tên file chưa trùng (ví dụ `mail_001.txt`).
4. Server tạo file `mail_001.txt` trong `mail_data/<toUser>/` và dùng `BufferedWriter` ghi `FROM`, `TO`, `BODY` vào file.
5. Server phản hồi `SEND_MAIL_SUCCESS` về Client.

---

## MODULE 5: XÁC THỰC ĐĂNG NHẬP VÀ ĐỌC DANH SÁCH EMAIL (`LOGIN` & `listFiles`)

### 📌 1. Các dòng code chính
- **Server**: [server/MailServer.java](file:///d:/Lap_Trinh_Mang/Lab5_bai2/server/MailServer.java#L94-L116) (Hàm `handleLogin`)
  ```java
  File userFolder = new File(MAIL_DATA_DIR, username);
  if (!userFolder.exists() || !userFolder.isDirectory()) return "LOGIN_FAILED...";

  File[] files = userFolder.listFiles();
  StringBuilder sb = new StringBuilder("LOGIN SUCCESS\n");
  for (File file : files) {
      if (file.isFile()) sb.append(file.getName()).append("\n");
  }
  return sb.toString();
  ```

### 🔍 2. Giải thích chi tiết từng dòng code

#### 🔹 Dòng 1: `if (!userFolder.exists() || !userFolder.isDirectory())`
- **Nó làm gì?**: Kiểm tra đường dẫn thư mục tài khoản `mail_data/<username>` có tồn tại và đúng là thư mục hay không.
- **Tại sao cần?**: Để xác thực người dùng đã đăng ký tài khoản thành công hay chưa.

#### 🔹 Dòng 2: `File[] files = userFolder.listFiles();`
- **Nó làm gì?**: Đọc tất cả các tập tin/thư mục con có trong `mail_data/<username>/` và trả về mảng `File[]`.
- **Tại sao cần?**: Để lấy toàn bộ danh sách các lá thư hiện có trong hòm thư cá nhân của user.
- **Dữ liệu đi đâu?**: Lấy dữ liệu từ đĩa cứng lưu vào mảng `files` trong RAM Server.
- **Nếu bỏ đoạn này thì sao?**: Server không lấy được danh sách file email để trả về cho Client.

#### 🔹 Dòng 3: `file.isFile()` & `file.getName()`
- **Nó làm gì?**: Lọc chỉ lấy tập tin (`isFile()`) và lấy tên ngắn gọn (`getName()` dạng `mail_001.txt`).
- **Tại sao cần?**: Tránh lấy nhầm thư mục con và tránh lộ đường dẫn ổ đĩa tuyệt đối (`d:\Lap_Trinh_Mang\...`).

#### 🔹 Dòng 4: `StringBuilder` & `sb.append(...)`
- **Nó làm gì?**: Gom toàn bộ thông điệp gồm dòng tiêu đề và danh sách tên các file email thành một chuỗi duy nhất.
- **Tại sao cần?**: Tối ưu đường truyền mạng, chỉ cần 1 lần gọi `writeUTF()` để gửi toàn bộ danh sách về Client.

### 🔄 3. Luồng hoạt động (Execution Flow)
1. Client gửi lệnh `LOGIN king`.
2. Server xác thực `mail_data/king` tồn tại trên đĩa.
3. Server gọi `userFolder.listFiles()` $\rightarrow$ Duyệt mảng `File[]` lấy tên từng file `new_email.txt`, `mail_001.txt`...
4. Server ghép thành chuỗi văn bản bằng `StringBuilder` $\rightarrow$ Gửi chuỗi về Client qua `dos.writeUTF()`.
5. Client nhận chuỗi và in ra màn hình Console.
