package server;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MailServer {
    private static final String MAIL_DATA_DIR = "mail_data";

    public static void main(String[] args) {
        int port = 2345;
        System.out.println("=== MAIL SERVER (PHASE 6 - LIST EMAILS) ===");
        System.out.println("Dang khoi tao Server Socket tai port " + port + "...");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server dang lang nghe va cho Client ket noi...");

            Socket clientSocket = serverSocket.accept();
            System.out.println("-> Client da ket noi thanh cong!");

            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());

            // Đọc loại Request từ Client
            String requestType = dis.readUTF();
            System.out.println("-> Server nhan duoc Request type: [" + requestType + "]");

            String response;
            if (requestType.startsWith("CREATE_USER")) {
                String[] parts = requestType.split(" ", 2);
                if (parts.length < 2 || parts[1].trim().isEmpty()) {
                    response = "CREATE_USER_FAIL - Ten user khong duoc de rong!";
                } else {
                    String username = parts[1].trim();
                    response = handleCreateUser(username);
                }
            } else if (requestType.startsWith("LOGIN")) {
                String[] parts = requestType.split(" ", 2);
                if (parts.length < 2 || parts[1].trim().isEmpty()) {
                    response = "LOGIN_FAILED - Username dang nhap khong duoc de rong!";
                } else {
                    String username = parts[1].trim();
                    response = handleLogin(username);
                }
            } else if (requestType.equals("SEND_MAIL")) {
                String fromUser = dis.readUTF();
                String toUser = dis.readUTF();
                String body = dis.readUTF();

                System.out.println("   + FROM: " + fromUser);
                System.out.println("   + TO: " + toUser);
                System.out.println("   + BODY: " + body);

                response = handleSendMail(fromUser, toUser, body);
            } else {
                response = "UNKNOWN_COMMAND - Lenh khong hop le!";
            }

            // Gửi Response về Client
            dos.writeUTF(response);
            dos.flush();
            System.out.println("<- Server da gui Response thanh cong.");

            clientSocket.close();
            System.out.println("Da dong ket noi voi Client.");
        } catch (IOException e) {
            System.err.println("Loi Server: " + e.getMessage());
        }
    }

    private static String handleCreateUser(String username) {
        File userFolder = new File(MAIL_DATA_DIR, username);

        if (userFolder.exists()) {
            return "CREATE_USER_FAIL - User [" + username + "] da ton tai!";
        }

        boolean createdDir = userFolder.mkdirs();
        if (!createdDir) {
            return "CREATE_USER_FAIL - Khong the tao thu muc cho user!";
        }

        File newEmailFile = new File(userFolder, "new_email.txt");
        try {
            boolean createdFile = newEmailFile.createNewFile();
            if (createdFile) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(newEmailFile))) {
                    writer.write("WELCOME TO MAIL SERVER SYSTEM!");
                    writer.newLine();
                }
                return "CREATE_USER_SUCCESS - Tai khoan [" + username + "] da duoc tao thanh cong!";
            } else {
                return "CREATE_USER_FAIL - Khong the tao file new_email.txt!";
            }
        } catch (IOException e) {
            return "CREATE_USER_FAIL - Loi File System: " + e.getMessage();
        }
    }

    private static String handleLogin(String username) {
        File userFolder = new File(MAIL_DATA_DIR, username);

        // 1. Kiểm tra thư mục tài khoản
        if (!userFolder.exists() || !userFolder.isDirectory()) {
            return "LOGIN_FAILED - Tai khoan [" + username + "] khong ton tai tren Server!";
        }

        // 2. Đọc tất cả các file trong thư mục user bằng listFiles()
        File[] files = userFolder.listFiles();
        StringBuilder sb = new StringBuilder();
        sb.append("LOGIN SUCCESS\n");
        sb.append("Danh sach email cua ").append(username).append(":\n");

        if (files == null || files.length == 0) {
            sb.append("(Thu muc trong, khong co email)");
        } else {
            int count = 1;
            for (File file : files) {
                if (file.isFile()) {
                    sb.append(count).append(". ").append(file.getName()).append("\n");
                    count++;
                }
            }
        }

        return sb.toString().trim();
    }

    private static String handleSendMail(String fromUser, String toUser, String body) {
        File recipientFolder = new File(MAIL_DATA_DIR, toUser);

        if (!recipientFolder.exists() || !recipientFolder.isDirectory()) {
            return "SEND_MAIL_FAIL - Nguoi nhan [" + toUser + "] khong ton tai tren Server!";
        }

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
                return "SEND_MAIL_SUCCESS - Da gui email den [" + toUser + "] (File: " + mailFile.getName() + ")!";
            } else {
                return "SEND_MAIL_FAIL - Khong the tao file email!";
            }
        } catch (IOException e) {
            return "SEND_MAIL_FAIL - Loi File System: " + e.getMessage();
        }
    }
}
