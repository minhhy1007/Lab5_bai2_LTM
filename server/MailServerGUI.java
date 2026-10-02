package server;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class MailServerGUI extends JFrame implements MailServer.LogListener {

    private final JLabel lblStatus;
    private final JTextField txtPort;
    private final JLabel lblClients;
    private final JTextArea txtLog;
    private final JButton btnStart;
    private final JButton btnStop;

    // Color Palette
    private static final Color BG_COLOR = new Color(0xF5, 0xF7, 0xFA);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color PRIMARY_COLOR = new Color(0x25, 0x63, 0xEB);
    private static final Color SUCCESS_COLOR = new Color(0x16, 0xA3, 0x4A);
    private static final Color DANGER_COLOR = new Color(0xDC, 0x26, 0x26);
    private static final Color TEXT_COLOR = new Color(0x1F, 0x29, 0x37);
    private static final Color BORDER_COLOR = new Color(0xD1, 0xD5, 0xDB);

    public MailServerGUI() {
        super("Mail Server Dashboard - TCP Socket");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(650, 550);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(BG_COLOR);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // --- HEADER ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BG_COLOR);
        JLabel titleLabel = new JLabel("MAIL SERVER DASHBOARD");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(TEXT_COLOR);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        lblStatus = new JLabel("● STOPPED");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblStatus.setForeground(DANGER_COLOR);
        headerPanel.add(lblStatus, BorderLayout.EAST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // --- CENTER PANEL (INFO CARD & LOG AREA) ---
        JPanel centerPanel = new JPanel(new BorderLayout(15, 15));
        centerPanel.setBackground(BG_COLOR);

        // Info Card
        JPanel infoCard = new JPanel(new GridLayout(3, 2, 10, 10));
        infoCard.setBackground(CARD_BG);
        infoCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(15, 15, 15, 15)
        ));

        infoCard.add(createLabel("Host:"));
        JLabel lblHost = createLabel("localhost (127.0.0.1)");
        lblHost.setFont(new Font("Segoe UI", Font.BOLD, 13));
        infoCard.add(lblHost);

        infoCard.add(createLabel("Port:"));
        txtPort = new JTextField("2345");
        txtPort.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        infoCard.add(txtPort);

        infoCard.add(createLabel("Connected Clients:"));
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

        JLabel logHeader = new JLabel("SERVER LOG");
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

        mainPanel.add(centerPanel, BorderLayout.CENTER);

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
