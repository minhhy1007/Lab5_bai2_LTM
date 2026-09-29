# PHASE 7 – HOÀN THIỆN, NÂNG CAP, TEST TOÀN BỘ HỆ THỐNG VÀ BỘ CÂU HỎI VẤN ĐÁP 30 CÂU

## 1. Mục tiêu
- Không thêm chức năng lớn mới.
- Tổng hợp toàn bộ sơ đồ kiến trúc hệ thống Mail Server bằng Java TCP Socket.
- Xây dựng quy trình Debug 6 câu hỏi và Bảng Test 12 kịch bản chuẩn.
- Đóng gói Bộ Vấn Đáp 30 câu hỏi chuyên sâu phục vụ bảo vệ bài tập lớn / thi vấn đáp.

## 2. Sơ đồ toàn hệ thống

```text
                    MAIL SERVER
                         │
              ┌──────────┼──────────┐
              │          │          │
              ▼          ▼          ▼
        CREATE_USER   SEND_MAIL    LOGIN
              │          │          │
              ▼          ▼          ▼
           Folder     Recipient   Folder
              │          │          │
              ▼          ▼          ▼
       new_email.txt  mail_x.txt  listFiles()
                                    │
                                    ▼
                              Email filenames
```

## 3. Bảng kịch bản Test 12 kịch bản

| STT | Chức năng | Input | Kết quả mong đợi |
| :--- | :--- | :--- | :--- |
| 1 | Khởi chạy Server | Run `MailServer` | Server mở port 2345, lắng nghe `accept()` |
| 2 | Kết nối Client | Run `MailClient` | Client nối thành công tới `localhost:2345` |
| 3 | Tạo user mới | `CREATE_USER king` | Tạo folder `mail_data/king` & `new_email.txt` |
| 4 | Tạo trùng user | `CREATE_USER king` | Báo lỗi `CREATE_USER_FAIL - User da ton tai!` |
| 5 | Tạo user 2 | `CREATE_USER user1` | Tạo folder `mail_data/user1` & `new_email.txt` |
| 6 | Gửi mail hợp lệ | `king` -> `user1` | Tạo file `mail_data/user1/mail_001.txt` |
| 7 | Người nhận không có | `king` -> `abc` | Báo lỗi `SEND_MAIL_FAIL - Nguoi nhan khong ton tai!` |
| 8 | Login user hợp lệ | `LOGIN king` | Báo `LOGIN SUCCESS` và danh sách email |
| 9 | Login user không có | `LOGIN abc` | Báo `LOGIN_FAILED - Tai khoan khong ton tai!` |
| 10 | Gửi nhiều mail | `king` -> `user1` (lần 2) | Sinh file `mail_002.txt` không đè `mail_001.txt` |
| 11 | Client ngắt đột ngột | Tắt Client | Server ném IOException/EOF Exception |
| 12 | Server ngắt đột ngột | Tắt Server | Client báo ConnectException |

## 4. Quy trình Debug theo 6 câu hỏi chuẩn
1. Lỗi xảy ra ở phía Client hay phía Server?
2. Lỗi xảy ra ở giai đoạn Connect, Request, Process hay Response?
3. Dữ liệu phía Client gửi đi có đúng định dạng không?
4. Phía Server đọc dữ liệu có đúng thứ tự `readUTF()` không?
5. Cấu trúc File/Folder trên Server đĩa cứng có chính xác không?
6. Chuỗi Response phản hồi về phía Client có bị rỗng hay sai không?

## 5. Bộ Vấn Đáp 30 Câu Chuyên Sâu (Phân theo Chủ đề)

### Topic 1: TCP Protocol & Network Basics
1. **TCP là gì? Tại sao ứng dụng Mail Server bắt buộc phải dùng TCP thay vì UDP?**
   - *Trả lời*: TCP là giao thức hướng kết nối (Connection-oriented), đảm bảo dữ liệu truyền đi không bị mất mát, đúng thứ tự và không trùng lặp nhờ bắt tay 3 bước và cơ chế ACK. Mail Server cần độ tin cậy tuyệt đối về mặt nội dung thư nên chọn TCP.
