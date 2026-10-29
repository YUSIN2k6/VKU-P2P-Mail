package com.vku.app;

import com.vku.app.view.AuthGUI;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class ClientApp {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            AuthGUI authFrame = new AuthGUI();
            authFrame.setVisible(true);
        });
    }
}
