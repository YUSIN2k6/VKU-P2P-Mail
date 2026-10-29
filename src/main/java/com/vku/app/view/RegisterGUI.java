package com.vku.app.view;

import com.vku.app.controller.MailController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class RegisterGUI extends JFrame {
    private JTextField txtUsername, txtEmail, txtPassword;
    private JButton btnRegister, btnBackToLogin;
    private final MailController controller;
    private final JFrame parentLoginFrame;

    public RegisterGUI(MailController controller, JFrame parentLoginFrame) {
        this.controller = controller;
        this.parentLoginFrame = parentLoginFrame;

        setTitle("VKU Mail - Đăng Ký Tài Khoản");
        setSize(430, 310);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        // Tiêu đề phía trên
        JLabel lblHeader = new JLabel("TẠO TÀI KHOẢN MỚI", SwingConstants.CENTER);
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblHeader.setForeground(new Color(0, 102, 204));
        mainPanel.add(lblHeader, BorderLayout.NORTH);

        // Form nhập liệu (3 trường: Tên đăng nhập, Email, Mật khẩu - Textfield bình thường không ẩn)
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 12));
        Font font = new Font("Segoe UI", Font.PLAIN, 14);

        formPanel.add(new JLabel("Tên đăng nhập:"));
        txtUsername = new JTextField();
        txtUsername.setFont(font);
        formPanel.add(txtUsername);

        formPanel.add(new JLabel("Địa chỉ Email:"));
        txtEmail = new JTextField();
        txtEmail.setFont(font);
        formPanel.add(txtEmail);

        formPanel.add(new JLabel("Mật khẩu:"));
        txtPassword = new JTextField();
        txtPassword.setFont(font);
        formPanel.add(txtPassword);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Nút hành động phía dưới
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        btnRegister = new JButton("Đăng Ký Ngay");
        btnRegister.setFont(new Font("Segoe UI", Font.BOLD, 13));

        btnBackToLogin = new JButton("Quay lại Đăng nhập");
        btnBackToLogin.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        buttonPanel.add(btnRegister);
        buttonPanel.add(btnBackToLogin);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Sự kiện
        btnRegister.addActionListener(e -> attemptRegister());
        btnBackToLogin.addActionListener(e -> backToLogin());

        // Khi đóng cửa sổ đăng ký thì mở lại đăng nhập
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                backToLogin();
            }
        });
    }

    private void attemptRegister() {
        String username = txtUsername.getText().trim();
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ Tên đăng nhập, Email và Mật khẩu!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!email.contains("@")) {
            JOptionPane.showMessageDialog(this, "Email không hợp lệ (cần có ký tự @)!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        StringBuilder errorMsg = new StringBuilder();
        boolean ok = controller.register(username, email, password, errorMsg);
        if (ok) {
            String folder = email.substring(0, email.indexOf("@"));
            JOptionPane.showMessageDialog(this, "Đăng ký thành công!\nHộp thư của bạn được tạo tại: storage/" + folder, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            backToLogin();
        } else {
            JOptionPane.showMessageDialog(this, errorMsg.toString(), "Lỗi đăng ký", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void backToLogin() {
        this.dispose();
        if (parentLoginFrame != null) {
            parentLoginFrame.setVisible(true);
        }
    }
}

