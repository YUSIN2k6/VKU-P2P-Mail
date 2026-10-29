package com.vku.app.network;

import com.vku.app.model.MailMessage;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EmailService {

    // =========================================================================
    // CẤU HÌNH HỆ THỐNG GỬI/NHẬN GMAIL (SMTP / POP3)
    // Email trường VKU dùng nền tảng Google Workspace (Gmail Server)
    // =========================================================================
    public static final String SYSTEM_GMAIL_ADDRESS = "tuannh.24itb@vku.udn.vn";
    public static final String SYSTEM_GMAIL_APP_PASSWORD = "owku szkn jtjy zpnz";
    // =========================================================================

    /**
     * Gửi Email qua SMTP Google.
     */
    public static void sendEmail(String senderEmail, MailMessage mail) throws Exception {
        Properties props = new Properties();

        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        final String authEmail = SYSTEM_GMAIL_ADDRESS;
        final String authPass = SYSTEM_GMAIL_APP_PASSWORD.replace(" ", "");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(authEmail, authPass);
            }
        });

        MimeMessage message = new MimeMessage(session);
        // Mã hóa tên người gửi và tiêu đề chuẩn UTF-8 tránh lỗi ký tự tiếng Việt (như
        // b?n nhé)
        message.setFrom(new InternetAddress(authEmail, senderEmail, "UTF-8"));
        message.setReplyTo(new Address[] { new InternetAddress(senderEmail) });
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mail.getReceiver()));

        String title = (mail.getTitle() != null && !mail.getTitle().isEmpty()) ? mail.getTitle() : "(Không tiêu đề)";
        message.setSubject(MimeUtility.encodeText(title, "UTF-8", "B"));

        Multipart multipart = new MimeMultipart();

        // 1. Nội dung văn bản với charset UTF-8
        MimeBodyPart textBodyPart = new MimeBodyPart();
        textBodyPart.setText(mail.getContent() != null ? mail.getContent() : "", "UTF-8");
        multipart.addBodyPart(textBodyPart);

        // 2. File đính kèm
        if (mail.getFiles() != null) {
            for (File file : mail.getFiles()) {
                if (file.exists()) {
                    MimeBodyPart attachPart = new MimeBodyPart();
                    attachPart.attachFile(file);
                    // Mã hóa tên file đính kèm chuẩn UTF-8
                    attachPart.setFileName(MimeUtility.encodeText(file.getName(), "UTF-8", null));
                    multipart.addBodyPart(attachPart);
                }
            }
        }

        message.setContent(multipart);
        Transport.send(message);
    }

    /**
     * Tải thư về từ hộp thư đến qua POP3 và chuyển thành danh sách MailMessage hoàn chỉnh.
     */
    public static List<MailMessage> fetchInboxPOP3Messages(int limit) throws Exception {
        List<MailMessage> result = new ArrayList<>();
        
        String cleanPass = SYSTEM_GMAIL_APP_PASSWORD.replace(" ", "");
        if (cleanPass.equalsIgnoreCase("xxxxxxxxxxxxxxxx") || cleanPass.length() < 8) {
            return result;
        }

        Properties props = new Properties();
        String host = "pop.gmail.com";
        int port = 995;

        props.put("mail.pop3.ssl.enable", "true");
        props.put("mail.pop3.host", host);
        props.put("mail.pop3.port", String.valueOf(port));
        props.put("mail.pop3.ssl.protocols", "TLSv1.2");

        Session session = Session.getDefaultInstance(props);
        Store store = session.getStore("pop3s");
        store.connect(host, port, SYSTEM_GMAIL_ADDRESS, cleanPass);

        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);

        Message[] messages = inbox.getMessages();
        int total = messages.length;
        int start = Math.max(1, total - limit + 1);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (int i = total - 1; i >= start - 1; i--) {
            Message msg = messages[i];
            try {
                String sender = InternetAddress.toString(msg.getFrom());
                String title = msg.getSubject() != null ? msg.getSubject() : "(Không tiêu đề)";
                String timeStr = msg.getSentDate() != null ? sdf.format(msg.getSentDate()) : "";
                
                StringBuilder contentSb = new StringBuilder();
                Object content = msg.getContent();
                if (content instanceof String) {
                    contentSb.append(content);
                } else if (content instanceof Multipart) {
                    Multipart mp = (Multipart) content;
                    for (int j = 0; j < mp.getCount(); j++) {
                        BodyPart bp = mp.getBodyPart(j);
                        if (bp.isMimeType("text/plain")) {
                            contentSb.append(bp.getContent());
                            break;
                        }
                    }
                }

                MailMessage m = new MailMessage(timeStr, sender, SYSTEM_GMAIL_ADDRESS, title, contentSb.toString().trim());
                result.add(m);
            } catch (Exception ignored) {}
        }

        inbox.close(false);
        store.close();
        return result;
    }
}
