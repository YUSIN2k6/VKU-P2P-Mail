package com.vku.app;

import com.vku.app.view.MailServerGUI;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class ServerApp {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            MailServerGUI serverFrame = new MailServerGUI();
            serverFrame.setVisible(true);
        });
    }
}

