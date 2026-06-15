package com.aichatbot;

import com.aichatbot.gui.ChatBotGUI;
import com.aichatbot.service.ChatBotService;

/**
 * AI Chatbot Application - Main Entry Point
 * Supports both GUI mode and Web Server mode
 */
public class ChatBotApplication {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║      AI Chatbot - Initializing...    ║");
        System.out.println("╚══════════════════════════════════════╝");

        // Check if web mode or GUI mode
        boolean webMode = args.length > 0 && args[0].equalsIgnoreCase("--web");

        if (webMode) {
            System.out.println("[MODE] Starting Web Server on http://localhost:8080");
            ChatBotWebServer server = new ChatBotWebServer();
            server.start(8080);
        } else {
            System.out.println("[MODE] Starting Desktop GUI");
            ChatBotService service = new ChatBotService();
            javax.swing.SwingUtilities.invokeLater(() -> {
                new ChatBotGUI(service).setVisible(true);
            });
        }
    }
}
