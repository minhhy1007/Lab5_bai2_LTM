package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * GIAO DIỆN CHAT NET SEND UDP & MULTICAST - BỐ CỤC CHUẨN ZALO / MESSENGER
 * - Ô tin nhắn co giãn vừa khít nội dung (không bị tràn toàn màn hình).
 * - Phân chia tin nhắn rõ ràng: Tin của mình (bên phải, nền xanh), tin nhóm/người khác (bên trái, nền trắng có tên người gửi).
 * - Loại bỏ toàn bộ icon lỗi font vuông [], giao diện trực quan, sang trọng.
 */
public class NetSendChatGUI extends JFrame {

    private int listenPort = 9876;
    private String defaultMulticastGroup = "225.1.2.3";
    private MulticastSocket socket;
    private Thread receiverThread;
    private volatile boolean isListening = false;
    private String myLocalIP = "127.0.0.1";

    private final Set<String> joinedGroups = new HashSet<>();

    public enum MsgType {
        UNICAST, MULTICAST, BROADCAST, SYSTEM
    }

    public static class ChatMessage {
        public final String sender;
        public final String target;
        public final String content;
        public final MsgType type;
        public final boolean isMe;
        public final String time;

        public ChatMessage(String sender, String target, String content, MsgType type, boolean isMe) {
            this.sender = sender;
            this.target = target;
            this.content = content;
            this.type = type;
            this.isMe = isMe;
            this.time = new SimpleDateFormat("HH:mm").format(new Date());
        }
    }

    private final List<ChatMessage> allMessages = new CopyOnWriteArrayList<>();

    public static class ChatChannel {
        public final String id;
        public final String name;
        public final String targetAddress;
        public final MsgType type;
        public final String tag;
        public int unreadCount = 0;

        public ChatChannel(String id, String name, String targetAddress, MsgType type, String tag) {
            this.id = id;
            this.name = name;
            this.targetAddress = targetAddress;
            this.type = type;
            this.tag = tag;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final DefaultListModel<ChatChannel> channelListModel = new DefaultListModel<>();
    private JList<ChatChannel> channelList;
    private ChatChannel currentChannel;

    // UI Controls
    private JLabel lblCurrentTitle;
    private JLabel lblTargetBadge;
    private JLabel lblNetStatus;
    private JTextPane chatTextPane;
    private JTextField txtInputMessage;
    private JTextField txtRawCommand;
    private JTextArea txtActivityLog;

    // Bảng màu chuẩn hiện đại
    private static final Color COLOR_PRIMARY = new Color(0x25, 0x63, 0xEB);      // Xanh dương Messenger (#2563EB)
    private static final Color COLOR_SIDEBAR = new Color(0x0F, 0x17, 0x2A);      // Slate Dark (#0F172A)
    private static final Color COLOR_SIDEBAR_ACTIVE = new Color(0x3B, 0x82, 0xF6);
    private static final Color COLOR_BG_CHAT = new Color(0xF1, 0xF5, 0xF9);      // Nền chat xám sạch (#F1F5F9)
    private static final Color COLOR_BORDER = new Color(0xCB, 0xD5, 0xE1);
    private static final Color COLOR_TEXT_DARK = new Color(0x0F, 0x17, 0x2A);
    private static final Color COLOR_TEXT_MUTED = new Color(0x64, 0x74, 0x8B);

    public NetSendChatGUI() {
        super("Chương Trình Net Send Java Multicast & UDP");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 680);
        setMinimumSize(new Dimension(860, 560));
        setLocationRelativeTo(null);

        try {
            myLocalIP = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignored) {
            myLocalIP = "127.0.0.1";
        }

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(COLOR_BG_CHAT);

        root.add(createTopBar(), BorderLayout.NORTH);
        root.add(createMainSplit(), BorderLayout.CENTER);
        root.add(createBottomCommandBar(), BorderLayout.SOUTH);

        add(root);

        initDefaultChannels();
        startNetworkListener();
    }

