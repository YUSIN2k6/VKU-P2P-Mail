package com.vku.app.view;

import com.vku.app.controller.MailController;
import com.vku.app.controller.MailController.SendMode;
import com.vku.app.model.MailMessage;
import com.vku.app.model.UserAccount;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MailClientGUI extends JFrame {
    private final MailController controller;

    // Các thành phần bên trái (Soạn thư)
    private JTextField txtTo, txtTitle, txtAttachedFiles;
    private JTextArea txtMessage;
    private JButton btnLogout, btnSend, btnAddFile, btnOpenStorage;

    // 3 lựa chọn hình thức gửi
    private JRadioButton rdoBoth, rdoOnlyEmail, rdoOnlyTCP;
    private ButtonGroup btnGroupMode;

    // Các thành phần bên phải (Danh sách Thẻ Thư)
    private JPanel mailListContainer;
    private JScrollPane scrollMailList;

    private List<File> attachedFilesList = new ArrayList<>();
    private final Font mainFont = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font boldFont = new Font("Segoe UI", Font.BOLD, 13);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public MailClientGUI(MailController controller) {
        this.controller = controller;
        UserAccount user = controller.getCurrentUser();

        setTitle("VKU Mail Client (TCP & SMTP/POP3) - [" + user.getEmail() + "]");

        // Mở toàn màn hình
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(1050, 700));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // 1. TOP PANEL: Thông tin người dùng & Nút Đăng xuất
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new EmptyBorder(0, 0, 5, 0));

        JLabel lblWelcome = new JLabel("Xin chào: " + user.getUsername() + " (" + user.getEmail() + ")");
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblWelcome.setForeground(new Color(0, 102, 204));
        topPanel.add(lblWelcome, BorderLayout.WEST);

        btnLogout = new JButton("Đăng xuất");
        btnLogout.setFont(boldFont);
        topPanel.add(btnLogout, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // =========================================================================
        // 2. KHU VỰC BÊN TRÁI: SOẠN THƯ / GỬI TIN NHẮN
        // =========================================================================
        JPanel leftPanel = new JPanel(new BorderLayout(0, 10));
        leftPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), " Soạn thư mới ",
                        TitledBorder.LEFT, TitledBorder.TOP, boldFont),
                new EmptyBorder(10, 10, 10, 10)));

        JPanel headerSendPanel = new JPanel(new GridLayout(4, 1, 0, 6));

        JPanel toPanel = new JPanel(new BorderLayout(10, 0));
        toPanel.add(new JLabel("Gửi tới (Email):"), BorderLayout.WEST);
        txtTo = new JTextField();
        txtTo.setFont(mainFont);
        toPanel.add(txtTo, BorderLayout.CENTER);

        JPanel titlePanel = new JPanel(new BorderLayout(25, 0));
        titlePanel.add(new JLabel("Tiêu đề:"), BorderLayout.WEST);
        txtTitle = new JTextField();
        txtTitle.setFont(mainFont);
        titlePanel.add(txtTitle, BorderLayout.CENTER);

        JPanel attachPanel = new JPanel(new BorderLayout(23, 0));
        attachPanel.add(new JLabel("Đính kèm:"), BorderLayout.WEST);
        txtAttachedFiles = new JTextField();
        txtAttachedFiles.setFont(mainFont);
        txtAttachedFiles.setEditable(false);
        txtAttachedFiles.setForeground(Color.BLUE);
        attachPanel.add(txtAttachedFiles, BorderLayout.CENTER);

        // 3 LỰA CHỌN GỬI BẰNG JRADIOBUTTON (Mặc định chọn "Chỉ gửi qua TCP Server")
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        rdoBoth = new JRadioButton("Gửi qua Email và TCP Server");
        rdoOnlyEmail = new JRadioButton("Chỉ gửi qua Email");
        rdoOnlyTCP = new JRadioButton("Chỉ gửi qua TCP Server", true);

        Font rdoFont = new Font("Segoe UI", Font.PLAIN, 12);
        rdoBoth.setFont(rdoFont);
        rdoOnlyEmail.setFont(rdoFont);
        rdoOnlyTCP.setFont(rdoFont);

        btnGroupMode = new ButtonGroup();
        btnGroupMode.add(rdoBoth);
        btnGroupMode.add(rdoOnlyEmail);
        btnGroupMode.add(rdoOnlyTCP);

        modePanel.add(rdoBoth);
        modePanel.add(rdoOnlyEmail);
        modePanel.add(rdoOnlyTCP);

        headerSendPanel.add(toPanel);
        headerSendPanel.add(titlePanel);
        headerSendPanel.add(attachPanel);
        headerSendPanel.add(modePanel);
        leftPanel.add(headerSendPanel, BorderLayout.NORTH);

        txtMessage = new JTextArea();
        txtMessage.setFont(mainFont);
        txtMessage.setLineWrap(true);
        txtMessage.setWrapStyleWord(true);
        leftPanel.add(new JScrollPane(txtMessage), BorderLayout.CENTER);

        JPanel bottomSendPanel = new JPanel(new GridLayout(1, 3, 8, 0));
        btnSend = new JButton("Gửi Email & File");
        btnSend.setFont(boldFont);

        btnAddFile = new JButton("Đính kèm File/Ảnh");
        btnAddFile.setFont(boldFont);

        btnOpenStorage = new JButton("Mở Storage");
        btnOpenStorage.setFont(boldFont);

        bottomSendPanel.add(btnSend);
        bottomSendPanel.add(btnAddFile);
        bottomSendPanel.add(btnOpenStorage);
        leftPanel.add(bottomSendPanel, BorderLayout.SOUTH);

        // =========================================================================
        // 3. KHU VỰC BÊN PHẢI: HIỂN THỊ CÁC THẺ THƯ (GỬI VÀ NHẬN) + CUỘN LỊCH SỬ
        // =========================================================================
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY),
                        " Hộp thư & Lịch sử thư đã gửi / nhận ",
                        TitledBorder.LEFT, TitledBorder.TOP, boldFont),
                new EmptyBorder(5, 5, 5, 5)));

        mailListContainer = new JPanel();
        mailListContainer.setLayout(new BoxLayout(mailListContainer, BoxLayout.Y_AXIS));
        mailListContainer.setBackground(new Color(245, 245, 245));

        scrollMailList = new JScrollPane(mailListContainer);
        scrollMailList.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollMailList.getVerticalScrollBar().setUnitIncrement(22);
        scrollMailList.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        rightPanel.add(scrollMailList, BorderLayout.CENTER);

        // =========================================================================
        // 4. CHIA 2 CỘT TRÁI - PHẢI BẰNG JSPLITPANE
        // =========================================================================
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setResizeWeight(0.42);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);
        add(splitPane, BorderLayout.CENTER);

        // 1. Tự động nạp lịch sử đã lưu từ storage/<prefix>.txt
        loadHistoryCards();

        // 2. Tự động kiểm tra và kéo thư mới từ POP3 chạy nền
        autoFetchPOP3Async();

        // 3. Lắng nghe TCP mail đến real-time
        controller.startTCPListening(this::onNewTCPMailReceived);

        // Sự kiện các nút bấm
        btnLogout.addActionListener(e -> logout());
        btnSend.addActionListener(e -> sendEmailAction());
        btnAddFile.addActionListener(e -> chooseFilesToAttach());
        btnOpenStorage.addActionListener(e -> openStorageFolder());
    }

    /**
     * Nạp lịch sử các thư đã lưu (Thư mới nhất hiển thị ở trên cùng)
     */
    private void loadHistoryCards() {
        List<MailMessage> history = controller.loadSavedMailHistory();
        String myEmail = controller.getCurrentUser().getEmail();
        // Duyệt theo thứ tự để thêm vào vị trí đầu tiên (index 0)
        for (MailMessage mail : history) {
            boolean isOutgoing = mail.getSender() != null && mail.getSender().equalsIgnoreCase(myEmail);
            MailCardPanel card = new MailCardPanel(mail, isOutgoing);
            // Chèn vào vị trí 0 (trên cùng), đẩy các tin cũ xuống dưới
            mailListContainer.add(Box.createVerticalStrut(10), 0);
            mailListContainer.add(card, 0);
        }
        mailListContainer.revalidate();
        mailListContainer.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollMailList.getVerticalScrollBar();
            vertical.setValue(0); // Cuộn lên trên cùng để xem thư mới nhất
        });
    }

    /**
     * Tự động tải thư mới từ POP3 chạy nền ngầm
     */
    private void autoFetchPOP3Async() {
        new Thread(() -> {
            try {
                List<MailMessage> pop3Messages = controller.fetchInboxPOP3(5);
                if (pop3Messages != null && !pop3Messages.isEmpty()) {
                    for (MailMessage msg : pop3Messages) {
                        addMailCard(msg, false);
                    }
                }
            } catch (Exception ignored) {
            }
        }).start();
    }

    /**
     * Thêm thẻ thư mới lên TRÊN CÙNG của khung bên phải (Đẩy các tin cũ xuống dưới)
     */
    public void addMailCard(MailMessage mail, boolean isOutgoing) {
        SwingUtilities.invokeLater(() -> {
            MailCardPanel card = new MailCardPanel(mail, isOutgoing);
            // Chèn vào vị trí 0 (đỉnh đầu danh sách), đẩy toàn bộ thẻ cũ xuống dưới
            mailListContainer.add(Box.createVerticalStrut(10), 0);
            mailListContainer.add(card, 0);

            mailListContainer.revalidate();
            mailListContainer.repaint();

            // Tự động cuộn mượt lên trên cùng để thấy ngay thư vừa gửi/nhận
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = scrollMailList.getVerticalScrollBar();
                vertical.setValue(0);
            });
        });
    }

    private void onNewTCPMailReceived(MailMessage mail) {
        if (mail.getSendingTime() == null || mail.getSendingTime().isEmpty()) {
            mail.setSendingTime(dateFormat.format(new Date()));
        }
        if (mail.getReceiver() == null || mail.getReceiver().isEmpty()) {
            mail.setReceiver(controller.getCurrentUser().getEmail());
        }
        addMailCard(mail, false);
    }

    private void chooseFilesToAttach() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            for (File file : fileChooser.getSelectedFiles()) {
                if (!attachedFilesList.contains(file)) {
                    attachedFilesList.add(file);
                }
            }
            updateAttachedFilesDisplay();
        }
    }

    private void updateAttachedFilesDisplay() {
        if (attachedFilesList.isEmpty()) {
            txtAttachedFiles.setText("");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < attachedFilesList.size(); i++) {
            sb.append(attachedFilesList.get(i).getName());
            if (i < attachedFilesList.size() - 1)
                sb.append(", ");
        }
        txtAttachedFiles.setText(sb.toString());
    }

    private void sendEmailAction() {
        String to = txtTo.getText().trim();
        if (to.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập Email người nhận!", "Cảnh báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String title = txtTitle.getText().trim();
        String content = txtMessage.getText().trim();

        SendMode mode;
        String modeName;
        if (rdoBoth.isSelected()) {
            mode = SendMode.BOTH_EMAIL_AND_TCP;
            modeName = "Email & TCP Server";
        } else if (rdoOnlyEmail.isSelected()) {
            mode = SendMode.ONLY_EMAIL;
            modeName = "Email (SMTP)";
        } else {
            mode = SendMode.ONLY_TCP;
            modeName = "TCP Server";
        }

        final String finalModeName = modeName;
        btnSend.setEnabled(false);
        btnSend.setText("Đang gửi (" + modeName + ")...");

        new Thread(() -> {
            try {
                controller.sendMailWithMode(to, title, content, attachedFilesList, mode);

                String currentTime = dateFormat.format(new Date());
                MailMessage outgoingMail = new MailMessage(currentTime, controller.getCurrentUser().getEmail(), to, title, content);
                if (!attachedFilesList.isEmpty()) {
                    outgoingMail.setFiles(new ArrayList<>(attachedFilesList));
                }

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            "Đã gửi thành công qua " + finalModeName + "!\nDữ liệu đã được lưu trữ.",
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);

                    // ĐẨY THẺ THƯ MỚI GỬI LÊN TRÊN CÙNG
                    addMailCard(outgoingMail, true);

                    txtTitle.setText("");
                    txtMessage.setText("");
                    attachedFilesList.clear();
                    updateAttachedFilesDisplay();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Lỗi gửi: " + ex.getMessage(), "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                SwingUtilities.invokeLater(() -> {
                    btnSend.setEnabled(true);
                    btnSend.setText("Gửi Email & File");
                });
            }

        }).start();
    }

    private void openStorageFolder() {
        try {
            File dir = controller.getStorageFolder();
            if (dir != null && dir.exists()) {
                Desktop.getDesktop().open(dir);
            } else {
                JOptionPane.showMessageDialog(this, "Thư mục chưa tồn tại.", "Thông báo",
                        JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Không thể mở thư mục: " + ex.getMessage(), "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void logout() {
        controller.logout();
        this.dispose();
        SwingUtilities.invokeLater(() -> new AuthGUI().setVisible(true));
    }
}
