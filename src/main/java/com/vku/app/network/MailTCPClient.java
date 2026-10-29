package com.vku.app.network;

import com.vku.app.model.MailMessage;
import com.vku.app.model.UserAccount;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class MailTCPClient {
    private final String serverHost;
    private final int serverPort;
    private TCPConnection connection;
    private Consumer<MailMessage> onMailReceived;
    private boolean isListening = false;

    // Quản lý phản hồi cho request gửi từ UI thread
    private CompletableFuture<String> pendingResponse;

    public MailTCPClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public void setOnMailReceived(Consumer<MailMessage> onMailReceived) {
        this.onMailReceived = onMailReceived;
    }

    public void connect() throws IOException {
        if (connection == null || !connection.isConnected()) {
            connection = new TCPConnection(serverHost, serverPort);
        }
    }

    public String register(String username, String email, String password) throws IOException {
        connect();
        connection.sendText("REGISTER::" + username + "::" + email + "::" + password);
        return connection.receiveText();
    }

    public UserAccount login(String identifier, String password) throws IOException {
        connect();
        connection.sendText("LOGIN::" + identifier + "::" + password);
        String response = connection.receiveText();
        if (response.startsWith("SYS_OK::LOGIN_SUCCESS::")) {
            String[] parts = response.split("::");
            String username = parts[2];
            String email = parts[3];
            return new UserAccount(username, email, password);
        } else {
            String error = response.replace("SYS_ERROR::", "");
            throw new IOException(error);
        }
    }

    /**
     * Bắt đầu lắng nghe gói tin từ Server.
     * Khi nhận được file từ máy khác gửi sang, sẽ lưu trực tiếp vào thư mục file_temp/
     */
    public void startListening(File fileTempDir) {
        if (isListening) return;
        isListening = true;

        new Thread(() -> {
            while (isListening && connection != null && connection.isConnected()) {
                try {
                    String msg = connection.receiveText();
                    if (msg == null) break;

                    if (msg.startsWith("NEW_MAIL::")) {
                        // Cú pháp: NEW_MAIL::sender::title::content::fileCount
                        String[] parts = msg.split("::", 5);
                        String sender = parts[1];
                        String title = parts[2];
                        String content = parts[3];
                        int fileCount = Integer.parseInt(parts[4]);

                        MailMessage mail = new MailMessage();
                        mail.setSender(sender);
                        mail.setTitle(title);
                        mail.setContent(content);

                        // Lưu trực tiếp các file nhận được vào thư mục storage/file_temp/
                        for (int i = 0; i < fileCount; i++) {
                            File received = connection.receiveFile(fileTempDir);
                            mail.addFile(received);
                        }

                        if (onMailReceived != null) {
                            onMailReceived.accept(mail);
                        }
                    } else if (msg.startsWith("SYS_OK") || msg.startsWith("SYS_ERROR")) {
                        // Nếu là phản hồi của một thao tác gửi thư (sendMail)
                        if (pendingResponse != null && !pendingResponse.isDone()) {
                            pendingResponse.complete(msg);
                        }
                    }
                } catch (Exception e) {
                    if (pendingResponse != null && !pendingResponse.isDone()) {
                        pendingResponse.completeExceptionally(e);
                    }
                    break;
                }
            }
        }).start();
    }

    public void sendMail(String receiver, String title, String content, List<File> files) throws Exception {
        if (connection == null || !connection.isConnected()) {
            throw new IOException("Chưa kết nối đến TCP Server!");
        }

        pendingResponse = new CompletableFuture<>();

        int fileCount = (files != null) ? files.size() : 0;
        connection.sendText("SEND_MAIL::" + receiver + "::" + title + "::" + content + "::" + fileCount);

        if (files != null) {
            for (File f : files) {
                connection.sendFile(f);
            }
        }

        // Đợi phản hồi được đọc từ background thread listening
        String res = pendingResponse.get(10, java.util.concurrent.TimeUnit.SECONDS);
        if (!res.startsWith("SYS_OK")) {
            throw new IOException(res.replace("SYS_ERROR::", ""));
        }
    }

    public void disconnect() {
        isListening = false;
        try {
            if (connection != null && connection.isConnected()) {
                connection.sendText("LOGOUT");
            }
        } catch (Exception ignored) {}
        if (connection != null) {
            connection.close();
        }
    }
}
