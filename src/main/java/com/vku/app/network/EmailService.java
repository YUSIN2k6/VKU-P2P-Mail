package com.vku.app.network;

import com.vku.app.model.MailMessage;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EmailService {

    // =========================================================================
    // CẤU HÌNH HỆ THỐNG GỬI/NHẬN GMAIL (SMTP / POP3)
    // Bạn chỉ cần điền Email và dán mã App Password 16 ký tự vào 2 dòng dưới đây:
    // =========================================================================
    public static final String SYSTEM_GMAIL_ADDRESS = "tuannh.24itb@vku.udn.vn";
    public static final String SYSTEM_GMAIL_APP_PASSWORD = "twou azmp lahd caoy"; // DÁN MÃ 16 KÝ TỰ VÀO ĐÂY
    // =========================================================================

    /**
     * Gửi Email qua SMTP.
     * Sử dụng App Password của hệ thống để xác thực với Google SMTP server.
     */
    public static void sendEmail(String senderEmail, MailMessage mail) throws Exception {
        Properties props = new Properties();
        boolean isGmail = senderEmail.toLowerCase().endsWith("@gmail.com")
                || SYSTEM_GMAIL_ADDRESS.toLowerCase().endsWith("@gmail.com");

        if (isGmail) {
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        } else {
            // Localhost SMTP (port 25)
            props.put("mail.smtp.host", "localhost");
            props.put("mail.smtp.port", "25");
            props.put("mail.smtp.auth", "false");
        }

        final String authEmail = isGmail ? SYSTEM_GMAIL_ADDRESS : senderEmail;
        final String authPass = isGmail ? SYSTEM_GMAIL_APP_PASSWORD.replace(" ", "") : "";

        Session session;
        if (isGmail) {
            session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(authEmail, authPass);
                }
            });
        } else {
            session = Session.getInstance(props);
        }

        Message message = new MimeMessage(session);
        // Địa chỉ hiển thị người gửi
        message.setFrom(new InternetAddress(authEmail, senderEmail));
        message.setReplyTo(new Address[] { new InternetAddress(senderEmail) });
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mail.getReceiver()));
        message.setSubject(mail.getTitle() != null ? mail.getTitle() : "(Không tiêu đề)");

        // Tạo nội dung Multipart hỗ trợ văn bản + tệp đính kèm (ảnh)
        Multipart multipart = new MimeMultipart();

        // 1. Phần văn bản
        MimeBodyPart textBodyPart = new MimeBodyPart();
        textBodyPart.setText(mail.getContent() != null ? mail.getContent() : "", "UTF-8");
        multipart.addBodyPart(textBodyPart);

        // 2. Phần file đính kèm
        if (mail.getFiles() != null) {
            for (File file : mail.getFiles()) {
                if (file.exists()) {
                    MimeBodyPart attachPart = new MimeBodyPart();
                    attachPart.attachFile(file);
                    multipart.addBodyPart(attachPart);
                }
            }
        }

        message.setContent(multipart);
        Transport.send(message);
    }

    /**
     * Tải thư về từ hộp thư đến qua POP3 sử dụng tài khoản hệ thống đã cấu hình.
     */
    public static List<String> fetchInboxPOP3(int limit) throws Exception {
        List<String> result = new ArrayList<>();
        Properties props = new Properties();

        String host = "pop.gmail.com";
        int port = 995;

        props.put("mail.pop3.ssl.enable", "true");
        props.put("mail.pop3.host", host);
        props.put("mail.pop3.port", String.valueOf(port));
        props.put("mail.pop3.ssl.protocols", "TLSv1.2");

        Session session = Session.getDefaultInstance(props);
        Store store = session.getStore("pop3s");
        store.connect(host, port, SYSTEM_GMAIL_ADDRESS, SYSTEM_GMAIL_APP_PASSWORD.replace(" ", ""));

        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);

        Message[] messages = inbox.getMessages();
        int total = messages.length;
        int start = Math.max(1, total - limit + 1);

        for (int i = total - 1; i >= start - 1; i--) {
            Message msg = messages[i];
            StringBuilder sb = new StringBuilder();
            sb.append("──────────────────────────────────────────\n");
            sb.append("📩 Thư từ: ").append(InternetAddress.toString(msg.getFrom())).append("\n");
            sb.append("📅 Ngày nhận: ").append(msg.getSentDate()).append("\n");
            sb.append("📌 Tiêu đề: ").append(msg.getSubject()).append("\n");
            sb.append("💬 Nội dung xem nhanh:\n");
            try {
                Object content = msg.getContent();
                if (content instanceof String) {
                    sb.append(content).append("\n");
                } else if (content instanceof Multipart) {
                    Multipart mp = (Multipart) content;
                    for (int j = 0; j < mp.getCount(); j++) {
                        BodyPart bp = mp.getBodyPart(j);
                        if (bp.isMimeType("text/plain")) {
                            sb.append(bp.getContent()).append("\n");
                            break;
                        }
                    }
                }
            } catch (Exception ex) {
                sb.append("[Không thể đọc preview nội dung: ").append(ex.getMessage()).append("]\n");
            }
            result.add(sb.toString());
        }

        inbox.close(false);
        store.close();
        return result;
    }
}
