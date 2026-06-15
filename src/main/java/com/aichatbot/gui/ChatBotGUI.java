package com.aichatbot.gui;

import com.aichatbot.model.Message;
import com.aichatbot.service.ChatBotService;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Desktop GUI using Swing with a modern dark-themed chat interface.
 */
public class ChatBotGUI extends JFrame {

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final Color BG_DARK      = new Color(18, 18, 28);
    private static final Color BG_PANEL     = new Color(26, 26, 40);
    private static final Color BG_INPUT     = new Color(35, 35, 52);
    private static final Color ACCENT       = new Color(99, 102, 241);   // indigo
    private static final Color ACCENT_LIGHT = new Color(129, 140, 248);
    private static final Color BOT_BUBBLE   = new Color(40, 40, 62);
    private static final Color USER_BUBBLE  = new Color(79, 70, 229);
    private static final Color TEXT_PRIMARY = new Color(240, 240, 255);
    private static final Color TEXT_MUTED   = new Color(140, 140, 180);
    private static final Color SUCCESS      = new Color(52, 211, 153);

    private final ChatBotService service;
    private JTextPane chatPane;
    private JTextField inputField;
    private JLabel statusLabel;
    private JLabel typingLabel;
    private final javax.swing.text.StyledDocument doc;
    private Timer typingTimer;

    public ChatBotGUI(ChatBotService service) {
        this.service = service;
        setTitle("🤖 AI ChatBot — JavaBot");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(750, 680);
        setMinimumSize(new Dimension(550, 500));
        setLocationRelativeTo(null);
        setBackground(BG_DARK);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG_DARK);
        root.add(buildHeader(),  BorderLayout.NORTH);
        root.add(buildChat(),    BorderLayout.CENTER);
        root.add(buildInput(),   BorderLayout.SOUTH);
        setContentPane(root);

        doc = chatPane.getStyledDocument();
        showWelcome();

        // Enter key
        inputField.addActionListener(e -> sendMessage());
        inputField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { updateSendBtn(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { updateSendBtn(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateSendBtn(); }
        });
    }

