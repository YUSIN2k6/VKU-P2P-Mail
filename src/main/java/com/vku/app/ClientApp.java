package com.vku.app;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class ClientApp {
    public static void main(String[] args) {
        // Thiết lập giao diện theo mặc định của hệ điều hành (Windows/Mac) cho đẹp hơn
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Khởi chạy Client an toàn trên luồng đồ họa (EDT)
        SwingUtilities.invokeLater(() -> {
            MailClientGUI clientFrame = new MailClientGUI();
            clientFrame.setVisible(true);
        });
    }
}