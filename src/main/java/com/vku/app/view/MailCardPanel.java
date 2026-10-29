package com.vku.app.view;

import com.vku.app.model.MailMessage;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class MailCardPanel extends JPanel {

    public MailCardPanel(MailMessage mail, boolean isOutgoing) {
        setLayout(new BorderLayout(5, 5));
        
        // Nền thẻ: bên gửi màu xanh nhạt, bên nhận màu trắng/xám nhẹ
        Color bgColor = isOutgoing ? new Color(240, 248, 255) : new Color(255, 255, 255);
        Color borderColor = isOutgoing ? new Color(180, 210, 240) : new Color(220, 220, 220);
        setBackground(bgColor);

        setBorder(new CompoundBorder(
                new LineBorder(borderColor, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));

        // 1. Header của Thư
        JPanel topHeader = new JPanel(new BorderLayout(5, 2));
        topHeader.setOpaque(false);

        String tag = isOutgoing ? "[THƯ BẠN ĐÃ GỬI]" : "[THƯ NHẬN ĐƯỢC]";
        JLabel lblTag = new JLabel(tag);
        lblTag.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTag.setForeground(isOutgoing ? new Color(0, 102, 204) : new Color(40, 167, 69));

        JLabel lblTime = new JLabel(mail.getSendingTime() != null ? mail.getSendingTime() : "");
        lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblTime.setForeground(Color.GRAY);

        topHeader.add(lblTag, BorderLayout.WEST);
        topHeader.add(lblTime, BorderLayout.EAST);

        // Thông tin người gửi / người nhận
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 0, 2));
        infoPanel.setOpaque(false);

        JLabel lblSender = new JLabel("Từ: " + (mail.getSender() != null ? mail.getSender() : ""));
        lblSender.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel lblReceiver = new JLabel("Đến: " + (mail.getReceiver() != null ? mail.getReceiver() : ""));
        lblReceiver.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel lblTitle = new JLabel("Tiêu đề: " + (mail.getTitle() != null ? mail.getTitle() : ""));
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));

        infoPanel.add(lblSender);
        infoPanel.add(lblReceiver);
        infoPanel.add(lblTitle);

        JPanel headerContainer = new JPanel(new BorderLayout(0, 4));
        headerContainer.setOpaque(false);
        headerContainer.add(topHeader, BorderLayout.NORTH);
        headerContainer.add(infoPanel, BorderLayout.CENTER);

        add(headerContainer, BorderLayout.NORTH);

        // 2. Nội dung thư
        JTextArea txtContent = new JTextArea(mail.getContent() != null ? mail.getContent() : "");
        txtContent.setEditable(false);
        txtContent.setLineWrap(true);
        txtContent.setWrapStyleWord(true);
        txtContent.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtContent.setOpaque(false);
        txtContent.setBorder(new EmptyBorder(8, 0, 8, 0));

        add(txtContent, BorderLayout.CENTER);

        // 3. File đính kèm & Nút Tải xuống
        if (mail.getFiles() != null && !mail.getFiles().isEmpty()) {
            JPanel filesPanel = new JPanel();
            filesPanel.setLayout(new BoxLayout(filesPanel, BoxLayout.Y_AXIS));
            filesPanel.setOpaque(false);
            filesPanel.setBorder(new CompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 230, 230)),
                    new EmptyBorder(6, 0, 2, 0)
            ));

            for (File file : mail.getFiles()) {
                JPanel itemRow = new JPanel(new BorderLayout(8, 0));
                itemRow.setOpaque(false);

                String name = file.getName();
                JLabel lblFileName = new JLabel("Tệp: " + name);
                lblFileName.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                itemRow.add(lblFileName, BorderLayout.CENTER);

                JButton btnDownload = new JButton("Tải xuống");
                btnDownload.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                btnDownload.setFocusPainted(false);
                btnDownload.addActionListener(e -> downloadFileToUserDownloads(file, this));

                itemRow.add(btnDownload, BorderLayout.EAST);
                filesPanel.add(itemRow);
                filesPanel.add(Box.createVerticalStrut(4));
            }
            add(filesPanel, BorderLayout.SOUTH);
        }

        // Tối đa chiều ngang, giãn theo chiều dọc
        setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));
    }

    private void downloadFileToUserDownloads(File sourceFile, Component parent) {
        if (sourceFile == null || !sourceFile.exists()) {
            JOptionPane.showMessageDialog(parent, "Không tìm thấy file nguồn!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Thư mục Downloads mặc định của người dùng trên Windows: C:\Users\<Username>\Downloads
            String userHome = System.getProperty("user.home");
            File downloadsDir = new File(userHome, "Downloads");
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }

            File targetFile = new File(downloadsDir, sourceFile.getName());
            
            // Nếu file đã tồn tại thì tự động đổi tên tránh ghi đè
            if (targetFile.exists()) {
                String name = sourceFile.getName();
                String baseName = name;
                String ext = "";
                int dotIdx = name.lastIndexOf(".");
                if (dotIdx > 0) {
                    baseName = name.substring(0, dotIdx);
                    ext = name.substring(dotIdx);
                }
                targetFile = new File(downloadsDir, baseName + "_" + System.currentTimeMillis() + ext);
            }

            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            int choice = JOptionPane.showConfirmDialog(parent,
                    "Đã tải thành công về:\n" + targetFile.getAbsolutePath() + "\n\nBạn có muốn mở thư mục Downloads không?",
                    "Tải xuống hoàn tất",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE);

            if (choice == JOptionPane.YES_OPTION) {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(downloadsDir);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Lỗi tải file: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}

