package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class MailServer {
    public static final String MAIL_DATA_DIR = "mail_data";
    private static ServerSocket serverSocket;
    private static volatile boolean isRunning = false;
    private static final AtomicInteger connectedClientsCount = new AtomicInteger(0);
    
    // Callback listener interface cho Server GUI
    public interface LogListener {
        void onLog(String message);
        void onClientCountChanged(int count);
    }

    private static final CopyOnWriteArrayList<LogListener> logListeners = new CopyOnWriteArrayList<>();

    public static void addLogListener(LogListener listener) {
        logListeners.add(listener);
    }

    public static void removeLogListener(LogListener listener) {
        logListeners.remove(listener);
    }

    public static void log(String msg) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String formatted = "[" + timestamp + "] " + msg;
        System.out.println(formatted);
        for (LogListener listener : logListeners) {
            listener.onLog(formatted);
        }
    }

    private static void notifyClientCount() {
        int count = connectedClientsCount.get();
        for (LogListener listener : logListeners) {
            listener.onClientCountChanged(count);
        }
    }

    public static synchronized void startServer(int port) throws IOException {
        if (isRunning) return;
        serverSocket = new ServerSocket(port);
        isRunning = true;
        log("Mail Server started on port " + port);

        Thread acceptThread = new Thread(() -> {
            while (isRunning && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    connectedClientsCount.incrementAndGet();
                    notifyClientCount();
                    log("Client connected from " + clientSocket.getRemoteSocketAddress());
                    new Thread(new ClientHandler(clientSocket)).start();
                } catch (IOException e) {
                    if (!isRunning) {
                        break;
                    }
                    log("Accept error: " + e.getMessage());
                }
            }
        });
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public static synchronized void stopServer() {
        isRunning = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
        }
        log("Mail Server stopped.");
        connectedClientsCount.set(0);
        notifyClientCount();
    }

    public static boolean isRunning() {
        return isRunning;
    }

    public static int getConnectedClientsCount() {
        return connectedClientsCount.get();
    }

    public static void main(String[] args) {
        int port = 2345;
        try {
            startServer(port);
            System.out.println("Nhan ENTER de dung Server...");
            System.in.read();
            stopServer();
        } catch (IOException e) {
            System.err.println("Loi Server: " + e.getMessage());
        }
    }

    // Luồng xử lý từng Client kết nối
    private static class ClientHandler implements Runnable {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream dis = new DataInputStream(socket.getInputStream());
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

                // Đọc loại Request từ Client
                String requestType = dis.readUTF();

                String response;
                if (requestType.startsWith("CREATE_USER")) {
                    String[] parts = requestType.split(" ", 3);
                    if (parts.length < 3 || parts[1].trim().isEmpty() || parts[2].trim().isEmpty()) {
                        response = "CREATE_USER_FAIL - Username va mat khau khong duoc de rong!";
                    } else {
                        String username = parts[1].trim();
                        String password = parts[2].trim();
                        log("REGISTER " + username); // An toàn: Không log password
                        response = handleCreateUser(username, password);
                    }
                } else if (requestType.startsWith("LOGIN")) {
                    String[] parts = requestType.split(" ", 3);
                    if (parts.length < 3 || parts[1].trim().isEmpty() || parts[2].trim().isEmpty()) {
                        response = "LOGIN_FAILED - Username va mat khau dang nhap khong duoc de rong!";
                    } else {
                        String username = parts[1].trim();
                        String password = parts[2].trim();
                        log("LOGIN " + username); // An toàn: Không log password
                        response = handleLogin(username, password);
                    }
                } else if (requestType.startsWith("GET_EMAILS") || requestType.startsWith("REFRESH_MAILBOX")) {
                    String[] parts = requestType.split(" ", 2);
                    if (parts.length < 2 || parts[1].trim().isEmpty()) {
                        response = "GET_EMAILS_FAIL - Username khong duoc de rong!";
                    } else {
                        String username = parts[1].trim();
                        log("GET_EMAILS " + username);
                        response = handleGetEmails(username);
                    }
                } else if (requestType.startsWith("READ_MAIL")) {
                    String[] parts = requestType.split(" ", 3);
                    if (parts.length < 3 || parts[1].trim().isEmpty() || parts[2].trim().isEmpty()) {
                        response = "READ_MAIL_FAIL - Thieu thong tin doc file!";
                    } else {
                        String username = parts[1].trim();
                        String fileName = parts[2].trim();
                        log("READ_MAIL " + username + " -> " + fileName);
                        response = handleReadMail(username, fileName);
                    }
                } else if (requestType.equals("SEND_MAIL")) {
                    String fromUser = dis.readUTF();
                    String toUser = dis.readUTF();
                    String body = dis.readUTF();

                    log("SEND_MAIL " + fromUser + " -> " + toUser);

                    response = handleSendMail(fromUser, toUser, body);
                } else {
                    response = "UNKNOWN_COMMAND - Lenh khong hop le!";
                }

                // Gửi Response về Client
                dos.writeUTF(response);
                dos.flush();

            } catch (IOException e) {
                log("Client handling exception: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {}
                connectedClientsCount.decrementAndGet();
                notifyClientCount();
                log("Client disconnected.");
            }
        }
    }

    public static String handleCreateUser(String username, String password) {
        File userFolder = new File(MAIL_DATA_DIR, username);

        if (userFolder.exists()) {
            return "CREATE_USER_FAIL - User [" + username + "] da ton tai!";
        }

        // Tạo thư mục cá nhân thật trên ổ đĩa
        boolean createdDir = userFolder.mkdirs();
        if (!createdDir) {
            return "CREATE_USER_FAIL - Khong the tao thu muc cho user!";
        }

        // Lưu mật khẩu vào file password.txt trong thư mục user
        File passFile = new File(userFolder, "password.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(passFile))) {
            writer.write(password);
            writer.newLine();
        } catch (IOException e) {
            return "CREATE_USER_FAIL - Loi luu mat khau: " + e.getMessage();
        }

        // Đại diện file mail_data/<username>/new_email.txt
        File newEmailFile = new File(userFolder, "new_email.txt");
        try {
            boolean createdFile = newEmailFile.createNewFile();
            if (createdFile) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(newEmailFile))) {
                    writer.write("WELCOME TO MAIL SERVER SYSTEM!");
                    writer.newLine();
                }
                log("Account created: " + username);
                return "CREATE_USER_SUCCESS - Tai khoan [" + username + "] da duoc tao thanh cong!";
            } else {
                return "CREATE_USER_FAIL - Khong the tao file new_email.txt!";
            }
        } catch (IOException e) {
            return "CREATE_USER_FAIL - Loi File System: " + e.getMessage();
        }
    }

    public static String handleLogin(String username, String password) {
        File userFolder = new File(MAIL_DATA_DIR, username);

        // 1. Kiểm tra thư mục tài khoản
        if (!userFolder.exists() || !userFolder.isDirectory()) {
            return "LOGIN_FAILED - Tai khoan [" + username + "] khong ton tai tren Server!";
        }

        // 2. Kiểm tra mật khẩu
        File passFile = new File(userFolder, "password.txt");
        if (!passFile.exists()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(passFile))) {
                writer.write(password);
                writer.newLine();
            } catch (IOException e) {
                return "LOGIN_FAILED - Khong thiet lap duoc mat khau!";
            }
        } else {
            try (BufferedReader reader = new BufferedReader(new FileReader(passFile))) {
                String storedPassword = reader.readLine();
                if (storedPassword == null || !storedPassword.trim().equals(password.trim())) {
                    log("LOGIN FAILED for " + username + " (Wrong password)");
                    return "LOGIN_FAILED - Sai mat khau!";
                }
            } catch (IOException e) {
                return "LOGIN_FAILED - Loi doc mat khau: " + e.getMessage();
            }
        }

        log("Authentication successful for " + username);

        // 3. Đọc tất cả các file trong thư mục user bằng listFiles() (bỏ qua file password.txt)
        File[] files = userFolder.listFiles();
        StringBuilder sb = new StringBuilder();
        sb.append("LOGIN SUCCESS\n");
        sb.append("Danh sach email cua ").append(username).append(":\n");

        if (files == null || files.length == 0) {
            sb.append("(Thu muc trong, khong co email)");
        } else {
            int count = 1;
            for (File file : files) {
                if (file.isFile() && !file.getName().equals("password.txt")) {
                    sb.append(count).append(". ").append(file.getName()).append("\n");
                    count++;
                }
            }
        }

        return sb.toString().trim();
    }

    public static String handleGetEmails(String username) {
        File userFolder = new File(MAIL_DATA_DIR, username);

        if (!userFolder.exists() || !userFolder.isDirectory()) {
            return "GET_EMAILS_FAIL - Tai khoan [" + username + "] khong ton tai tren Server!";
        }

        File[] files = userFolder.listFiles();
        StringBuilder sb = new StringBuilder();
        sb.append("LOGIN SUCCESS\n");
        sb.append("Danh sach email cua ").append(username).append(":\n");

        if (files == null || files.length == 0) {
            sb.append("(Thu muc trong, khong co email)");
        } else {
            int count = 1;
            for (File file : files) {
                if (file.isFile() && !file.getName().equals("password.txt")) {
                    sb.append(count).append(". ").append(file.getName()).append("\n");
                    count++;
                }
            }
        }

        return sb.toString().trim();
    }

    public static String handleReadMail(String username, String fileName) {
        File userFolder = new File(MAIL_DATA_DIR, username);
        File mailFile = new File(userFolder, fileName);
        if (!mailFile.exists() || !mailFile.isFile()) {
            return "READ_MAIL_FAIL - File email khong ton tai!";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("READ_MAIL_SUCCESS\n");
        try (BufferedReader reader = new BufferedReader(new FileReader(mailFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            return "READ_MAIL_FAIL - Loi doc file: " + e.getMessage();
        }
        return sb.toString().trim();
    }

    public static String handleSendMail(String fromUser, String toUser, String body) {
        File recipientFolder = new File(MAIL_DATA_DIR, toUser);

        if (!recipientFolder.exists() || !recipientFolder.isDirectory()) {
            return "SEND_MAIL_FAIL - Nguoi nhan [" + toUser + "] khong ton tai tren Server!";
        }

        // 1. Tạo file trong hòm thư người nhận (mail_001.txt, mail_002.txt...)
        int index = 1;
        File mailFile;
        while (true) {
            String fileName = String.format("mail_%03d.txt", index);
            mailFile = new File(recipientFolder, fileName);
            if (!mailFile.exists()) {
                break;
            }
            index++;
        }

        // 2. Tạo bản sao file trong hòm thư người gửi (sent_001.txt, sent_002.txt...)
        File senderFolder = new File(MAIL_DATA_DIR, fromUser);
        int sentIndex = 1;
        File sentFile = null;
        if (senderFolder.exists()) {
            while (true) {
                String sentFileName = String.format("sent_%03d.txt", sentIndex);
                sentFile = new File(senderFolder, sentFileName);
                if (!sentFile.exists()) {
                    break;
                }
                sentIndex++;
            }
        }

        try {
            boolean created = mailFile.createNewFile();
            if (created) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(mailFile))) {
                    writer.write("FROM: " + fromUser);
                    writer.newLine();
                    writer.write("TO: " + toUser);
                    writer.newLine();
                    writer.newLine();
                    writer.write(body);
                    writer.newLine();
                }

                // Ghi file bản sao thư đã gửi vào thư mục người gửi
                if (sentFile != null) {
                    sentFile.createNewFile();
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(sentFile))) {
                        writer.write("FROM: " + fromUser);
                        writer.newLine();
                        writer.write("TO: " + toUser);
                        writer.newLine();
                        writer.newLine();
                        writer.write(body);
                        writer.newLine();
                    }
                }

                log("Email saved: " + mailFile.getName() + " for " + toUser + " | Sent copy: " + (sentFile != null ? sentFile.getName() : "N/A"));
                return "SEND_MAIL_SUCCESS - Da gui email den [" + toUser + "] (File nhan: " + mailFile.getName() + ")!";
            } else {
                return "SEND_MAIL_FAIL - Khong the tao file email!";
            }
        } catch (IOException e) {
            return "SEND_MAIL_FAIL - Loi File System: " + e.getMessage();
        }
    }
}
