package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class MailClientGUI extends JFrame {

    private String serverHost = "localhost";
    private int serverPort = 2345;
    private String currentUser = null;

    // CardLayout Navigation
    private final CardLayout cardLayout;
    private final JPanel cardsPanel;

    // Card Names
    private static final String CARD_CONNECT = "CONNECT";
    private static final String CARD_AUTH = "AUTH";
    private static final String CARD_MAILBOX = "MAILBOX";

    // UI Components - Connect Panel
    private JTextField txtHost;
    private JTextField txtPort;
    private JLabel lblConnectStatus;
    private JButton btnConnect;

    // UI Components - Auth Panel (Mật khẩu dạng text bình thường 123)
    private JTabbedPane authTabPane;
    private JTextField txtLoginUser;
    private JTextField txtLoginPass; // JTextField thuần - Hiện 123 không cần mã hóa dot
    private JTextField txtRegUser;
    private JTextField txtRegPass;   // JTextField thuần - Hiện 123 không cần mã hóa dot
    private JTextField txtRegConfirmPass; // JTextField thuần

    // UI Components - Mailbox Panel
    private JLabel lblUserHeader;
    private DefaultListModel<String> emailListModel;
    private JList<String> emailList;
    private JTextArea txtEmailViewer;
    private JLabel lblSelectedFileName;
    private JLabel lblActionStatus; // Hiển thị thông báo trạng thái tạo file / gửi cho từng client

    // Theme Colors
    private static final Color BG_COLOR = new Color(0xF5, 0xF7, 0xFA);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color PRIMARY_COLOR = new Color(0x25, 0x63, 0xEB);
    private static final Color SUCCESS_COLOR = new Color(0x16, 0xA3, 0x4A);
    private static final Color DANGER_COLOR = new Color(0xDC, 0x26, 0x26);
    private static final Color TEXT_COLOR = new Color(0x1F, 0x29, 0x37);
    private static final Color BORDER_COLOR = new Color(0xD1, 0xD5, 0xDB);

    public MailClientGUI() {
        super("Mail Client - TCP Socket Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 640);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);
        cardsPanel.setBackground(BG_COLOR);

        // Build Cards
        cardsPanel.add(createConnectPanel(), CARD_CONNECT);
        cardsPanel.add(createAuthPanel(), CARD_AUTH);
        cardsPanel.add(createMailboxPanel(), CARD_MAILBOX);

        add(cardsPanel);

        // State 1: DISCONNECTED
        cardLayout.show(cardsPanel, CARD_CONNECT);
    }

    // ==========================================
    // 1. CONNECT PANEL (State 1 - Disconnected)
    // ==========================================
    private JPanel createConnectPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(BG_COLOR);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(25, 35, 25, 35)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        JLabel title = new JLabel("CONNECT TO MAIL SERVER", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(TEXT_COLOR);
        card.add(title, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        card.add(createLabel("Host / Server IP:"), gbc);

        gbc.gridx = 1;
        txtHost = new JTextField("localhost", 18);
        txtHost.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        card.add(txtHost, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        card.add(createLabel("Port:"), gbc);

        gbc.gridx = 1;
        txtPort = new JTextField("2345", 18);
        txtPort.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        card.add(txtPort, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        btnConnect = new JButton("CONNECT TO SERVER");
        styleButton(btnConnect, PRIMARY_COLOR, Color.WHITE);
        btnConnect.addActionListener(e -> handleConnect());
        card.add(btnConnect, gbc);

        gbc.gridy++;
        lblConnectStatus = new JLabel("Status: ● DISCONNECTED", SwingConstants.CENTER);
        lblConnectStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblConnectStatus.setForeground(DANGER_COLOR);
        card.add(lblConnectStatus, gbc);

        outer.add(card);
        return outer;
    }

    private void handleConnect() {
        serverHost = txtHost.getText().trim();
        String portStr = txtPort.getText().trim();
        if (serverHost.isEmpty() || portStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui long nhap Host va Port!", "Loi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            serverPort = Integer.parseInt(portStr);
            try (Socket socket = new Socket(serverHost, serverPort)) {
                lblConnectStatus.setText("Status: ● CONNECTED");
                lblConnectStatus.setForeground(SUCCESS_COLOR);
                
                JOptionPane.showMessageDialog(this, "Ket noi Server thanh cong!", "Thong bao", JOptionPane.INFORMATION_MESSAGE);
                cardLayout.show(cardsPanel, CARD_AUTH);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Port phai la so!", "Loi", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            lblConnectStatus.setText("Status: ● DISCONNECTED");
            lblConnectStatus.setForeground(DANGER_COLOR);
            JOptionPane.showMessageDialog(this, "Connection failed. Please check Server status and port.", "Loi Ket Noi Socket", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==========================================
    // 2. AUTH PANEL (State 2 - Connected / Login & Register)
    // ==========================================
    private JPanel createAuthPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(BG_COLOR);

        authTabPane = new JTabbedPane();
        authTabPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        authTabPane.setBackground(CARD_BG);

        authTabPane.addTab("  LOGIN  ", createLoginTab());
        authTabPane.addTab("  REGISTER  ", createRegisterTab());

        outer.add(authTabPane);
        return outer;
    }

    private JPanel createLoginTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(CARD_BG);
        panel.setBorder(new EmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        JLabel title = new JLabel("USER LOGIN", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_COLOR);
        panel.add(title, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        panel.add(createLabel("Username:"), gbc);

        gbc.gridx = 1;
        txtLoginUser = new JTextField(16);
        panel.add(txtLoginUser, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(createLabel("Password (Chữ thường):"), gbc);

        gbc.gridx = 1;
        // HIỂN THỊ MẶC ĐỊNH MẮT THƯỜNG (DẠNG TEXT THƯỜNG 123)
        txtLoginPass = new JTextField(16);
        panel.add(txtLoginPass, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        JButton btnLogin = new JButton("LOGIN");
        styleButton(btnLogin, PRIMARY_COLOR, Color.WHITE);
        btnLogin.addActionListener(e -> handleLogin());
        panel.add(btnLogin, gbc);

        return panel;
    }

    private JPanel createRegisterTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(CARD_BG);
        panel.setBorder(new EmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        JLabel title = new JLabel("CREATE ACCOUNT", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_COLOR);
        panel.add(title, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        panel.add(createLabel("Username:"), gbc);

        gbc.gridx = 1;
        txtRegUser = new JTextField(16);
        panel.add(txtRegUser, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(createLabel("Password:"), gbc);

        gbc.gridx = 1;
        // HIỂN THỊ DẠNG TEXT BÌNH THƯỜNG (XEM MẬT KHẨU TRỰC TIẾP)
        txtRegPass = new JTextField(16);
        panel.add(txtRegPass, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(createLabel("Confirm Password:"), gbc);

        gbc.gridx = 1;
        txtRegConfirmPass = new JTextField(16);
        panel.add(txtRegConfirmPass, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        JButton btnRegister = new JButton("CREATE ACCOUNT");
        styleButton(btnRegister, SUCCESS_COLOR, Color.WHITE);
        btnRegister.addActionListener(e -> handleRegister());
        panel.add(btnRegister, gbc);

        return panel;
    }

    private void handleLogin() {
        String username = txtLoginUser.getText().trim();
        String password = txtLoginPass.getText().trim(); // Lấy chữ thường

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Password cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String response = sendTcpRequest("LOGIN " + username + " " + password);
        if (response == null) return;

        if (response.startsWith("LOGIN SUCCESS")) {
            currentUser = username;
            lblUserHeader.setText("User: " + currentUser);
            
            updateMailboxListFromResponse(response);
            lblActionStatus.setText("Trạng thái: Đã đăng nhập tài khoản [" + currentUser + "]. Đường dẫn Server: mail_data/" + currentUser + "/");

            cardLayout.show(cardsPanel, CARD_MAILBOX);
            JOptionPane.showMessageDialog(this, "Login successful! Welcome " + currentUser, "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, response, "Login Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRegister() {
        String username = txtRegUser.getText().trim();
        String password = txtRegPass.getText().trim();
        String confirmPass = txtRegConfirmPass.getText().trim();

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Password cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!password.equals(confirmPass)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String response = sendTcpRequest("CREATE_USER " + username + " " + password);
        if (response == null) return;

        if (response.startsWith("CREATE_USER_SUCCESS")) {
            JOptionPane.showMessageDialog(this, "Tạo tài khoản thành công!\nĐã sinh thư mục Server: mail_data/" + username + "/\nĐã sinh file: new_email.txt & password.txt", "Tạo Tài Khoản Thành Công", JOptionPane.INFORMATION_MESSAGE);
            txtLoginUser.setText(username);
            txtLoginPass.setText(password);
            authTabPane.setSelectedIndex(0);
        } else {
            JOptionPane.showMessageDialog(this, response, "Registration Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==========================================
    // 3. MAILBOX PANEL (State 3 - Logged In)
    // ==========================================
    private JPanel createMailboxPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_COLOR);

        // Header Bar
        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setBackground(CARD_BG);
        headerBar.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1),
                new EmptyBorder(10, 20, 10, 20)
        ));

        lblUserHeader = new JLabel("User: ");
        lblUserHeader.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblUserHeader.setForeground(TEXT_COLOR);

        JLabel lblOnline = new JLabel("  Status: ● CONNECTED");
        lblOnline.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblOnline.setForeground(SUCCESS_COLOR);

        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftHeader.setBackground(CARD_BG);
        leftHeader.add(lblUserHeader);
        leftHeader.add(lblOnline);

        headerBar.add(leftHeader, BorderLayout.WEST);

        JButton btnLogout = new JButton("Logout");
        styleButton(btnLogout, DANGER_COLOR, Color.WHITE);
        btnLogout.addActionListener(e -> handleLogout());
        headerBar.add(btnLogout, BorderLayout.EAST);

        panel.add(headerBar, BorderLayout.NORTH);

        // Main Content (SplitPane: Left Mail List, Right Email Content Viewer)
        emailListModel = new DefaultListModel<>();
        emailList = new JList<>(emailListModel);
        emailList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        emailList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        emailList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedFile = emailList.getSelectedValue();
                if (selectedFile != null) {
                    String cleanFileName = cleanFileName(selectedFile);
                    readEmailContent(cleanFileName);
                }
            }
        });

        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBackground(CARD_BG);
        leftPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JLabel lblInboxHeader = new JLabel("INBOX MAILBOX");
        lblInboxHeader.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblInboxHeader.setForeground(TEXT_COLOR);
        leftPanel.add(lblInboxHeader, BorderLayout.NORTH);

        JScrollPane scrollList = new JScrollPane(emailList);
        scrollList.setBorder(new LineBorder(BORDER_COLOR, 1));
        leftPanel.add(scrollList, BorderLayout.CENTER);

        // Right Viewer Panel
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setBackground(CARD_BG);
        rightPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        lblSelectedFileName = new JLabel("EMAIL CONTENT (Select an email)");
        lblSelectedFileName.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblSelectedFileName.setForeground(TEXT_COLOR);
        rightPanel.add(lblSelectedFileName, BorderLayout.NORTH);

        txtEmailViewer = new JTextArea();
        txtEmailViewer.setEditable(false);
        txtEmailViewer.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtEmailViewer.setBackground(new Color(0xFA, 0xFA, 0xFA));
        JScrollPane scrollViewer = new JScrollPane(txtEmailViewer);
        scrollViewer.setBorder(new LineBorder(BORDER_COLOR, 1));
        rightPanel.add(scrollViewer, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(250);
        splitPane.setResizeWeight(0.3);

        panel.add(splitPane, BorderLayout.CENTER);

        // Bottom Container (Action Buttons & Status Bar)
        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.setBackground(BG_COLOR);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        bottomBar.setBackground(BG_COLOR);

        JButton btnCompose = new JButton("Compose Email");
        styleButton(btnCompose, PRIMARY_COLOR, Color.WHITE);
        btnCompose.addActionListener(e -> openComposeDialog());

        JButton btnRead = new JButton("Read Email");
        styleButton(btnRead, PRIMARY_COLOR, Color.WHITE);
        btnRead.addActionListener(e -> {
            String sel = emailList.getSelectedValue();
            if (sel == null) {
                JOptionPane.showMessageDialog(this, "Vui long chon file email trong danh sach!", "Thong bao", JOptionPane.INFORMATION_MESSAGE);
            } else {
                readEmailContent(cleanFileName(sel));
            }
        });

        JButton btnRefresh = new JButton("Refresh Mailbox");
        styleButton(btnRefresh, SUCCESS_COLOR, Color.WHITE);
        btnRefresh.addActionListener(e -> refreshMailbox());

        bottomBar.add(btnCompose);
        bottomBar.add(btnRead);
        bottomBar.add(btnRefresh);

        bottomContainer.add(bottomBar, BorderLayout.NORTH);

        // Status Notification Label (Hiển thị chi tiết file được tạo / đọc / gửi cho từng client)
        lblActionStatus = new JLabel("Trạng thái: Sẵn sàng");
        lblActionStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblActionStatus.setForeground(PRIMARY_COLOR);
        lblActionStatus.setBorder(new EmptyBorder(0, 15, 8, 15));
        bottomContainer.add(lblActionStatus, BorderLayout.SOUTH);

        panel.add(bottomContainer, BorderLayout.SOUTH);

        return panel;
    }

    private String cleanFileName(String displayStr) {
        if (displayStr == null) return "";
        if (displayStr.contains(". ")) {
            return displayStr.substring(displayStr.indexOf(". ") + 2).trim();
        }
        return displayStr.trim();
    }

    private void refreshMailbox() {
        if (currentUser == null) return;
        String response = sendTcpRequest("LOGIN " + currentUser + " default");
        if (response != null && response.startsWith("LOGIN SUCCESS")) {
            updateMailboxListFromResponse(response);
            lblActionStatus.setText("Trạng thái: Đã làm mới danh sách file email trong mail_data/" + currentUser + "/");
            JOptionPane.showMessageDialog(this, "Mailbox refreshed successfully!", "Refresh", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void updateMailboxListFromResponse(String response) {
        emailListModel.clear();
        String[] lines = response.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("LOGIN SUCCESS") || line.startsWith("Danh sach email")) {
                continue;
            }
            emailListModel.addElement(line);
        }
    }

    private void readEmailContent(String fileName) {
        if (currentUser == null || fileName.isEmpty()) return;
        lblSelectedFileName.setText("File Email: " + fileName + " (Thư mục Server: mail_data/" + currentUser + "/)");
        String response = sendTcpRequest("READ_MAIL " + currentUser + " " + fileName);
        if (response != null && response.startsWith("READ_MAIL_SUCCESS")) {
            String content = response.substring("READ_MAIL_SUCCESS".length()).trim();
            
            boolean isSentMail = fileName.startsWith("sent_");
            StringBuilder formatted = new StringBuilder();
            formatted.append("====================================================\n");
            if (isSentMail) {
                formatted.append("📤 LỊCH SỬ THƯ ĐÃ GỬI (SENT MAIL)\n");
            } else {
                formatted.append("📥 THƯ ĐẾN TRONG HÒM THƯ (INBOX)\n");
            }
            formatted.append("• Tên Tập Tin: ").append(fileName).append("\n");
            formatted.append("• Hòm Thư Tài Khoản: ").append(currentUser).append("\n");
            formatted.append("• Đường Dẫn File Server: mail_data/").append(currentUser).append("/").append(fileName).append("\n");
            formatted.append("====================================================\n\n");
            formatted.append(content);

            txtEmailViewer.setText(formatted.toString());
            if (isSentMail) {
                lblActionStatus.setText("Trạng thái: Đang xem lịch sử thư đã gửi [" + fileName + "].");
            } else {
                lblActionStatus.setText("Trạng thái: Đã tải nội dung file [" + fileName + "] từ Server.");
            }
        } else {
            txtEmailViewer.setText("Không thể đọc file email: " + response);
        }
    }

    private void openComposeDialog() {
        JDialog composeDialog = new JDialog(this, "Soạn Thư & Gửi Cho Client Khác", true);
        composeDialog.setSize(520, 440);
        composeDialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(CARD_BG);
        panel.setBorder(new EmptyBorder(15, 20, 15, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        panel.add(createLabel("FROM (Người gửi):"), gbc);
        gbc.gridx = 1;
        JTextField txtFrom = new JTextField(currentUser);
        txtFrom.setEditable(false);
        panel.add(txtFrom, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(createLabel("TO (Client nhận):"), gbc);
        gbc.gridx = 1;
        JTextField txtTo = new JTextField();
        panel.add(txtTo, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(createLabel("Message Body (Nội dung):"), gbc);
        gbc.gridx = 1;
        JTextArea txtBody = new JTextArea(8, 25);
        txtBody.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane scrollBody = new JScrollPane(txtBody);
        panel.add(scrollBody, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        JPanel btnPane = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPane.setBackground(CARD_BG);

        JButton btnSend = new JButton("SEND EMAIL");
        styleButton(btnSend, PRIMARY_COLOR, Color.WHITE);
        btnSend.addActionListener(e -> {
            String toUser = txtTo.getText().trim();
            String body = txtBody.getText().trim();

            if (toUser.isEmpty()) {
                JOptionPane.showMessageDialog(composeDialog, "Recipient cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (body.isEmpty()) {
                JOptionPane.showMessageDialog(composeDialog, "Message cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (Socket socket = new Socket(serverHost, serverPort);
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                 DataInputStream dis = new DataInputStream(socket.getInputStream())) {

                dos.writeUTF("SEND_MAIL");
                dos.writeUTF(currentUser);
                dos.writeUTF(toUser);
                dos.writeUTF(body);
                dos.flush();

                String res = dis.readUTF();
                if (res.startsWith("SEND_MAIL_SUCCESS")) {
                    String msg = "Gửi email thành công từ [" + currentUser + "] đến client [" + toUser + "]!\n" +
                                 "Server đã sinh file thư mới trong thư mục: mail_data/" + toUser + "/";
                    JOptionPane.showMessageDialog(composeDialog, msg, "Gửi Mail Thành Công", JOptionPane.INFORMATION_MESSAGE);
                    lblActionStatus.setText("Trạng thái: Đã gửi mail từ [" + currentUser + "] đến [" + toUser + "]. File được tạo trên Server.");
                    composeDialog.dispose();
                    refreshMailbox();
                } else {
                    JOptionPane.showMessageDialog(composeDialog, res, "Send Mail Error", JOptionPane.ERROR_MESSAGE);
                }

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(composeDialog, "Loi Socket khi gui mail: " + ex.getMessage(), "Loi Network", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnCancel = new JButton("Cancel");
        styleButton(btnCancel, DANGER_COLOR, Color.WHITE);
        btnCancel.addActionListener(e -> composeDialog.dispose());

        btnPane.add(btnSend);
        btnPane.add(btnCancel);
        panel.add(btnPane, gbc);

        composeDialog.add(panel);
        composeDialog.setVisible(true);
    }

    private void handleLogout() {
        currentUser = null;
        txtEmailViewer.setText("");
        lblSelectedFileName.setText("EMAIL CONTENT (Select an email)");
        emailListModel.clear();
        
        cardLayout.show(cardsPanel, CARD_AUTH);
        JOptionPane.showMessageDialog(this, "Logged out successfully.", "Logout", JOptionPane.INFORMATION_MESSAGE);
    }

    private String sendTcpRequest(String request) {
        try (Socket socket = new Socket(serverHost, serverPort);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeUTF(request);
            dos.flush();

            return dis.readUTF();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Loi ket noi Server TCP: " + e.getMessage() + "\nVui long kiem tra Server dang chay!", "Loi Socket", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(TEXT_COLOR);
        return lbl;
    }

    private void styleButton(JButton btn, Color bgColor, Color fgColor) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bgColor);
        btn.setForeground(fgColor);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new MailClientGUI().setVisible(true);
        });
    }
}