2. **"Connection-oriented" (Hướng kết nối) nghĩa là gì?**
   - *Trả lời*: Nghĩa là trước khi truyền dữ liệu thực sự, 2 máy phải thực hiện thủ tục bắt tay 3 bước để thiết lập kênh kết nối hợp lệ.
3. **TCP có đảm bảo giữ nguyên thứ tự dữ liệu truyền đi không?**
   - *Trả lời*: Có, TCP đánh số thứ tự gói tin (Sequence Number) và sắp xếp lại tại bên nhận.
4. **TCP có tự hiểu câu lệnh `CREATE_USER` hay `LOGIN` không?**
   - *Trả lời*: Không. TCP chỉ hiểu byte thô. Các câu lệnh này là giao thức cấp ứng dụng (Application Protocol) do lập trình viên quy ước.
5. **Khái niệm Ephemeral Port là gì? Nó dùng cho Client hay Server?**
   - *Trả lời*: Là cổng động tạm thời do Hệ điều hành tự động cấp phát cho Client khi khởi tạo kết nối.

### Topic 2: Socket & Java Networking
6. **`ServerSocket` khác `Socket` như thế nào?**
   - *Trả lời*: `ServerSocket` dùng phía Server để mở cổng lắng nghe kết nối (`accept()`). `Socket` đại diện cho kênh giao tiếp 1-1 giữa 2 máy.
7. **Hàm `accept()` hoạt động như thế nào?**
   - *Trả lời*: Là hàm chặn đồng bộ (Blocking call), tạm dừng luồng chạy cho đến khi có Client bắt tay TCP thành công và trả về một kết nối `Socket` mới.
8. **Tác dụng của `DataInputStream` và `DataOutputStream`?**
   - *Trả lời*: Là lớp bọc (Decorator Stream) giúp đọc/ghi các kiểu dữ liệu nguyên thủy và chuỗi chữ UTF-8 dễ dàng thay vì làm việc với byte thô.
9. **Tại sao luôn cần lệnh `flush()` sau `writeUTF()`?**
   - *Trả lời*: Ép tống toàn bộ dữ liệu trong bộ đệm (Buffer) ra đường truyền mạng ngay lập tức, tránh trễ dữ liệu.
10. **Tài nguyên Socket sẽ ra sao nếu quên gọi `close()`?**
    - *Trả lời*: Gây rò rỉ tài nguyên (Resource Leak), cổng mạng bị treo ở trạng thái TIME_WAIT.

### Topic 3: Client - Server Architecture
11. **Mô hình Client - Server hoạt động ra sao?**
    - *Trả lời*: Server đóng vai trò lắng nghe thụ động tại 1 địa chỉ/cổng cố định. Client đóng vai trò chủ động kết nối tới Server để gửi yêu cầu.
12. **Tại sao Server phải được chạy trước Client?**
    - *Trả lời*: Vì nếu Server chưa bật, không có cổng nào lắng nghe, Client gửi gói SYN sẽ bị OS từ chối ngay (`ConnectException`).
13. **Một ServerSocket có thể phục vụ nhiều Socket kết nối cùng lúc không?**
    - *Trả lời*: Có, mỗi lần `accept()` thành công sẽ sinh ra 1 `Socket` độc lập kết nối tới Client tương ứng.

### Topic 4: Application Protocol
14. **Request là gì? Response là gì?**
    - *Trả lời*: Request là câu lệnh yêu cầu gửi từ Client sang Server. Response là thông điệp phản hồi kết quả gửi từ Server về Client.
15. **CREATE_USER hay LOGIN thuộc tầng nào trong mô hình OSI?**
    - *Trả lời*: Thuộc tầng Ứng Dụng (Application Layer).
