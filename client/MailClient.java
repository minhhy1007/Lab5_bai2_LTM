package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class MailClient {
    public static void main(String[] args) {
        String serverAddress = "localhost";
        int port = 2345;

        System.out.println("=== MAIL CLIENT (PHASE 6 - LIST EMAILS) ===");
        System.out.println("Dang ket noi den Server tai " + serverAddress + ":" + port + "...");

        try (Socket socket = new Socket(serverAddress, port);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("-> Da ket noi thanh cong den Server Mail!");

            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());

            System.out.println("Chon chuc nang:");
            System.out.println(" 1. CREATE_USER (Tao tai khoan)");
            System.out.println(" 2. LOGIN       (Dang nhap & lay danh sach email)");
            System.out.println(" 3. SEND_MAIL   (Gui email)");
            System.out.print("Nhap lua chon (1, 2 hoac 3): ");
            String choice = scanner.nextLine().trim();

            if (choice.equals("1")) {
                System.out.print("Nhap username moi: ");
                String username = scanner.nextLine().trim();
                dos.writeUTF("CREATE_USER " + username);
                dos.flush();
            } else if (choice.equals("2")) {
                System.out.print("Nhap username dang nhap: ");
                String username = scanner.nextLine().trim();
                dos.writeUTF("LOGIN " + username);
                dos.flush();
            } else if (choice.equals("3")) {
                dos.writeUTF("SEND_MAIL");

                System.out.print("FROM (Nguoi gui): ");
                String fromUser = scanner.nextLine().trim();

                System.out.print("TO (Nguoi nhan): ");
                String toUser = scanner.nextLine().trim();

                System.out.print("BODY (Noi dung email): ");
                String body = scanner.nextLine().trim();

                dos.writeUTF(fromUser);
                dos.writeUTF(toUser);
                dos.writeUTF(body);
                dos.flush();
            } else {
                dos.writeUTF(choice);
                dos.flush();
            }

            // Nhận Response phản hồi từ Server (Bao gồm danh sách các file email)
            String response = dis.readUTF();
            System.out.println("\n=== PHAN HOI TU SERVER ===");
            System.out.println(response);

        } catch (IOException e) {
            System.err.println("Loi Client: " + e.getMessage());
        }
    }
}