    // =========================================================================
    // 1. THANH TRÊN CÙNG: RÕ RÀNG, KHÔNG LỖI FONT
    // =========================================================================
    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER),
                new EmptyBorder(8, 16, 8, 16)
        ));

        // Trái: Tên app + IP Port
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setBackground(Color.WHITE);

        JLabel logo = new JLabel("NET SEND");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        logo.setForeground(COLOR_PRIMARY);

        lblNetStatus = new JLabel("● Đang lắng nghe");
        lblNetStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblNetStatus.setForeground(new Color(0x16, 0xA3, 0x4A));

        JLabel lblIP = new JLabel("|  IP máy này: " + myLocalIP + "  |  Port: " + listenPort);
        lblIP.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblIP.setForeground(COLOR_TEXT_MUTED);

        left.add(logo);
        left.add(lblNetStatus);
        left.add(lblIP);

        // Phải: 2 nút chức năng gọn gàng
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setBackground(Color.WHITE);

        JButton btnGroups = createStyledButton("Quản lý Nhóm", false);
        btnGroups.addActionListener(e -> openGroupManagerDialog());

        JButton btnLog = createStyledButton("Log Kỹ thuật", false);
        btnLog.addActionListener(e -> openLogDialog());

        right.add(btnGroups);
        right.add(btnLog);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // =========================================================================
    // 2. PHÂN CHIA 2 CỘT CHÍNH (SIDEBAR + KHUNG CHAT)
    // =========================================================================
    private JComponent createMainSplit() {
        JPanel sidebar = createSidebar();
        sidebar.setPreferredSize(new Dimension(250, 600));
        sidebar.setMinimumSize(new Dimension(200, 400));

        JPanel chatArea = createChatArea();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, chatArea);
        split.setDividerLocation(250);
        split.setDividerSize(3);
        split.setBorder(null);
        split.setResizeWeight(0.25);
        return split;
    }

    // --- CỘT TRÁI: DANH SÁCH KÊNH (TẤT CẢ / NHÓM / TÔI) ---
    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(COLOR_SIDEBAR);

        // Header nhỏ của sidebar
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_SIDEBAR);
        header.setBorder(new EmptyBorder(12, 14, 10, 14));

        JLabel title = new JLabel("DANH SÁCH HỘI THOẠI");
        title.setFont(new Font("Segoe UI", Font.BOLD, 11));
        title.setForeground(new Color(0x94, 0xA3, 0xB8));

        JButton btnAdd = new JButton("+ Thêm");
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setBackground(new Color(0x33, 0x41, 0x55));
        btnAdd.setFocusPainted(false);
        btnAdd.setBorder(new EmptyBorder(3, 8, 3, 8));
        btnAdd.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAdd.addActionListener(e -> openAddTargetDialog());

        header.add(title, BorderLayout.WEST);
        header.add(btnAdd, BorderLayout.EAST);
        sidebar.add(header, BorderLayout.NORTH);

        // Danh sách kênh
        channelList = new JList<>(channelListModel);
        channelList.setBackground(COLOR_SIDEBAR);
        channelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        channelList.setCellRenderer(new ChannelCellRenderer());
        channelList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ChatChannel selected = channelList.getSelectedValue();
                if (selected != null) {
                    switchChannel(selected);
                }
            }
        });

        JScrollPane scroll = new JScrollPane(channelList);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(COLOR_SIDEBAR);
        sidebar.add(scroll, BorderLayout.CENTER);

        return sidebar;
    }

    // --- CỘT PHẢI: KHUNG CHAT BONG BÓNG GỌN GÀNG ---
    private JPanel createChatArea() {
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBackground(COLOR_BG_CHAT);

        // Header kênh đang chọn
        JPanel head = new JPanel(new BorderLayout());
        head.setBackground(Color.WHITE);
        head.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER),
                new EmptyBorder(10, 18, 10, 18)
        ));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setBackground(Color.WHITE);

        lblCurrentTitle = new JLabel("Nhóm LTM");
        lblCurrentTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblCurrentTitle.setForeground(COLOR_TEXT_DARK);

        lblTargetBadge = new JLabel("(225.1.2.3)");
        lblTargetBadge.setFont(new Font("Consolas", Font.BOLD, 12));
        lblTargetBadge.setForeground(COLOR_PRIMARY);

        titlePanel.add(lblCurrentTitle);
        titlePanel.add(lblTargetBadge);

        JButton btnClear = createStyledButton("Xóa khung chat", false);
        btnClear.addActionListener(e -> clearCurrentChat());

        head.add(titlePanel, BorderLayout.WEST);
        head.add(btnClear, BorderLayout.EAST);
        chatPanel.add(head, BorderLayout.NORTH);

        // Khung hiển thị bong bóng chat (HTML Table Renderer: Chuẩn ôm khít tin nhắn)
        chatTextPane = new JTextPane();
        chatTextPane.setContentType("text/html");
        chatTextPane.setEditable(false);
        chatTextPane.setBackground(COLOR_BG_CHAT);
        chatTextPane.setBorder(new EmptyBorder(10, 15, 10, 15));

        JScrollPane chatScroll = new JScrollPane(chatTextPane);
        chatScroll.setBorder(null);
        chatScroll.getViewport().setBackground(COLOR_BG_CHAT);
        chatPanel.add(chatScroll, BorderLayout.CENTER);

        // Khung nhập tin nhắn tinh gọn
        chatPanel.add(createInputBar(), BorderLayout.SOUTH);

        return chatPanel;
    }

    // Ô NHẬP TIN NHẮN VÀ NÚT GỬI NỔI BẬT
    private JPanel createInputBar() {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_BORDER),
                new EmptyBorder(10, 16, 10, 16)
        ));

        txtInputMessage = new JTextField();
        txtInputMessage.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtInputMessage.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        txtInputMessage.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });

        // Nút GỬI vẽ màu xanh nổi bật chắc chắn không bị lỗi nền trắng
        JButton btnSend = new JButton("GỬI") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(new Color(0x1D, 0x4E, 0xD8));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(0x3B, 0x82, 0xF6));
                } else {
                    g2.setColor(COLOR_PRIMARY);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnSend.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSend.setForeground(Color.WHITE);
        btnSend.setContentAreaFilled(false);
        btnSend.setOpaque(false);
        btnSend.setFocusPainted(false);
        btnSend.setBorderPainted(false);
        btnSend.setBorder(new EmptyBorder(8, 24, 8, 24));
        btnSend.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSend.addActionListener(e -> sendMessage());

        row.add(txtInputMessage, BorderLayout.CENTER);
        row.add(btnSend, BorderLayout.EAST);
        return row;
    }

    // 3. THANH LỆNH THÔ GỌN GÀNG DƯỚI ĐÁY
    private JPanel createBottomCommandBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBackground(new Color(0xEE, 0xF2, 0xF6));
        bar.setBorder(new EmptyBorder(6, 16, 6, 16));

        JLabel lbl = new JLabel("Lệnh net send:");
        lbl.setFont(new Font("Consolas", Font.BOLD, 12));
        lbl.setForeground(COLOR_TEXT_MUTED);

        txtRawCommand = new JTextField("net send 225.1.2.3 Xin chao ca nhom!");
        txtRawCommand.setFont(new Font("Consolas", Font.PLAIN, 12));

        JButton btnRun = createStyledButton("Thực thi", false);
        btnRun.addActionListener(e -> executeRawCommand());

        bar.add(lbl, BorderLayout.WEST);
        bar.add(txtRawCommand, BorderLayout.CENTER);
        bar.add(btnRun, BorderLayout.EAST);
        return bar;
    }

    // =========================================================================
    // 4. DANH SÁCH KÊNH MẶC ĐỊNH
    // =========================================================================
    private void initDefaultChannels() {
        ChatChannel chGroup = new ChatChannel("group", "Nhóm LTM", defaultMulticastGroup, MsgType.MULTICAST, "[NHÓM]");
        ChatChannel chAll = new ChatChannel("all", "Tất cả (Broadcast)", "255.255.255.255", MsgType.BROADCAST, "[ALL]");
        ChatChannel chMe = new ChatChannel("me", "Tôi (Localhost)", "127.0.0.1", MsgType.UNICAST, "[1-1]");

        channelListModel.addElement(chGroup);
        channelListModel.addElement(chAll);
        channelListModel.addElement(chMe);

        channelList.setSelectedIndex(0);
        currentChannel = chGroup;
        renderChatHTML();
    }

    private void switchChannel(ChatChannel ch) {
        this.currentChannel = ch;
        ch.unreadCount = 0;
        channelList.repaint();

        lblCurrentTitle.setText(ch.name);
        lblTargetBadge.setText("(" + ch.targetAddress + ")");

        renderChatHTML();
        txtInputMessage.requestFocus();
    }

    // =========================================================================
    // 5. RENDER BONG BÓNG CHAT: CO GIÃN VỪA KHÍT NỘI DUNG (DÙNG HTML TABLE)
    // =========================================================================
    private void renderChatHTML() {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>")
            .append("body { font-family: 'Segoe UI', Tahoma, sans-serif; background-color: #F1F5F9; margin: 4px; }")
            .append("</style></head><body>");

        boolean hasMsg = false;
        for (ChatMessage msg : allMessages) {
            if (!shouldShowInChannel(msg, currentChannel)) continue;
            hasMsg = true;

            if (msg.type == MsgType.SYSTEM) {
                html.append("<table width='100%' border='0' cellspacing='0' cellpadding='0' style='margin: 8px 0;'>")
                    .append("<tr><td align='center'>")
                    .append("<font face='Segoe UI' size='2' color='#64748B'><i>• ").append(escapeHtml(msg.content)).append(" •</i></font>")
                    .append("</td></tr></table>");
                continue;
            }

            if (msg.isMe) {
                // --- BONG BÓNG CỦA TÔI (CĂN PHẢI, NỀN XANH DƯƠNG, CO GIÃN VỪA CHỮ) ---
                html.append("<table width='100%' border='0' cellspacing='0' cellpadding='0' style='margin-bottom: 8px;'>")
                    .append("<tr><td align='right'>")
                    .append("<table border='0' cellspacing='0' cellpadding='7' bgcolor='#2563EB' style='border: 1px solid #1D4ED8;'>")
                    .append("<tr><td>")
                    .append("<font face='Segoe UI' size='3' color='#FFFFFF'><b>").append(escapeHtml(msg.content)).append("</b></font>")
                    .append("&nbsp;&nbsp;&nbsp;&nbsp;")
                    .append("<font face='Segoe UI' size='1' color='#BFDBFE'>").append(msg.time).append("</font>")
                    .append("</td></tr></table>")
                    .append("</td></tr></table>");
            } else {
                // --- BONG BÓNG NGƯỜI KHÁC / NHÓM (CĂN TRÁI, NỀN TRẮNG, CÓ TÊN NGƯỜI GỬI) ---
                String badgeColor = "#0284C7"; // Mặc định xanh dương
                String badgeName = "[Cá nhân]";
                if (msg.type == MsgType.MULTICAST) {
                    badgeColor = "#D97706"; // Màu cam nhóm
                    badgeName = "[Nhóm " + msg.target + "]";
                } else if (msg.type == MsgType.BROADCAST) {
                    badgeColor = "#7C3AED"; // Màu tím Broadcast
                    badgeName = "[Tất cả]";
                }

                html.append("<table width='100%' border='0' cellspacing='0' cellpadding='0' style='margin-bottom: 8px;'>")
                    .append("<tr><td align='left'>")
                    .append("<div style='margin-bottom: 3px;'>")
                    .append("<font face='Segoe UI' size='2' color='#0F172A'><b>").append(msg.sender).append("</b></font>")
                    .append("&nbsp;")
                    .append("<font face='Segoe UI' size='1' color='").append(badgeColor).append("'><b>").append(badgeName).append("</b></font>")
                    .append("</div>")
                    .append("<table border='0' cellspacing='0' cellpadding='7' bgcolor='#FFFFFF' style='border: 1px solid #CBD5E1;'>")
                    .append("<tr><td>")
                    .append("<font face='Segoe UI' size='3' color='#0F172A'>").append(escapeHtml(msg.content)).append("</font>")
                    .append("&nbsp;&nbsp;&nbsp;&nbsp;")
                    .append("<font face='Segoe UI' size='1' color='#94A3B8'>").append(msg.time).append("</font>")
                    .append("</td></tr></table>")
                    .append("</td></tr></table>");
            }
        }

        if (!hasMsg) {
            html.append("<table width='100%' height='200' border='0'><tr><td align='center'>")
                .append("<font face='Segoe UI' size='2' color='#94A3B8'>Chưa có tin nhắn trong kênh này.<br/>Hãy nhập tin nhắn bên dưới và bấm <b>GỬI</b>!</font>")
                .append("</td></tr></table>");
        }

        html.append("</body></html>");
        chatTextPane.setText(html.toString());
        SwingUtilities.invokeLater(() -> chatTextPane.setCaretPosition(chatTextPane.getDocument().getLength()));
    }

    private boolean shouldShowInChannel(ChatMessage msg, ChatChannel ch) {
        if (ch == null || msg.type == MsgType.SYSTEM) return true;
        if (ch.type == MsgType.BROADCAST) return msg.type == MsgType.BROADCAST;
        if (ch.type == MsgType.MULTICAST) return msg.type == MsgType.MULTICAST && msg.target.equals(ch.targetAddress);
        return (msg.type == MsgType.UNICAST) && (msg.target.equals(ch.targetAddress) || msg.sender.equals(ch.targetAddress));
    }

    private void clearCurrentChat() {
        if (currentChannel == null) return;
        allMessages.removeIf(m -> shouldShowInChannel(m, currentChannel));
        renderChatHTML();
    }

    // =========================================================================
    // 6. TIẾN TRÌNH LẮNG NGHE MẠNG UDP & MULTICAST
    // =========================================================================
    private void startNetworkListener() {
        if (isListening) return;
        try {
            socket = new MulticastSocket(listenPort);
            isListening = true;

            joinMulticastGroup(defaultMulticastGroup);

            receiverThread = new Thread(() -> {
                byte[] buffer = new byte[4096];
                while (isListening && !socket.isClosed()) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);

                        String content = new String(packet.getData(), 0, packet.getLength(), "UTF-8").trim();
                        String senderIP = packet.getAddress().getHostAddress();

                        MsgType type = packet.getAddress().isMulticastAddress() ? MsgType.MULTICAST : MsgType.UNICAST;
                        String target = packet.getAddress().getHostAddress();

                        if (content.startsWith("[BROADCAST]") || packet.getAddress().toString().contains("255.255.255.255")) {
                            type = MsgType.BROADCAST;
                            content = content.replace("[BROADCAST]", "").trim();
                        }

                        handleIncoming(senderIP, target, content, type);
                    } catch (IOException e) {
                        if (!isListening) break;
                    }
                }
            });
            receiverThread.setDaemon(true);
            receiverThread.start();

        } catch (IOException ex) {
            lblNetStatus.setText("● Lỗi Port " + listenPort);
            lblNetStatus.setForeground(Color.RED);
        }
    }

    private void joinMulticastGroup(String ip) {
        try {
            InetAddress g = InetAddress.getByName(ip);
            socket.joinGroup(g);
            joinedGroups.add(ip);
            log("Join nhóm " + ip);
            allMessages.add(new ChatMessage("System", ip, "Đã tham gia nhóm " + ip, MsgType.SYSTEM, false));
            renderChatHTML();
        } catch (IOException ignored) {}
    }

    private void leaveMulticastGroup(String ip) {
        try {
            InetAddress g = InetAddress.getByName(ip);
            socket.leaveGroup(g);
            joinedGroups.remove(ip);
            log("Rời nhóm " + ip);
            allMessages.add(new ChatMessage("System", ip, "Đã rời nhóm " + ip, MsgType.SYSTEM, false));
            renderChatHTML();
        } catch (IOException ignored) {}
    }

    private void handleIncoming(String senderIP, String target, String content, MsgType type) {
        SwingUtilities.invokeLater(() -> {
            ChatMessage msg = new ChatMessage(senderIP, target, content, type, false);
            allMessages.add(msg);

            // Tự động thêm bạn mới nếu là tin nhắn 1-1
            if (type == MsgType.UNICAST && !senderIP.equals("127.0.0.1") && !senderIP.equals(myLocalIP)) {
                ensureChannelExists("user_" + senderIP, senderIP, senderIP, MsgType.UNICAST, "[1-1]");
            }

            for (int i = 0; i < channelListModel.getSize(); i++) {
                ChatChannel ch = channelListModel.get(i);
                if (shouldShowInChannel(msg, ch) && ch != currentChannel) {
                    ch.unreadCount++;
                    channelList.repaint();
                }
            }

            log("Nhận [" + senderIP + "]: " + content);
            renderChatHTML();
        });
    }

    // =========================================================================
    // 7. GỬI TIN NHẮN THEO ĐÚNG KÊNH ĐANG CHỌN
    // =========================================================================
    private void sendMessage() {
        String content = txtInputMessage.getText().trim();
        if (content.isEmpty() || currentChannel == null) return;

        String targetStr = currentChannel.targetAddress;
        MsgType type = currentChannel.type;

        try {
            InetAddress targetAddr = InetAddress.getByName(targetStr);
            byte[] data = content.getBytes("UTF-8");

            DatagramPacket packet = new DatagramPacket(data, data.length, targetAddr, listenPort);
            socket.send(packet);

            allMessages.add(new ChatMessage("Tôi", targetStr, content, type, true));
            log("Gửi tới [" + targetStr + "]: " + content);

            txtInputMessage.setText("");
            renderChatHTML();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Lỗi gửi: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void executeRawCommand() {
        String raw = txtRawCommand.getText().trim();
        if (raw.toLowerCase().startsWith("net send ")) {
            String[] parts = raw.split(" ", 4);
            if (parts.length >= 4) {
                txtInputMessage.setText(parts[3].trim());
                sendMessage();
            }
        }
    }

    private void openAddTargetDialog() {
        String ip = JOptionPane.showInputDialog(this, "Nhập IP người chat (1-1) hoặc IP Nhóm (225.x.x.x):", "Thêm Kênh Chat", JOptionPane.PLAIN_MESSAGE);
        if (ip == null || ip.trim().isEmpty()) return;
        ip = ip.trim();

        boolean isMC = ip.startsWith("224.") || ip.startsWith("225.");
        String name = isMC ? "Nhóm " + ip : ip;
        ChatChannel ch = new ChatChannel(UUID.randomUUID().toString(), name, ip, isMC ? MsgType.MULTICAST : MsgType.UNICAST, isMC ? "[NHÓM]" : "[1-1]");

        if (isMC) joinMulticastGroup(ip);

        channelListModel.addElement(ch);
        channelList.setSelectedValue(ch, true);
    }

    private void openGroupManagerDialog() {
        JDialog dlg = new JDialog(this, "Quản lý Nhóm Multicast", true);
        dlg.setSize(400, 280);
        dlg.setLocationRelativeTo(this);

        DefaultListModel<String> m = new DefaultListModel<>();
        for (String g : joinedGroups) m.addElement(g);
        JList<String> list = new JList<>(m);
        list.setFont(new Font("Consolas", Font.BOLD, 13));

        JPanel top = new JPanel(new BorderLayout(6, 0));
        top.setBorder(new EmptyBorder(8, 8, 8, 8));
        JTextField txtG = new JTextField("225.1.2.4");
        JButton btnAdd = createStyledButton("Tham gia Nhóm", true);
        btnAdd.addActionListener(e -> {
            String g = txtG.getText().trim();
            if (!g.isEmpty() && !joinedGroups.contains(g)) {
                joinMulticastGroup(g);
                m.addElement(g);
            }
        });
        top.add(txtG, BorderLayout.CENTER);
        top.add(btnAdd, BorderLayout.EAST);

        JButton btnLeave = createStyledButton("Rời Nhóm Đang Chọn", false);
        btnLeave.addActionListener(e -> {
            String sel = list.getSelectedValue();
            if (sel != null) {
                leaveMulticastGroup(sel);
                m.removeElement(sel);
            }
        });

        dlg.add(top, BorderLayout.NORTH);
        dlg.add(new JScrollPane(list), BorderLayout.CENTER);
        dlg.add(btnLeave, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void openLogDialog() {
        JDialog dlg = new JDialog(this, "Nhật Ký Kỹ Thuật (Socket UDP)", false);
        dlg.setSize(560, 380);
        dlg.setLocationRelativeTo(this);
        if (txtActivityLog == null) {
            txtActivityLog = new JTextArea();
            txtActivityLog.setFont(new Font("Consolas", Font.PLAIN, 12));
            txtActivityLog.setEditable(false);
            txtActivityLog.setBackground(new Color(0x0F, 0x17, 0x2A));
            txtActivityLog.setForeground(new Color(0x38, 0xBD, 0xF8));
        }
        dlg.add(new JScrollPane(txtActivityLog));
        dlg.setVisible(true);
    }

    private void log(String msg) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String line = "[" + time + "] " + msg;
        System.out.println(line);
        if (txtActivityLog == null) {
            txtActivityLog = new JTextArea();
            txtActivityLog.setFont(new Font("Consolas", Font.PLAIN, 12));
            txtActivityLog.setEditable(false);
            txtActivityLog.setBackground(new Color(0x0F, 0x17, 0x2A));
            txtActivityLog.setForeground(new Color(0x38, 0xBD, 0xF8));
        }
        txtActivityLog.append(line + "\n");
    }

    private void ensureChannelExists(String id, String name, String ip, MsgType type, String tag) {
        for (int i = 0; i < channelListModel.getSize(); i++) {
            if (channelListModel.get(i).targetAddress.equals(ip)) return;
        }
        channelListModel.addElement(new ChatChannel(id, name, ip, type, tag));
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br/>");
    }

    // ITEM DANH SÁCH BÊN TRÁI
    private class ChannelCellRenderer extends JPanel implements ListCellRenderer<ChatChannel> {
        private final JLabel lblTag = new JLabel();
        private final JLabel lblTitle = new JLabel();
        private final JLabel lblBadge = new JLabel();

        public ChannelCellRenderer() {
            setLayout(new BorderLayout(8, 0));
            setBorder(new EmptyBorder(8, 12, 8, 12));
            setOpaque(true);

            lblTag.setFont(new Font("Consolas", Font.BOLD, 10));
            lblTag.setForeground(new Color(0x38, 0xBD, 0xF8));

            lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));

            lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lblBadge.setForeground(Color.WHITE);
            lblBadge.setOpaque(true);
            lblBadge.setBackground(new Color(0xEF, 0x44, 0x44));
            lblBadge.setBorder(new EmptyBorder(1, 5, 1, 5));

            add(lblTag, BorderLayout.WEST);
            add(lblTitle, BorderLayout.CENTER);
            add(lblBadge, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ChatChannel> list, ChatChannel val, int idx, boolean isSel, boolean hasFocus) {
            lblTag.setText(val.tag);
            lblTitle.setText(val.name);

            if (val.unreadCount > 0) {
                lblBadge.setText(String.valueOf(val.unreadCount));
                lblBadge.setVisible(true);
            } else {
                lblBadge.setVisible(false);
            }

            if (isSel) {
                setBackground(COLOR_SIDEBAR_ACTIVE);
                lblTitle.setForeground(Color.WHITE);
                lblTag.setForeground(new Color(0xDB, 0xEA, 0xFE));
            } else {
                setBackground(COLOR_SIDEBAR);
                lblTitle.setForeground(new Color(0xE2, 0xE8, 0xF0));
                lblTag.setForeground(new Color(0x94, 0xA3, 0xB8));
            }
            return this;
        }
    }

    private JButton createStyledButton(String text, boolean isPrimary) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (isPrimary) {
            btn.setBackground(COLOR_PRIMARY);
            btn.setForeground(Color.WHITE);
            btn.setBorder(new EmptyBorder(6, 14, 6, 14));
        } else {
            btn.setBackground(new Color(0xF1, 0xF5, 0xF9));
            btn.setForeground(COLOR_TEXT_DARK);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(COLOR_BORDER, 1),
                    new EmptyBorder(5, 12, 5, 12)
            ));
        }
        return btn;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new NetSendChatGUI().setVisible(true);
        });
    }
}
