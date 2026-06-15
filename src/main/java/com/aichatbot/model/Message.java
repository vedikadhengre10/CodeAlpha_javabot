package com.aichatbot.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single chat message
 */
public class Message {

    public enum Sender { USER, BOT, SYSTEM }

    private final String content;
    private final Sender sender;
    private final LocalDateTime timestamp;
    private final double confidence;
    private final String intent;

    public Message(String content, Sender sender) {
        this(content, sender, 1.0, "general");
    }

    public Message(String content, Sender sender, double confidence, String intent) {
        this.content = content;
        this.sender = sender;
        this.timestamp = LocalDateTime.now();
        this.confidence = confidence;
        this.intent = intent;
    }

    public String getContent()   { return content; }
    public Sender getSender()    { return sender; }
    public double getConfidence(){ return confidence; }
    public String getIntent()    { return intent; }

    public String getTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public String toJson() {
        return String.format(
            "{\"sender\":\"%s\",\"content\":\"%s\",\"timestamp\":\"%s\",\"confidence\":%.2f,\"intent\":\"%s\"}",
            sender.name().toLowerCase(),
            content.replace("\"", "\\\"").replace("\n", "\\n"),
            getTimestamp(),
            confidence,
            intent
        );
    }
}
