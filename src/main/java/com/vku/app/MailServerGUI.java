package com.vku.app;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

    // Nơi lưu trữ tài khoản (Tên -> Mật khẩu)
    private Map<String, String> registeredAccounts = new HashMap<>();
    private final File accountFile = new File("storage", "accounts.txt");

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

        loadAccounts(); // Nạp dữ liệu tài khoản khi bật server

        new Thread(this::startServer).start();
    }

    // Đọc danh sách tài khoản từ file txt
    private void loadAccounts() {
        try {
            File storageDir = new File("storage");
            if (!storageDir.exists())
                storageDir.mkdirs();

            if (accountFile.exists()) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(new FileInputStream(accountFile), StandardCharsets.UTF_8));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("::");
                    if (parts.length == 2) {
                        registeredAccounts.put(parts[0], parts[1]);
                    }
                }
                br.close();
            }
        } catch (Exception e) {
            log("Lỗi đọc file accounts: " + e.getMessage());
        }
    }

    // Ghi tài khoản mới vào file txt
    private void saveAccount(String username, String password) {
        try {
            registeredAccounts.put(username, password);
            BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(accountFile, true), StandardCharsets.UTF_8));
            bw.write(username + "::" + password + "\n");
            bw.close();
        } catch (Exception e) {
            log("Lỗi ghi file accounts: " + e.getMessage());
        }
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

            // XỬ LÝ ĐĂNG KÝ
            if (cmd.equals("REGISTER")) {
                String name = parts[1];
                String pass = parts[2];
                if (registeredAccounts.containsKey(name)) {
                    connection.send("SYS::Tên tài khoản đã tồn tại!", clientIP, clientPort);
                } else {
                    saveAccount(name, pass); // Lưu vào bộ nhớ và file
                    connection.send("SYS::Đăng ký thành công! Hãy bấm Đăng nhập.", clientIP, clientPort);
                    log("Tài khoản mới được tạo: " + name);
                }

                // XỬ LÝ ĐĂNG NHẬP
            } else if (cmd.equals("LOGIN")) {
                String name = parts[1];
                String pass = parts[2];

                // Kiểm tra tài khoản và mật khẩu
                if (!registeredAccounts.containsKey(name)) {
                    connection.send("SYS::Tài khoản không tồn tại!", clientIP, clientPort);
                    return;
                }
                if (!registeredAccounts.get(name).equals(pass)) {
                    connection.send("SYS::Sai mật khẩu!", clientIP, clientPort);
                    return;
                }

                // Đăng nhập thành công, thiết lập kết nối
                InetAddress realClientIP = clientIP;
                try {
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

                // Ghi đè file câu chúc
                Writer w = new OutputStreamWriter(new FileOutputStream(new File(dir, "new_email.txt")),
                        StandardCharsets.UTF_8);
                w.write("Thank you for using this service. we hope that you will feel comfortable");
                w.close();

                log(name + " đã đăng nhập");
                connection.send("SYS::Đăng nhập thành công", clientIP, clientPort);

                // Đồng bộ thư cũ (như bản trước)
                File[] files = dir.listFiles();
                if (files != null && files.length > 0) {
                    for (File f : files) {
                        if (f.isFile() && f.getName().startsWith("email_")) {
                            try {
                                byte[] fileBytes = Files.readAllBytes(f.toPath());
                                String mailContent = new String(fileBytes, StandardCharsets.UTF_8);
                                connection.send("MAIL::[THƯ ĐÃ NHẬN TRƯỚC ĐÓ]\n" + mailContent + "\n", clientIP,
                                        clientPort);
                                Thread.sleep(50);
                            } catch (Exception e) {
                            }
                        }
                    }
                }

            } else if (cmd.equals("LOGOUT")) {
                String name = parts[1];
                activeClients.remove(name);
                log(name + " đã ngắt kết nối");

                // XỬ LÝ GỬI TEXT
            } else if (cmd.equals("SEND")) {
                String sender = parts[1];
                String receiver = parts[2];
                String title = parts[3];
                String content = parts[4];

                File toDir = new File("storage", receiver);
                if (!toDir.exists())
                    toDir.mkdirs();

                String currentTime = dateFormat.format(new Date());
                String formattedMail = String.format("Time: %s\nSender: %s\nReceiver: %s\nTitle: %s\nContent: %s",
                        currentTime, sender, receiver, title, content);

                Writer w = new OutputStreamWriter(
                        new FileOutputStream(new File(toDir, "email_" + System.currentTimeMillis() + ".txt")),
                        StandardCharsets.UTF_8);
                w.write(formattedMail);
                w.close();

                log(sender + " đã gửi một thư đến " + receiver + ":\n   [Tiêu đề] " + title + "\n   [Nội dung] "
                        + content);

                connection.send("SYS::Gửi thư thành công cho " + receiver + ".", clientIP, clientPort);

                InetSocketAddress receiverAddress = activeClients.get(receiver);
                if (receiverAddress != null) {
                    connection.send("MAIL::\n[Thư mới nhận] >>>\n" + formattedMail + "\n", receiverAddress.getAddress(),
                            receiverAddress.getPort());
                }

                // XỬ LÝ GỬI FILE ĐÍNH KÈM
            } else if (cmd.equals("FILE")) {
                String sender = parts[1];
                String receiver = parts[2];
                String fileName = parts[3];
                String base64Content = parts[4];

                File toDir = new File("storage", receiver);
                if (!toDir.exists())
                    toDir.mkdirs();

                byte[] fileBytes = java.util.Base64.getDecoder().decode(base64Content);
                File destFile = new File(toDir, fileName);
                java.nio.file.Files.write(destFile.toPath(), fileBytes);

                log("   => Tệp đính kèm: [" + fileName + "] đã được lưu vào hộp thư của " + receiver);

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