    // ── UI Builders ───────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(BG_PANEL);
        hdr.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, new Color(60, 60, 90)),
            new EmptyBorder(14, 20, 14, 20)
        ));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        // Avatar circle
        JLabel avatar = new JLabel("🤖") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT);
                g2.fillOval(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        avatar.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        avatar.setPreferredSize(new Dimension(40, 40));

        JPanel info = new JPanel(new GridLayout(2, 1));
        info.setOpaque(false);
        JLabel nameLabel = new JLabel("JavaBot AI");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        nameLabel.setForeground(TEXT_PRIMARY);
        statusLabel = new JLabel("● Online");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(SUCCESS);
        info.add(nameLabel);
        info.add(statusLabel);

        left.add(avatar);
        left.add(info);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        right.add(iconBtn("📊", "Stats", () -> showStats()));
        right.add(iconBtn("🗑️", "Clear", this::clearChat));

        hdr.add(left,  BorderLayout.WEST);
        hdr.add(right, BorderLayout.EAST);
        return hdr;
    }

    private JScrollPane buildChat() {
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setBackground(BG_DARK);
        chatPane.setBorder(new EmptyBorder(12, 12, 12, 12));
        chatPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JScrollPane scroll = new JScrollPane(chatPane);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setBackground(BG_DARK);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    private JPanel buildInput() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(BG_PANEL);
        footer.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, new Color(60, 60, 90)),
            new EmptyBorder(10, 14, 14, 14)
        ));

        typingLabel = new JLabel(" ");
        typingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        typingLabel.setForeground(TEXT_MUTED);
        typingLabel.setBorder(new EmptyBorder(0, 4, 4, 0));

        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);

        inputField = new JTextField();
        inputField.setBackground(BG_INPUT);
        inputField.setForeground(TEXT_PRIMARY);
        inputField.setCaretColor(ACCENT_LIGHT);
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 70, 100), 1, true),
            new EmptyBorder(9, 14, 9, 14)
        ));

        JButton sendBtn = new JButton("Send ➤");
        sendBtn.setBackground(ACCENT);
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sendBtn.setFocusPainted(false);
        sendBtn.setBorder(new EmptyBorder(9, 18, 9, 18));
        sendBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendBtn.addActionListener(e -> sendMessage());
        sendBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { sendBtn.setBackground(ACCENT_LIGHT); }
            public void mouseExited(MouseEvent e)  { sendBtn.setBackground(ACCENT); }
        });

        inputRow.add(inputField, BorderLayout.CENTER);
        inputRow.add(sendBtn, BorderLayout.EAST);

        footer.add(typingLabel, BorderLayout.NORTH);
        footer.add(inputRow, BorderLayout.CENTER);
        return footer;
    }

    // ── Chat Logic ────────────────────────────────────────────────────────────

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        inputField.setText("");

        appendBubble(text, Message.Sender.USER);
        showTyping();

        Timer delay = new Timer(600 + (int)(Math.random() * 600), e -> {
            hideTyping();
            Message response = service.processInput(text);
            appendBubble(response.getContent(), Message.Sender.BOT);
            appendMeta(response);
        });
        delay.setRepeats(false);
        delay.start();
    }

    private void appendBubble(String text, Message.Sender sender) {
        try {
            boolean isBot = sender == Message.Sender.BOT;
            javax.swing.text.SimpleAttributeSet align = new javax.swing.text.SimpleAttributeSet();
            javax.swing.text.StyleConstants.setAlignment(align,
                isBot ? javax.swing.text.StyleConstants.ALIGN_LEFT : javax.swing.text.StyleConstants.ALIGN_RIGHT);

            // Bubble block
            javax.swing.text.SimpleAttributeSet bubble = new javax.swing.text.SimpleAttributeSet();
            javax.swing.text.StyleConstants.setBackground(bubble, isBot ? BOT_BUBBLE : USER_BUBBLE);
            javax.swing.text.StyleConstants.setForeground(bubble, TEXT_PRIMARY);
            javax.swing.text.StyleConstants.setFontFamily(bubble, "Segoe UI");
            javax.swing.text.StyleConstants.setFontSize(bubble, 13);
            javax.swing.text.StyleConstants.setSpaceAbove(bubble, 4);
            javax.swing.text.StyleConstants.setSpaceBelow(bubble, 4);

            String prefix = isBot ? "🤖  " : "👤  ";
            int end = doc.getLength();
            doc.setParagraphAttributes(end, 1, align, false);
            doc.insertString(end, prefix + text + "\n", bubble);
            scrollToBottom();
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void appendMeta(Message msg) {
        try {
            javax.swing.text.SimpleAttributeSet meta = new javax.swing.text.SimpleAttributeSet();
            javax.swing.text.StyleConstants.setForeground(meta, TEXT_MUTED);
            javax.swing.text.StyleConstants.setFontSize(meta, 10);
            javax.swing.text.StyleConstants.setFontFamily(meta, "Segoe UI");
            javax.swing.text.StyleConstants.setAlignment(meta, javax.swing.text.StyleConstants.ALIGN_LEFT);

            String bar = String.format("   intent: %s  |  confidence: %.0f%%  |  %s\n",
                msg.getIntent(), msg.getConfidence() * 100, msg.getTimestamp());
            doc.insertString(doc.getLength(), bar, meta);
            scrollToBottom();
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void showWelcome() {
        try {
            javax.swing.text.SimpleAttributeSet center = new javax.swing.text.SimpleAttributeSet();
            javax.swing.text.StyleConstants.setAlignment(center, javax.swing.text.StyleConstants.ALIGN_CENTER);
            javax.swing.text.StyleConstants.setForeground(center, TEXT_MUTED);
            javax.swing.text.StyleConstants.setFontSize(center, 12);
            doc.insertString(0, "─── JavaBot AI Chat ───\nType a message to begin\n\n", center);
        } catch (Exception e) { e.printStackTrace(); }

        Message welcome = service.processInput("hello");
        appendBubble(welcome.getContent(), Message.Sender.BOT);
    }

    private void showTyping() {
        typingLabel.setText("JavaBot is typing...");
        typingLabel.setForeground(ACCENT_LIGHT);
    }

    private void hideTyping() {
        typingLabel.setText(" ");
    }

    private void showStats() {
        String stats = service.getSessionStats();
        JOptionPane.showMessageDialog(this, stats, "Session Statistics", JOptionPane.INFORMATION_MESSAGE);
    }

    private void clearChat() {
        try {
            doc.remove(0, doc.getLength());
            showWelcome();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar sb = ((JScrollPane) chatPane.getParent().getParent()).getVerticalScrollBar();
            sb.setValue(sb.getMaximum());
        });
    }

    private void updateSendBtn() { /* placeholder */ }

    private JButton iconBtn(String icon, String tooltip, Runnable action) {
        JButton btn = new JButton(icon);
        btn.setToolTipText(tooltip);
        btn.setBackground(BG_INPUT);
        btn.setForeground(TEXT_MUTED);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(6, 10, 6, 10));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> action.run());
        return btn;
    }
}
