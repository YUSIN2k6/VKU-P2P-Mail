package com.vku.app.network;

import com.vku.app.model.MailMessage;
import com.vku.app.model.UserAccount;
import com.vku.app.repository.MailRepository;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MailTCPServer {
    private final int port;
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private final MailRepository repository;
    private final Consumer<String> logListener;

    // Lưu các client online: Identifier/Email -> ClientHandler
    private final Map<String, ClientHandler> onlineClients = Collections.synchronizedMap(new HashMap<>());

    public MailTCPServer(int port, Consumer<String> logListener) {
        this.port = port;
        this.repository = new MailRepository();
        this.logListener = logListener != null ? logListener : msg -> {};
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        isRunning = true;
        log("TCP Mail Server đã khởi động trên cổng: " + port);

        new Thread(() -> {
            while (isRunning) {
                try {
                    Socket socket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(socket);
                    new Thread(handler).start();
                } catch (IOException e) {
                    if (isRunning) {
                        log("Lỗi Server socket: " + e.getMessage());
                    }
                }
            }
        }).start();
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            for (ClientHandler handler : onlineClients.values()) {
                handler.close();
            }
            onlineClients.clear();
        } catch (Exception e) {
            log("Lỗi dừng Server: " + e.getMessage());
        }
    }

    private void log(String message) {
        logListener.accept(message);
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private TCPConnection connection;
        private UserAccount user;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                connection = new TCPConnection(socket);
                while (connection.isConnected()) {
                    String request = connection.receiveText();
                    if (request == null) break;

                    String[] parts = request.split("::", 5);
                    String cmd = parts[0];

                    switch (cmd) {
                        case "REGISTER" -> handleRegister(parts);
                        case "LOGIN" -> handleLogin(parts);
                        case "SEND_MAIL" -> handleSendMail(parts);
                        case "LOGOUT" -> {
                            handleLogout();
                            return;
                        }
                        default -> connection.sendText("SYS_ERROR::Lệnh không hợp lệ");
                    }
                }
            } catch (Exception e) {
                // Client ngắt kết nối
            } finally {
                handleLogout();
                close();
            }
        }

        private void handleRegister(String[] parts) throws IOException {
            if (parts.length < 4) {
                connection.sendText("SYS_ERROR::Thiếu thông tin đăng ký");
                return;
            }
            String username = parts[1];
            String email = parts[2];
            String password = parts[3];

            UserAccount existing = repository.findAccount(email);
            if (existing != null) {
                connection.sendText("SYS_ERROR::Email đã tồn tại");
                return;
            }

            UserAccount account = new UserAccount(username, email, password);
            boolean saved = repository.saveAccount(account);
            if (saved) {
                connection.sendText("SYS_OK::Đăng ký thành công");
                log("Đăng ký tài khoản mới: " + email + " -> storage/" + account.getFolderPrefix() + ".txt");
            } else {
                connection.sendText("SYS_ERROR::Lỗi lưu trữ dữ liệu");
            }
        }

        private void handleLogin(String[] parts) throws IOException {
            if (parts.length < 3) {
                connection.sendText("SYS_ERROR::Thiếu thông tin đăng nhập");
                return;
            }
            String identifier = parts[1];
            String password = parts[2];

            UserAccount account = repository.findAccount(identifier);
            if (account == null) {
                connection.sendText("SYS_ERROR::Tài khoản không tồn tại");
                return;
            }

            if (!account.getPassword().equals(password)) {
                connection.sendText("SYS_ERROR::Sai mật khẩu");
                return;
            }

            this.user = account;
            onlineClients.put(account.getEmail().toLowerCase().trim(), this);
            onlineClients.put(account.getFolderPrefix().toLowerCase().trim(), this);
            onlineClients.put(account.getUsername().toLowerCase().trim(), this);

            connection.sendText("SYS_OK::LOGIN_SUCCESS::" + account.getUsername() + "::" + account.getEmail());
            log(account.getEmail() + " đã đăng nhập (TCP)");
        }

        private void handleSendMail(String[] parts) throws Exception {
            // Cú pháp: SEND_MAIL::receiver::title::content::fileCount
            if (parts.length < 5) {
                connection.sendText("SYS_ERROR::Thiếu dữ liệu gửi thư");
                return;
            }
            String receiver = parts[1].trim();
            String title = parts[2];
            String content = parts[3];
            int fileCount = Integer.parseInt(parts[4]);

            String currentTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            MailMessage mail = new MailMessage(currentTime, user.getEmail(), receiver, title, content);

            // Nhận từng file gửi qua TCP stream và lưu vào storage/image/
            File imgStorageDir = repository.getImageStorageDir();
            for (int i = 0; i < fileCount; i++) {
                File savedFile = connection.receiveFile(imgStorageDir);
                mail.addFile(savedFile);
            }

            // 1. Lưu thông tin thư vào file txt của người gửi: storage/<sender_prefix>.txt
            repository.appendMailToUserDataFile(user.getFolderPrefix(), mail);

            // 2. Lưu thông tin thư vào file txt của người nhận: storage/<receiver_prefix>.txt
            String receiverPrefix = receiver.contains("@") ? receiver.substring(0, receiver.indexOf("@")) : receiver;
            repository.appendMailToUserDataFile(receiverPrefix, mail);

            // Gửi phản hồi thành công về cho người gửi
            connection.sendText("SYS_OK::Đã gửi thư thành công");
            log(user.getEmail() + " đã gửi thư qua TCP đến " + receiver + " (Kèm " + fileCount + " file)");

            // 3. Nếu người nhận đang online, chuyển tiếp real-time
            ClientHandler receiverHandler = onlineClients.get(receiver.toLowerCase());
            if (receiverHandler == null) {
                receiverHandler = onlineClients.get(receiverPrefix.toLowerCase());
            }

            if (receiverHandler != null && receiverHandler.connection != null && receiverHandler.connection.isConnected()) {
                receiverHandler.connection.sendText("NEW_MAIL::" + user.getEmail() + "::" + title + "::" + content + "::" + fileCount);
                for (File file : mail.getFiles()) {
                    receiverHandler.connection.sendFile(file);
                }
                log("-> Đã chuyển tiếp thư tức thì tới " + receiver);
            } else {
                log("-> Người nhận " + receiver + " hiện không online, thư đã được lưu vào storage/" + receiverPrefix + ".txt");
            }
        }

        private void handleLogout() {
            if (user != null) {
                onlineClients.remove(user.getEmail().toLowerCase().trim());
                onlineClients.remove(user.getFolderPrefix().toLowerCase().trim());
                onlineClients.remove(user.getUsername().toLowerCase().trim());
                log(user.getEmail() + " đã ngắt kết nối (TCP)");
                user = null;
            }
        }

        public void close() {
            if (connection != null) {
                connection.close();
            }
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (Exception ignored) {}
        }
    }
}
