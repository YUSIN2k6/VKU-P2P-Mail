package com.vku.app;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class MailClientGUI extends JFrame {
    private JTextField txtName, txtPassword, txtTo, txtTitle, txtServerIP;
    private JTextArea txtMessage, txtInbox;
    private JTextField txtAttachedFiles;
    private JButton btnLogin, btnRegister, btnDisconnect, btnSend, btnAddFile, btnOpenStorage;
    private UDPConnection connection;
    private boolean isConnected = false;
    private InetAddress serverAddress;

    private List<File> attachedFilesList = new ArrayList<>();

    private final Font mainFont = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font boldFont = new Font("Segoe UI", Font.BOLD, 13);
    private final Font logFont = new Font("Consolas", Font.PLAIN, 13);

    public MailClientGUI() {
        setTitle("VKU Mail Client - P2P Network");
        setSize(700, 750); // Mở rộng thêm một chút cho khung cấu hình
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // 1. TOP PANEL: Cấu hình và Đăng nhập/Đăng ký
        JPanel topPanel = new JPanel(new GridLayout(3, 3, 10, 10)); // Đổi thành 3 dòng
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), " ⚙ Xác thực tài khoản ",
                        TitledBorder.LEFT, TitledBorder.TOP, boldFont),
                new EmptyBorder(5, 10, 10, 10)));

        topPanel.add(new JLabel("IP Server:"));
        txtServerIP = new JTextField("localhost");
        txtServerIP.setFont(mainFont);
        topPanel.add(txtServerIP);

        btnRegister = new JButton("Đăng ký");
        btnRegister.setFont(boldFont);
        topPanel.add(btnRegister);

        topPanel.add(new JLabel("Tên tài khoản:"));
        txtName = new JTextField();
        txtName.setFont(mainFont);
        topPanel.add(txtName);

        btnLogin = new JButton("Đăng nhập");
        btnLogin.setFont(boldFont);
        topPanel.add(btnLogin);

        topPanel.add(new JLabel("Mật khẩu:"));
        txtPassword = new JTextField(); // Text thường, hiển thị rõ ký tự
        txtPassword.setFont(mainFont);
        topPanel.add(txtPassword);

        btnDisconnect = new JButton("Ngắt kết nối");
        btnDisconnect.setFont(boldFont);
        btnDisconnect.setEnabled(false);
        topPanel.add(btnDisconnect);

        add(topPanel, BorderLayout.NORTH);

        // 2. INBOX AREA
        JPanel inboxPanel = new JPanel(new BorderLayout());
        inboxPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), " 📥 Hộp thư & Thông báo ",
                        TitledBorder.LEFT, TitledBorder.TOP, boldFont),
                new EmptyBorder(5, 5, 5, 5)));

        txtInbox = new JTextArea();
        txtInbox.setEditable(false);
        txtInbox.setFont(logFont);
        txtInbox.setBackground(new Color(245, 245, 245));
        inboxPanel.add(new JScrollPane(txtInbox), BorderLayout.CENTER);

        // 3. COMPOSE AREA
        JPanel sendPanel = new JPanel(new BorderLayout(0, 10));
        sendPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), " ✉ Soạn thư mới ",
                        TitledBorder.LEFT, TitledBorder.TOP, boldFont),
                new EmptyBorder(10, 10, 10, 10)));

        JPanel headerSendPanel = new JPanel(new GridLayout(3, 1, 0, 8));

        JPanel toPanel = new JPanel(new BorderLayout(10, 0));
        toPanel.add(new JLabel("Người nhận:"), BorderLayout.WEST);
        txtTo = new JTextField();
        txtTo.setFont(mainFont);
        toPanel.add(txtTo, BorderLayout.CENTER);

        JPanel titlePanel = new JPanel(new BorderLayout(38, 0));
        titlePanel.add(new JLabel("Tiêu đề:"), BorderLayout.WEST);
        txtTitle = new JTextField();
        txtTitle.setFont(mainFont);
        titlePanel.add(txtTitle, BorderLayout.CENTER);

        JPanel attachPanel = new JPanel(new BorderLayout(26, 0));
        attachPanel.add(new JLabel("Đính kèm:"), BorderLayout.WEST);
        txtAttachedFiles = new JTextField();
        txtAttachedFiles.setFont(mainFont);
        txtAttachedFiles.setEditable(false);
        txtAttachedFiles.setForeground(Color.BLUE);
        attachPanel.add(txtAttachedFiles, BorderLayout.CENTER);

        headerSendPanel.add(toPanel);
        headerSendPanel.add(titlePanel);
        headerSendPanel.add(attachPanel);

        sendPanel.add(headerSendPanel, BorderLayout.NORTH);

        txtMessage = new JTextArea();
        txtMessage.setFont(mainFont);
        JScrollPane scrollMessage = new JScrollPane(txtMessage);
        scrollMessage.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        sendPanel.add(scrollMessage, BorderLayout.CENTER);

        JPanel bottomSendPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        btnSend = new JButton("Gửi Email");
        btnAddFile = new JButton("Thêm File");
        btnOpenStorage = new JButton("Mở kho lưu trữ");

        btnSend.setFont(boldFont);
        btnAddFile.setFont(boldFont);
        btnOpenStorage.setFont(boldFont);

        btnSend.setEnabled(false);
        btnAddFile.setEnabled(false);
        btnOpenStorage.setEnabled(false);

        bottomSendPanel.add(btnSend);
        bottomSendPanel.add(btnAddFile);
        bottomSendPanel.add(btnOpenStorage);

        sendPanel.add(bottomSendPanel, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inboxPanel, sendPanel);
        splitPane.setResizeWeight(0.5);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);

        add(splitPane, BorderLayout.CENTER);

        // Events
        btnRegister.addActionListener(e -> register());
        btnLogin.addActionListener(e -> login());
        btnDisconnect.addActionListener(e -> disconnect());
        btnSend.addActionListener(e -> sendMailWithAttachments());
        btnAddFile.addActionListener(e -> chooseFilesToAttach());
        btnOpenStorage.addActionListener(e -> openStorageFolder());
    }

    private void register() {
        if (!validateInput())
            return;
        try {
            if (connection == null)
                connection = new UDPConnection();
            serverAddress = InetAddress.getByName(txtServerIP.getText().trim());
            // Chỉ gửi lệnh đăng ký, không chuyển sang trạng thái online (isConnected =
            // true)
            // Lắng nghe 1 lần để lấy kết quả đăng ký
            connection.send("REGISTER::" + txtName.getText().trim() + "::" + txtPassword.getText().trim(),
                    serverAddress, 9876);

            DatagramPacket packet = connection.receive();
            String data = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
            if (data.startsWith("SYS::")) {
                JOptionPane.showMessageDialog(this, data.substring(5), "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối Server!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void login() {
        if (!validateInput())
            return;
        try {
            if (connection == null)
                connection = new UDPConnection();
            serverAddress = InetAddress.getByName(txtServerIP.getText().trim());

            // Lắng nghe 1 lần để check đăng nhập
            connection.send("LOGIN::" + txtName.getText().trim() + "::" + txtPassword.getText().trim(), serverAddress,
                    9876);
            DatagramPacket packet = connection.receive();
            String data = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();

            if (data.startsWith("SYS::Đăng nhập thành công")) {
                isConnected = true;
                txtInbox.append("[Hệ thống]: " + data.substring(5) + "\n");

                new Thread(this::listenIncomingMessages).start();
                startKeepAliveThread();

                txtName.setEditable(false);
                txtPassword.setEditable(false);
                btnLogin.setEnabled(false);
                btnRegister.setEnabled(false);
                btnDisconnect.setEnabled(true);
                btnSend.setEnabled(true);
                btnAddFile.setEnabled(true);
                btnOpenStorage.setEnabled(true);
            } else {
                JOptionPane.showMessageDialog(this, data.replace("SYS::", ""), "Đăng nhập thất bại",
                        JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối Server!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean validateInput() {
        if (txtName.getText().trim().isEmpty() || txtPassword.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đủ Tên tài khoản và Mật khẩu!", "Lỗi",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private void startKeepAliveThread() {
        new Thread(() -> {
            while (isConnected) {
                try {
                    Thread.sleep(10000);
                    connection.send("PING::" + txtName.getText().trim(), serverAddress, 9876);
                } catch (Exception e) {
                }
            }
        }).start();
    }

    private void listenIncomingMessages() {
        while (isConnected) {
            try {
                DatagramPacket packet = connection.receive();
                String data = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();

                SwingUtilities.invokeLater(() -> {
                    if (data.startsWith("SYS::")) {
                        txtInbox.append("[Hệ thống]: " + data.substring(5) + "\n");
                    } else if (data.startsWith("MAIL::")) {
                        txtInbox.append("\n" + data.substring(6) + "\n");
                    } else if (data.startsWith("FILE_FWD::")) {
                        try {
                            String[] parts = data.split("::", 4);
                            String sender = parts[1];
                            String fileName = parts[2];
                            byte[] fileBytes = Base64.getDecoder().decode(parts[3]);

                            File myStorage = new File("storage", txtName.getText().trim());
                            if (!myStorage.exists())
                                myStorage.mkdirs();

                            File destFile = new File(myStorage, fileName);
                            Files.write(destFile.toPath(), fileBytes);
                            txtInbox.append("[Hệ thống]: " + sender + " vừa gửi cho bạn 1 file -> " + fileName
                                    + " (Đã lưu vào kho)\n");
                        } catch (Exception e) {
                        }
                    }
                    txtInbox.setCaretPosition(txtInbox.getDocument().getLength());
                });
            } catch (Exception e) {
            }
        }
    }

    private void disconnect() {
        if (isConnected) {
            try {
                connection.send("LOGOUT::" + txtName.getText().trim(), serverAddress, 9876);
            } catch (Exception e) {
            }
            isConnected = false;
            connection.close();

            txtName.setEditable(true);
            txtPassword.setEditable(true);
            btnLogin.setEnabled(true);
            btnRegister.setEnabled(true);
            btnDisconnect.setEnabled(false);
            btnSend.setEnabled(false);
            btnAddFile.setEnabled(false);
            btnOpenStorage.setEnabled(false);

            txtInbox.append("[Hệ thống]: Đã ngắt kết nối.\n");
            attachedFilesList.clear();
            updateAttachedFilesDisplay();
        }
    }

    // Các hàm chọn file, cập nhật danh sách, gửi thư và mở kho giữ nguyên như cũ
    private void chooseFilesToAttach() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            for (File file : selectedFiles) {
                if (file.length() > 40000) {
                    JOptionPane.showMessageDialog(this, "File '" + file.getName() + "' vượt quá 40KB, sẽ bị bỏ qua.");
                    continue;
                }
                if (!attachedFilesList.contains(file))
                    attachedFilesList.add(file);
            }
            updateAttachedFilesDisplay();
        }
    }

    private void updateAttachedFilesDisplay() {
        if (attachedFilesList.isEmpty()) {
            txtAttachedFiles.setText("");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < attachedFilesList.size(); i++) {
            sb.append(attachedFilesList.get(i).getName());
            if (i < attachedFilesList.size() - 1)
                sb.append(", ");
        }
        txtAttachedFiles.setText(sb.toString());
    }

    private void sendMailWithAttachments() {
        String to = txtTo.getText().trim();
        if (to.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập người nhận!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String title = txtTitle.getText().trim();
        if (title.isEmpty())
            title = "(Không tiêu đề)";
        String msg = txtMessage.getText().trim();
        if (msg.isEmpty() && attachedFilesList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Thư trống!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String mailReq = "SEND::" + txtName.getText().trim() + "::" + to + "::" + title + "::" + msg;
            connection.send(mailReq, serverAddress, 9876);

            for (File file : attachedFilesList) {
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                String base64Content = Base64.getEncoder().encodeToString(fileBytes);
                String fileReq = "FILE::" + txtName.getText().trim() + "::" + to + "::" + file.getName() + "::"
                        + base64Content;
                connection.send(fileReq, serverAddress, 9876);
                Thread.sleep(100);
            }

            txtTitle.setText("");
            txtMessage.setText("");
            attachedFilesList.clear();
            updateAttachedFilesDisplay();
        } catch (Exception e) {
            txtInbox.append("[Lỗi]: Gửi thất bại -> " + e.getMessage() + "\n");
        }
    }

    private void openStorageFolder() {
        try {
            File dir = new File("storage", txtName.getText().trim());
            if (!dir.exists())
                dir.mkdirs();
            Desktop.getDesktop().open(dir);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi mở thư mục.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}