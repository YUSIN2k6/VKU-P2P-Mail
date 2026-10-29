package com.vku.app.view;

import com.vku.app.network.MailTCPServer;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MailServerGUI extends JFrame {
    private JTextArea logArea;
    private JButton btnStart, btnStop;
    private JTextField txtPort;
    private MailTCPServer server;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");

    public MailServerGUI() {
        setTitle("VKU TCP Mail Server");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Control Panel phía trên
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.add(new JLabel("Cổng (Port TCP):"));
        txtPort = new JTextField("8888", 6);
        topPanel.add(txtPort);

        btnStart = new JButton("Khởi động Server");
        btnStart.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        topPanel.add(btnStart);

        btnStop = new JButton("Dừng Server");
        btnStop.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnStop.setEnabled(false);
        topPanel.add(btnStop);

        add(topPanel, BorderLayout.NORTH);

        // Khung log ở giữa
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        btnStart.addActionListener(e -> startServer());
        btnStop.addActionListener(e -> stopServer());

        // Tự động khởi động khi mở giao diện
        startServer();
    }

    private void log(String message) {
        String entry = "[" + dateFormat.format(new Date()) + "] " + message + "\n";
        SwingUtilities.invokeLater(() -> {
            logArea.append(entry);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void startServer() {
        try {
            int port = Integer.parseInt(txtPort.getText().trim());
            server = new MailTCPServer(port, this::log);
            server.start();
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
            txtPort.setEnabled(false);
        } catch (IOException e) {
            log("Lỗi khởi động Server: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Không thể mở cổng TCP: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Cổng phải là một số hợp lệ!", "Lỗi", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void stopServer() {
        if (server != null) {
            server.stop();
            server = null;
        }
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        txtPort.setEnabled(true);
        log("Server đã dừng.");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MailServerGUI().setVisible(true));
    }
}
