package com.vku.app.controller;

import com.vku.app.model.MailMessage;
import com.vku.app.model.UserAccount;
import com.vku.app.network.EmailService;
import com.vku.app.network.MailTCPClient;
import com.vku.app.repository.MailRepository;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class MailController {
    public enum SendMode {
        BOTH_EMAIL_AND_TCP, // 1. Gửi qua Email và TCP Server
        ONLY_EMAIL, // 2. Chỉ gửi qua Email
        ONLY_TCP // 3. Chỉ gửi qua TCP Server (Mặc định)
    }

    private final MailRepository repository;
    private UserAccount currentUser;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private MailTCPClient tcpClient;
    private final String serverHost = "localhost";
    private final int serverPort = 8888;

    public MailController() {
        this.repository = new MailRepository();
    }

    public boolean register(String username, String email, String password, StringBuilder errorMsg) {
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            errorMsg.append("Vui lòng điền đầy đủ Tên, Email và Mật khẩu!");
            return false;
        }

        try {
            if (tcpClient == null) {
                tcpClient = new MailTCPClient(serverHost, serverPort);
            }
            String res = tcpClient.register(username, email, password);
            if (res.startsWith("SYS_OK")) {
                return true;
            } else {
                errorMsg.append(res.replace("SYS_ERROR::", ""));
                return false;
            }
        } catch (Exception e) {
            UserAccount existing = repository.findAccount(email);
            if (existing != null) {
                errorMsg.append("Email này đã được đăng ký!");
                return false;
            }
            UserAccount newAccount = new UserAccount(username, email, password);
            return repository.saveAccount(newAccount);
        }
    }

    public UserAccount login(String identifier, String password, StringBuilder errorMsg) {
        if (identifier.isEmpty() || password.isEmpty()) {
            errorMsg.append("Vui lòng nhập Email/Tên tài khoản và Mật khẩu!");
            return null;
        }

        try {
            if (tcpClient == null) {
                tcpClient = new MailTCPClient(serverHost, serverPort);
            }
            UserAccount account = tcpClient.login(identifier, password);
            this.currentUser = account;
            return account;
        } catch (Exception e) {
            UserAccount account = repository.findAccount(identifier);
            if (account == null) {
                errorMsg.append("Tài khoản không tồn tại!");
                return null;
            }
            if (!account.getPassword().equals(password)) {
                errorMsg.append("Mật khẩu không chính xác!");
                return null;
            }
            this.currentUser = account;
            return account;
        }
    }

    public void startTCPListening(Consumer<MailMessage> onMailReceived) {
        if (tcpClient != null && currentUser != null) {
            tcpClient.setOnMailReceived(onMailReceived);
            File fileTempDir = repository.getFileTempDir();
            tcpClient.startListening(fileTempDir);
        }
    }

    /**
     * Gửi thư theo 3 chế độ lựa chọn:
     * 1. Gửi qua Email và TCP Server
     * 2. Chỉ gửi qua Email
     * 3. Chỉ gửi qua TCP Server
     */
    public void sendMailWithMode(String receiver, String title, String content, List<File> files, SendMode mode)
            throws Exception {
        if (currentUser == null) {
            throw new IllegalStateException("Chưa đăng nhập!");
        }

        String currentTime = dateFormat.format(new Date());
        MailMessage mail = new MailMessage(currentTime, currentUser.getEmail(), receiver, title, content);
        if (files != null) {
            mail.setFiles(files);
        }

        // Gửi qua Email (SMTP) nếu chọn mode 1 hoặc mode 2
        if (mode == SendMode.BOTH_EMAIL_AND_TCP || mode == SendMode.ONLY_EMAIL) {
            EmailService.sendEmail(currentUser.getEmail(), mail);
        }

        // Gửi qua TCP Server nếu chọn mode 1 hoặc mode 3
        if (mode == SendMode.BOTH_EMAIL_AND_TCP || mode == SendMode.ONLY_TCP) {
            if (tcpClient != null) {
                tcpClient.sendMail(receiver, title, content, files);
            }
        }

        // Luôn lưu bản ghi vào file storage/<prefix>.txt để theo dõi lịch sử
        repository.appendMailToUserDataFile(currentUser.getFolderPrefix(), mail);
    }

    public List<MailMessage> fetchInboxPOP3(int limit) throws Exception {
        return EmailService.fetchInboxPOP3Messages(limit);
    }

    public List<MailMessage> loadSavedMailHistory() {
        if (currentUser == null)
            return List.of();
        return repository.loadMailHistory(currentUser.getFolderPrefix());
    }

    public void logout() {
        if (tcpClient != null) {
            tcpClient.disconnect();
            tcpClient = null;
        }
        currentUser = null;
    }

    public UserAccount getCurrentUser() {
        return currentUser;
    }

    public File getUserDataFile() {
        if (currentUser == null) return null;

        return repository.getUserDataFile(currentUser.getFolderPrefix());
    }

    public File getStorageFolder() {
        return repository.getStorageRootDir();
    }
}
