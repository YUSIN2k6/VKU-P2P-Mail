package com.vku.app.repository;

import com.vku.app.model.MailMessage;
import com.vku.app.model.UserAccount;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class MailRepository {
    private static final String STORAGE_ROOT = "storage";

    public MailRepository() {
        File root = new File(STORAGE_ROOT);
        if (!root.exists()) {
            root.mkdirs();
        }
        getImageStorageDir();
        getFileTempDir();
    }

    public File getImageStorageDir() {
        File imgDir = new File(STORAGE_ROOT, "image");
        if (!imgDir.exists()) {
            imgDir.mkdirs();
        }
        return imgDir;
    }

    public File getFileTempDir() {
        File tempDir = new File(STORAGE_ROOT, "file_temp");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        return tempDir;
    }

    public File getUserDataFile(String prefix) {
        return new File(STORAGE_ROOT, prefix + ".txt");
    }

    public synchronized boolean saveAccount(UserAccount account) {
        try {
            File userFile = getUserDataFile(account.getFolderPrefix());
            
            if (!userFile.exists()) {
                try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(userFile), StandardCharsets.UTF_8))) {
                    bw.write("=== THÔNG TIN TÀI KHOẢN ===\n");
                    bw.write("Username: " + account.getUsername() + "\n");
                    bw.write("Email: " + account.getEmail() + "\n");
                    bw.write("Password: " + account.getPassword() + "\n");
                    bw.write("===========================\n\n");
                }
            } else {
                List<String> lines = Files.readAllLines(userFile.toPath(), StandardCharsets.UTF_8);
                boolean hasHeader = false;
                for (String l : lines) {
                    if (l.startsWith("Email: ")) {
                        hasHeader = true;
                        break;
                    }
                }
                if (!hasHeader) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("=== THÔNG TIN TÀI KHOẢN ===\n");
                    sb.append("Username: ").append(account.getUsername()).append("\n");
                    sb.append("Email: ").append(account.getEmail()).append("\n");
                    sb.append("Password: ").append(account.getPassword()).append("\n");
                    sb.append("===========================\n\n");
                    for (String l : lines) {
                        sb.append(l).append("\n");
                    }
                    Files.writeString(userFile.toPath(), sb.toString(), StandardCharsets.UTF_8);
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public synchronized UserAccount findAccount(String identifier) {
        File root = new File(STORAGE_ROOT);
        File[] files = root.listFiles((dir, name) -> name.endsWith(".txt") && !name.equalsIgnoreCase("run.txt"));
        if (files == null) return null;

        for (File f : files) {
            UserAccount acc = readAccountFromFile(f);
            if (acc != null) {
                String prefix = f.getName().replace(".txt", "");
                if (identifier.equalsIgnoreCase(acc.getEmail()) ||
                    identifier.equalsIgnoreCase(acc.getUsername()) ||
                    identifier.equalsIgnoreCase(prefix)) {
                    return acc;
                }
            }
        }
        return null;
    }

    private UserAccount readAccountFromFile(File file) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            String user = "";
            String email = "";
            String pass = "";

            while ((line = br.readLine()) != null) {
                if (line.startsWith("Username: ")) {
                    user = line.substring("Username: ".length()).trim();
                } else if (line.startsWith("Email: ")) {
                    email = line.substring("Email: ".length()).trim();
                } else if (line.startsWith("Password: ")) {
                    pass = line.substring("Password: ".length()).trim();
                } else if (line.contains("::")) {
                    String[] parts = line.split("::");
                    if (parts.length >= 2) {
                        user = parts[0].trim();
                        pass = parts[1].trim();
                        email = user;
                    }
                }
                if (line.startsWith("===========================")) {
                    break;
                }
            }
            if (!user.isEmpty() || !email.isEmpty()) {
                return new UserAccount(user, email, pass);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public synchronized boolean appendMailToUserDataFile(String prefix, MailMessage mail) {
        try {
            File userFile = getUserDataFile(prefix);

            List<String> fileNames = new ArrayList<>();
            if (mail.getFiles() != null && !mail.getFiles().isEmpty()) {
                File imgDir = getImageStorageDir();
                for (File file : mail.getFiles()) {
                    if (file.exists()) {
                        File dest = new File(imgDir, file.getName());
                        Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        fileNames.add(file.getName());
                    }
                }
            }

            String fileEntry = fileNames.isEmpty() ? "null" : String.join(", ", fileNames);

            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(userFile, true), StandardCharsets.UTF_8))) {
                bw.write("--- THƯ LỊCH SỬ ---\n");
                bw.write("Sending time: " + mail.getSendingTime() + "\n");
                bw.write("Sender: " + mail.getSender() + "\n");
                bw.write("Receiver: " + mail.getReceiver() + "\n");
                bw.write("Title: " + mail.getTitle() + "\n");
                bw.write("Content:\n" + mail.getContent() + "\n");
                bw.write("File: " + fileEntry + "\n");
                bw.write("-------------------\n\n");
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Đọc toàn bộ lịch sử thư đã gửi/nhận từ file storage/<prefix>.txt để đưa lên giao diện
     */
    public synchronized List<MailMessage> loadMailHistory(String prefix) {
        List<MailMessage> list = new ArrayList<>();
        File userFile = getUserDataFile(prefix);
        if (!userFile.exists()) return list;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(userFile), StandardCharsets.UTF_8))) {
            String line;
            boolean inMailBlock = false;
            String time = "";
            String sender = "";
            String receiver = "";
            String title = "";
            StringBuilder content = new StringBuilder();
            String fileStr = "";
            boolean inContent = false;

            while ((line = br.readLine()) != null) {
                if (line.startsWith("--- THƯ LỊCH SỬ ---")) {
                    inMailBlock = true;
                    time = "";
                    sender = "";
                    receiver = "";
                    title = "";
                    content = new StringBuilder();
                    fileStr = "";
                    inContent = false;
                    continue;
                }

                if (inMailBlock) {
                    if (line.startsWith("-------------------")) {
                        inMailBlock = false;
                        MailMessage mail = new MailMessage(time, sender, receiver, title, content.toString().trim());
                        if (!fileStr.isEmpty() && !fileStr.equalsIgnoreCase("null")) {
                            String[] fileNames = fileStr.split(",");
                            for (String fn : fileNames) {
                                String cleanName = fn.trim();
                                // Tìm trong file_temp trước, sau đó tìm trong image
                                File f = new File(getFileTempDir(), cleanName);
                                if (!f.exists()) {
                                    f = new File(getImageStorageDir(), cleanName);
                                }
                                if (f.exists()) {
                                    mail.addFile(f);
                                } else {
                                    mail.addFile(new File(getFileTempDir(), cleanName));
                                }
                            }
                        }
                        list.add(mail);
                        continue;
                    }

                    if (line.startsWith("Sending time: ")) {
                        time = line.substring("Sending time: ".length()).trim();
                    } else if (line.startsWith("Sender: ")) {
                        sender = line.substring("Sender: ".length()).trim();
                    } else if (line.startsWith("Receiver: ")) {
                        receiver = line.substring("Receiver: ".length()).trim();
                    } else if (line.startsWith("Title: ")) {
                        title = line.substring("Title: ".length()).trim();
                    } else if (line.startsWith("Content:")) {
                        inContent = true;
                    } else if (line.startsWith("File: ")) {
                        inContent = false;
                        fileStr = line.substring("File: ".length()).trim();
                    } else if (inContent) {
                        content.append(line).append("\n");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public File getStorageRootDir() {
        return new File(STORAGE_ROOT);
    }
}
