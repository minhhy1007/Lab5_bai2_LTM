package server;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MailServerGUI extends JFrame implements MailServer.LogListener {

    private JLabel lblStatus;
    private JTextField txtPort;
    private JLabel lblClients;
    private JTextArea txtLog;
    private JButton btnStart;
    private JButton btnStop;

    // UI Components - Tab 2: Registered Clients Management
    private DefaultListModel<String> userListModel;
    private JList<String> userList;
    private JLabel lblDetailUser;
    private JLabel lblDetailPass;
    private JLabel lblDetailDate;
    private JLabel lblDetailPath;
    
    private DefaultListModel<String> userFileListModel;
    private JList<String> userFileList;
    private JTextArea txtServerFileViewer;

    private String selectedUsername = null;

    // Color Palette
    private static final Color BG_COLOR = new Color(0xF5, 0xF7, 0xFA);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color PRIMARY_COLOR = new Color(0x25, 0x63, 0xEB);
    private static final Color SUCCESS_COLOR = new Color(0x16, 0xA3, 0x4A);
    private static final Color DANGER_COLOR = new Color(0xDC, 0x26, 0x26);
    private static final Color TEXT_COLOR = new Color(0x1F, 0x29, 0x37);
    private static final Color BORDER_COLOR = new Color(0xD1, 0xD5, 0xDB);

    public MailServerGUI() {
        super("Mail Server Dashboard & Registered Clients Manager");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 680);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(BG_COLOR);
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- HEADER ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BG_COLOR);
        JLabel titleLabel = new JLabel("MAIL SERVER SYSTEM CONTROL");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(TEXT_COLOR);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        lblStatus = new JLabel("● STOPPED");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblStatus.setForeground(DANGER_COLOR);
        headerPanel.add(lblStatus, BorderLayout.EAST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // --- TABBED PANE (Tab 1: Server Log, Tab 2: Registered Clients Detail Manager) ---
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        tabbedPane.addTab("  🖥️ SERVER DASHBOARD & LOG  ", createDashboardTab());
        tabbedPane.addTab("  👥 CLIENTS ĐÃ ĐĂNG KÝ & TẬP TIN  ", createClientsManagerTab());

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // --- FOOTER BUTTONS ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        buttonPanel.setBackground(BG_COLOR);

        btnStart = new JButton("START SERVER");
        styleButton(btnStart, PRIMARY_COLOR, Color.WHITE);
        btnStart.addActionListener(e -> startServer());

        btnStop = new JButton("STOP SERVER");
        styleButton(btnStop, DANGER_COLOR, Color.WHITE);
        btnStop.setEnabled(false);
        btnStop.addActionListener(e -> stopServer());

        buttonPanel.add(btnStart);
        buttonPanel.add(btnStop);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Reg log listener
        MailServer.addLogListener(this);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (MailServer.isRunning()) {
                    MailServer.stopServer();
                }
            }
        });

        // Tải danh sách Client ban đầu
        loadRegisteredClients();
    }

    // ==========================================
    // TAB 1: DASHBOARD & REALTIME LOG
    // ==========================================
    private JPanel createDashboardTab() {
        JPanel centerPanel = new JPanel(new BorderLayout(15, 15));
        centerPanel.setBackground(BG_COLOR);
        centerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Info Card
        JPanel infoCard = new JPanel(new GridLayout(3, 2, 10, 10));
        infoCard.setBackground(CARD_BG);
        infoCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(15, 15, 15, 15)
        ));

        infoCard.add(createLabel("Host / Loopback:"));
        JLabel lblHost = createLabel("localhost (127.0.0.1)");
        lblHost.setFont(new Font("Segoe UI", Font.BOLD, 13));
        infoCard.add(lblHost);

        infoCard.add(createLabel("Port TCP:"));
        txtPort = new JTextField("2345");
        txtPort.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        infoCard.add(txtPort);

        infoCard.add(createLabel("Connected Clients Count:"));
        lblClients = createLabel("0");
        lblClients.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblClients.setForeground(PRIMARY_COLOR);
        infoCard.add(lblClients);

        centerPanel.add(infoCard, BorderLayout.NORTH);

        // Server Log Card
        JPanel logCard = new JPanel(new BorderLayout(5, 5));
        logCard.setBackground(CARD_BG);
        logCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel logHeader = new JLabel("SERVER LOG (NHẬT KÝ HỆ THỐNG)");
        logHeader.setFont(new Font("Segoe UI", Font.BOLD, 14));
        logHeader.setForeground(TEXT_COLOR);
        logCard.add(logHeader, BorderLayout.NORTH);

        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtLog.setBackground(new Color(0xFA, 0xFA, 0xFA));
        JScrollPane scrollLog = new JScrollPane(txtLog);
        scrollLog.setBorder(new LineBorder(BORDER_COLOR, 1));
        logCard.add(scrollLog, BorderLayout.CENTER);

        centerPanel.add(logCard, BorderLayout.CENTER);

        return centerPanel;
    }

    // ==========================================
    // TAB 2: REGISTERED CLIENTS & FILE SYSTEM DETAILS
    // ==========================================
    private JPanel createClientsManagerTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG_COLOR);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top Refresh Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_COLOR);
        JLabel lblHeader = new JLabel("DANH SÁCH TÀI KHOẢN CLIENT ĐÃ ĐĂNG KÝ TRÊN SERVER");
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblHeader.setForeground(TEXT_COLOR);
        topBar.add(lblHeader, BorderLayout.WEST);

        JButton btnRefreshUsers = new JButton("Làm mới danh sách User");
        styleButton(btnRefreshUsers, SUCCESS_COLOR, Color.WHITE);
        btnRefreshUsers.addActionListener(e -> loadRegisteredClients());
        topBar.add(btnRefreshUsers, BorderLayout.EAST);

        panel.add(topBar, BorderLayout.NORTH);

        // Left Panel: User List
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFont(new Font("Segoe UI", Font.BOLD, 13));
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selUser = userList.getSelectedValue();
                if (selUser != null) {
                    showClientDetails(selUser);
                }
            }
        });

        JScrollPane scrollUserList = new JScrollPane(userList);
        scrollUserList.setBorder(new LineBorder(BORDER_COLOR, 1));

        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBackground(CARD_BG);
        leftPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel lblUsersListHeader = new JLabel("CÁC CLIENT (USER)");
        lblUsersListHeader.setFont(new Font("Segoe UI", Font.BOLD, 13));
        leftPanel.add(lblUsersListHeader, BorderLayout.NORTH);
        leftPanel.add(scrollUserList, BorderLayout.CENTER);

        // Right Panel: Details (Username, Password, Date/Time, File List & Content Viewer)
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBackground(BG_COLOR);

        // Card 1: Client Account Info
        JPanel accountInfoCard = new JPanel(new GridLayout(4, 2, 8, 8));
        accountInfoCard.setBackground(CARD_BG);
        accountInfoCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(12, 15, 12, 15)
        ));

        accountInfoCard.add(createLabel("Username:"));
        lblDetailUser = createLabel("(Chọn User bên trái)");
        lblDetailUser.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblDetailUser.setForeground(PRIMARY_COLOR);
        accountInfoCard.add(lblDetailUser);

        accountInfoCard.add(createLabel("Mật Khẩu (Password):"));
        lblDetailPass = createLabel("-");
        lblDetailPass.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblDetailPass.setForeground(DANGER_COLOR);
        accountInfoCard.add(lblDetailPass);

        accountInfoCard.add(createLabel("Ngày Giờ Tạo Tài Khoản:"));
        lblDetailDate = createLabel("-");
        lblDetailDate.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        accountInfoCard.add(lblDetailDate);

        accountInfoCard.add(createLabel("Thư Mục Đĩa Server:"));
        lblDetailPath = createLabel("-");
        lblDetailPath.setFont(new Font("Consolas", Font.PLAIN, 12));
        accountInfoCard.add(lblDetailPath);

        rightPanel.add(accountInfoCard, BorderLayout.NORTH);

        // Card 2: Files in User Directory & Content Viewer (SplitPane)
        userFileListModel = new DefaultListModel<>();
        userFileList = new JList<>(userFileListModel);
        userFileList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        userFileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userFileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedFile = userFileList.getSelectedValue();
                if (selectedFile != null && selectedUsername != null) {
                    showServerFileContent(selectedUsername, selectedFile);
                }
            }
        });

        JScrollPane scrollFileList = new JScrollPane(userFileList);
        scrollFileList.setBorder(new LineBorder(BORDER_COLOR, 1));

        JPanel userFilePanel = new JPanel(new BorderLayout(5, 5));
        userFilePanel.setBackground(CARD_BG);
        userFilePanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        JLabel lblUserFilesHeader = new JLabel("CÁC TẬP TIN MAIL");
        lblUserFilesHeader.setFont(new Font("Segoe UI", Font.BOLD, 13));
        userFilePanel.add(lblUserFilesHeader, BorderLayout.NORTH);
        userFilePanel.add(scrollFileList, BorderLayout.CENTER);

        // File Content Viewer
        txtServerFileViewer = new JTextArea();
        txtServerFileViewer.setEditable(false);
        txtServerFileViewer.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtServerFileViewer.setBackground(new Color(0xFA, 0xFA, 0xFA));
        JScrollPane scrollFileViewer = new JScrollPane(txtServerFileViewer);
        scrollFileViewer.setBorder(new LineBorder(BORDER_COLOR, 1));

        JPanel viewerPanel = new JPanel(new BorderLayout(5, 5));
        viewerPanel.setBackground(CARD_BG);
        viewerPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        JLabel lblViewerHeader = new JLabel("CHI TIẾT NỘI DUNG TẬP TIN DỰ ÁN");
        lblViewerHeader.setFont(new Font("Segoe UI", Font.BOLD, 13));
        viewerPanel.add(lblViewerHeader, BorderLayout.NORTH);
        viewerPanel.add(scrollFileViewer, BorderLayout.CENTER);

        JSplitPane detailSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, userFilePanel, viewerPanel);
        detailSplitPane.setDividerLocation(180);
        detailSplitPane.setResizeWeight(0.25);

        rightPanel.add(detailSplitPane, BorderLayout.CENTER);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        mainSplitPane.setDividerLocation(200);
        mainSplitPane.setResizeWeight(0.2);

        panel.add(mainSplitPane, BorderLayout.CENTER);

        return panel;
    }

    // --- TẢI DANH SÁCH CLIENT ĐÃ ĐĂNG KÝ TỪ HỆ THỐNG FILE SYSTEM SERVER ---
    public void loadRegisteredClients() {
        userListModel.clear();
        File mailDataDir = new File(MailServer.MAIL_DATA_DIR);
        if (mailDataDir.exists() && mailDataDir.isDirectory()) {
            File[] userFolders = mailDataDir.listFiles(File::isDirectory);
            if (userFolders != null) {
                for (File folder : userFolders) {
                    userListModel.addElement(folder.getName());
                }
            }
        }
    }

    // --- HIỂN THỊ CHI TIẾT CỦA SẢN PHẨM CLIENT ĐƯỢC CHỌN ---
    private void showClientDetails(String username) {
        this.selectedUsername = username;
        File userFolder = new File(MailServer.MAIL_DATA_DIR, username);

        lblDetailUser.setText(username);
        lblDetailPath.setText("mail_data/" + username + "/");

        // Đọc Mật khẩu từ password.txt & Ngày giờ tạo
        File passFile = new File(userFolder, "password.txt");
        if (passFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(passFile))) {
                String pass = reader.readLine();
                lblDetailPass.setText(pass != null ? pass.trim() : "(Chưa thiết lập)");
            } catch (IOException e) {
                lblDetailPass.setText("(Lỗi đọc mật khẩu)");
            }

            // Ngày giờ tạo/sửa file password.txt
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            lblDetailDate.setText(sdf.format(new Date(passFile.lastModified())));
        } else {
            lblDetailPass.setText("(Không có password.txt)");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            lblDetailDate.setText(sdf.format(new Date(userFolder.lastModified())));
        }

        // Đọc danh sách file trong thư mục của User này
        userFileListModel.clear();
        txtServerFileViewer.setText("");
        File[] files = userFolder.listFiles(File::isFile);
        if (files != null) {
            for (File file : files) {
                userFileListModel.addElement(file.getName());
            }
        }
    }

    // --- ĐỌC NỘI DUNG TẬP TIN DÀNH CHO SERVER VIEWER ---
    private void showServerFileContent(String username, String fileName) {
        File userFolder = new File(MailServer.MAIL_DATA_DIR, username);
        File mailFile = new File(userFolder, fileName);
        if (!mailFile.exists() || !mailFile.isFile()) {
            txtServerFileViewer.setText("File không tồn tại trên Server!");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("====================================================\n");
        sb.append("🖥️ SERVER SYSTEM FILE INSPECTOR\n");
        sb.append("• Client Sở Hữu: ").append(username).append("\n");
        sb.append("• Tên Tập Tin: ").append(fileName).append("\n");
        sb.append("• Ngày Giờ Cập Nhật File: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(mailFile.lastModified()))).append("\n");
        sb.append("• Đường Dẫn Đĩa Thật: ").append(mailFile.getAbsolutePath()).append("\n");
        sb.append("====================================================\n\n");

        try (BufferedReader reader = new BufferedReader(new FileReader(mailFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            sb.append("Lỗi đọc file: ").append(e.getMessage());
        }

        txtServerFileViewer.setText(sb.toString());
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

    private void startServer() {
        try {
            int port = Integer.parseInt(txtPort.getText().trim());
            MailServer.startServer(port);
            lblStatus.setText("● RUNNING");
            lblStatus.setForeground(SUCCESS_COLOR);
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
            txtPort.setEnabled(false);
            loadRegisteredClients();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Port khong hop le!", "Loi", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Khong the khoi tao Server: " + e.getMessage(), "Loi Socket", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopServer() {
        MailServer.stopServer();
        lblStatus.setText("● STOPPED");
        lblStatus.setForeground(DANGER_COLOR);
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        txtPort.setEnabled(true);
    }

    @Override
    public void onLog(String message) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(message + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
            // Tự động làm mới danh sách Client khi có tài khoản mới đăng ký
            if (message.contains("REGISTER") || message.contains("Account created")) {
                loadRegisteredClients();
            }
        });
    }

    @Override
    public void onClientCountChanged(int count) {
        SwingUtilities.invokeLater(() -> lblClients.setText(String.valueOf(count)));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new MailServerGUI().setVisible(true);
        });
    }
}
