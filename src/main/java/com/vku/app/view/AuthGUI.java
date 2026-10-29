package com.vku.app.view;

import com.vku.app.controller.MailController;
import com.vku.app.model.UserAccount;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AuthGUI extends JFrame {
    private JTextField txtEmail, txtPassword;
    private JButton btnLogin, btnOpenRegister;
    private final MailController controller;

    public AuthGUI() {
        this.controller = new MailController();

        setTitle("VKU Mail - Đăng Nhập");
        setSize(420, 270);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        // Tiêu đề phía trên
        JLabel lblHeader = new JLabel("ĐĂNG NHẬP HỆ THỐNG", SwingConstants.CENTER);
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblHeader.setForeground(new Color(0, 102, 204));
        mainPanel.add(lblHeader, BorderLayout.NORTH);

        // Form đăng nhập (2 trường: Email và Mật khẩu - Không bị ẩn)
        JPanel formPanel = new JPanel(new GridLayout(2, 2, 10, 15));
        Font font = new Font("Segoe UI", Font.PLAIN, 14);

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
        btnLogin = new JButton("Đăng Nhập");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 13));

        btnOpenRegister = new JButton("Tạo tài khoản mới");
        btnOpenRegister.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        buttonPanel.add(btnLogin);
        buttonPanel.add(btnOpenRegister);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Sự kiện
        btnLogin.addActionListener(e -> attemptLogin());
        btnOpenRegister.addActionListener(e -> openRegisterWindow());
    }

    private void attemptLogin() {
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập Email và Mật khẩu!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        StringBuilder errorMsg = new StringBuilder();
        UserAccount user = controller.login(email, password, errorMsg);
        if (user != null) {
            MailClientGUI clientGUI = new MailClientGUI(controller);
            clientGUI.setVisible(true);
            this.dispose();
        } else {
            JOptionPane.showMessageDialog(this, errorMsg.toString(), "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openRegisterWindow() {
        this.setVisible(false);
        RegisterGUI registerFrame = new RegisterGUI(controller, this);
        registerFrame.setVisible(true);
    }
}
