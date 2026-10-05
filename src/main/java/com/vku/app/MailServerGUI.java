package com.vku.app;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MailServerGUI extends JFrame {
    private JTextArea logArea;
    private UDPConnection connection;
    private boolean isRunning = true;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy");

    private Map<String, InetSocketAddress> activeClients = new HashMap<>();

    public MailServerGUI() {
        setTitle("Mail Server");
        setSize(800, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        add(new JLabel(" Nhật ký hoạt động của Server:"), BorderLayout.NORTH);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        new Thread(this::startServer).start();
    }

    private void log(String message) {
        String logEntry = dateFormat.format(new Date()) + " " + message + "\n";
        SwingUtilities.invokeLater(() -> {
            logArea.append(logEntry);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void startServer() {
        try {
            connection = new UDPConnection(9876);
            log("Server chạy thành công với port 9876.");

            while (isRunning) {
                DatagramPacket packet = connection.receive();
                handlePacket(packet);
            }
        } catch (Exception e) {
            log("Lỗi Server: " + e.getMessage());
        }
    }

    private void handlePacket(DatagramPacket packet) {
        try {
            String request = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
            String[] parts = request.split("::", 5);
            String cmd = parts[0];

            InetAddress clientIP = packet.getAddress();
            int clientPort = packet.getPort();

            if (cmd.equals("PING"))
                return;

            if (cmd.equals("LOGIN")) {
                String name = parts[1];
                InetAddress realClientIP = clientIP;
                try {
                    // Nếu IP trả về là localhost (127.0.0.1), thử lấy IP LAN thực của máy
                    if (clientIP.isLoopbackAddress() || clientIP.isAnyLocalAddress()) {
                        realClientIP = InetAddress.getLocalHost();
                    }
                } catch (Exception ex) {
                }
                activeClients.put(name, new InetSocketAddress(realClientIP, clientPort));
                File dir = new File("storage", name);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                Writer w = new OutputStreamWriter(new FileOutputStream(new File(dir, "new_email.txt")),
                        StandardCharsets.UTF_8);
                w.write("Thank you for using this service. we hope that you will feel comfortable");
                w.close();
                log(name + " đã kết nối");
                connection.send("SYS::Đăng nhập thành công", clientIP, clientPort);

            } else if (cmd.equals("LOGOUT")) {
                String name = parts[1];
                activeClients.remove(name);
                log(name + " đã ngắt kết nối");

                // 1. XỬ LÝ GÓI TEXT BÌNH THƯỜNG (Gửi 1-1)
            } else if (cmd.equals("SEND")) {
                String sender = parts[1];
                String receiver = parts[2];
                String title = parts[3];
                String content = parts[4];

                // Xác định gửi đến account nào, tạo thư mục nếu chưa có
                File toDir = new File("storage", receiver);
                if (!toDir.exists())
                    toDir.mkdirs();

                String currentTime = dateFormat.format(new Date());
                String formattedMail = String.format("Time: %s\nSender: %s\nReceiver: %s\nTitle: %s\nContent: %s",
                        currentTime, sender, receiver, title, content);

                // Lưu file email vào đúng thư mục của người nhận
                Writer w = new OutputStreamWriter(
                        new FileOutputStream(new File(toDir, "email_" + System.currentTimeMillis() + ".txt")),
                        StandardCharsets.UTF_8);
                w.write(formattedMail);
                w.close();

                // Ghi log lên giao diện Server (đã bỏ dấu gạch ngang)
                log(sender + " đã gửi một thư đến " + receiver + ":\n" +
                        "   [Tiêu đề] " + title + "\n" +
                        "   [Nội dung] " + content);

                // Báo cáo thành công cho người gửi
                connection.send("SYS::Gửi thư thành công cho " + receiver + ".", clientIP, clientPort);

                // CHỈ CHUYỂN TIẾP CHO ĐÚNG NGƯỜI NHẬN ĐÓ (Nếu họ đang online)
                InetSocketAddress receiverAddress = activeClients.get(receiver);
                if (receiverAddress != null) {
                    connection.send("MAIL::\n" + formattedMail + "\n", receiverAddress.getAddress(),
                            receiverAddress.getPort());
                }

                // 2. XỬ LÝ GÓI FILE ĐÍNH KÈM (Gửi 1-1)
            } else if (cmd.equals("FILE")) {
                String sender = parts[1];
                String receiver = parts[2];
                String fileName = parts[3];
                String base64Content = parts[4];

                File toDir = new File("storage", receiver);
                if (!toDir.exists())
                    toDir.mkdirs();

                // Lưu file vật lý vào thư mục người nhận
                byte[] fileBytes = java.util.Base64.getDecoder().decode(base64Content);
                File destFile = new File(toDir, fileName);
                java.nio.file.Files.write(destFile.toPath(), fileBytes);

                // Ghi log tệp đính kèm
                log("   => Tệp đính kèm: [" + fileName + "] đã được lưu vào hộp thư của " + receiver);

                // CHỈ CHUYỂN TIẾP TỆP ĐÍNH KÈM CHO ĐÚNG NGƯỜI NHẬN ĐÓ
                InetSocketAddress receiverAddress = activeClients.get(receiver);
                if (receiverAddress != null) {
                    String forwardPkt = "FILE_FWD::" + sender + "::" + fileName + "::" + base64Content;
                    connection.send(forwardPkt, receiverAddress.getAddress(), receiverAddress.getPort());
                }
            }
        } catch (Exception e) {
            log("Lỗi hệ thống: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MailServerGUI().setVisible(true));
    }
}