16. **Làm thế nào Server phân biệt được các loại lệnh khác nhau gửi từ Client?**
    - *Trả lời*: Nhờ đọc chuỗi Request đầu tiên và rẽ nhánh `if/else` bằng các hàm kiểm tra chuỗi như `startsWith()` hoặc `equals()`.

### Topic 5: File System & Data Storage
17. **Vì sao mỗi user cần có một thư mục riêng trong `mail_data/`?**
    - *Trả lời*: Để phân vùng lưu trữ (Isolation), tránh việc chồng chéo hay rò rỉ thư từ giữa các tài khoản khác nhau.
18. **`mkdir()` khác `mkdirs()` như thế nào?**
    - *Trả lời*: `mkdir()` chỉ tạo thư mục cuối nếu thư mục cha đã có. `mkdirs()` tự động tạo toàn bộ cây thư mục cha nếu chưa có.
19. **Tác dụng của `createNewFile()`?**
    - *Trả lời*: Tạo một tập tin vật lý mới rỗng 0-byte trên đĩa cứng Server.
20. **Hàm `listFiles()` trả về cái gì?**
    - *Trả lời*: Trả về mảng `File[]` chứa tất cả các đối tượng file/folder con nằm bên trong thư mục.

### Topic 6: Email Handling Logic
21. **Server xác định người nhận email dựa vào đâu?**
    - *Trả lời*: Dựa vào tham số `TO` trong gói tin `SEND_MAIL` và đối chiếu với đường dẫn `mail_data/<TO>`.
22. **Làm thế nào để tránh ghi đè khi một người nhận được nhiều email?**
    - *Trả lời*: Sử dụng thuật toán vòng lặp sinh tên file động (`mail_001.txt`, `mail_002.txt`...) bằng cách kiểm tra `mailFile.exists()`.
23. **Email được lưu trữ ở đâu trong hệ thống?**
    - *Trả lời*: Lưu trữ trực tiếp trên đĩa cứng của **Server** dưới dạng các file văn bản `.txt`.

### Topic 7: Troubleshooting & Debugging
24. **Gặp lỗi `ConnectException: Connection refused` thì xử lý thế nào?**
    - *Trả lời*: Kiểm tra xem Server đã Run chưa, đúng địa chỉ IP `localhost` và đúng Port `2345` chưa.
25. **Gặp lỗi `BindException: Address already in use` thì xử lý thế nào?**
    - *Trả lời*: Tắt phiên bản Server đang chạy ngầm hoặc đổi sang một số Port khác chưa bị chiếm dụng.
26. **Nếu Client gửi dữ liệu nhưng Server không nhận được thì kiểm tra gì?**
    - *Trả lời*: Kiểm tra xem phía Client đã gọi `dos.flush()` chưa và phía Server đã gọi hàm `dis.readUTF()` đúng thứ tự chưa.
27. **Nếu Đăng nhập thành công nhưng không thấy danh sách email thì kiểm tra gì?**
    - *Trả lời*: Kiểm tra hàm `handleLogin` đã gọi `listFiles()` chưa và biến `userFolder` có trỏ đúng vào thư mục `mail_data/<username>` không.
28. **Chương trình bị đơ (Deadlock) ở dòng `readUTF()` là do đâu?**
    - *Trả lời*: Do cả 2 bên Client và Server cùng đứng chờ `readUTF()` mà không bên nào chịu gọi `writeUTF()` trước.
29. **Tại sao phải dùng `StringBuilder` khi gửi danh sách email thay vì gửi từng file một?**
    - *Trả lời*: Để gom toàn bộ thông điệp thành 1 gói duy nhất gửi qua Socket, giảm hiện tượng nghẽn mạng (Network Overhead).
30. **Sự khác biệt cốt lõi giữa Socket Stream (tầng mạng) và File System (tầng đĩa) là gì?**
    - *Trả lời*: Socket Stream dùng để vận chuyển dữ liệu tạm thời giữa 2 máy qua mạng. File System dùng để lưu trữ dữ liệu bền vững vĩnh viễn trên đĩa cứng Server.
