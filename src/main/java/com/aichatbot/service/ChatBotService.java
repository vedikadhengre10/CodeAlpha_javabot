package com.aichatbot.service;

import com.aichatbot.model.Message;
import com.aichatbot.nlp.NLPEngine;

import java.util.*;

/**
 * ChatBot Service — orchestrates NLP, knowledge base lookup,
 * math evaluation, context tracking, and response generation.
 */
public class ChatBotService {

    private final NLPEngine nlp = new NLPEngine();
    private final KnowledgeBase kb = new KnowledgeBase();
    private final List<Message> conversationHistory = new ArrayList<>();
    private final Map<String, Integer> intentFrequency = new HashMap<>();
    private final Random random = new Random();

    // Context state
    private String lastIntent = "unknown";
    private int messageCount = 0;
    private String userName = null;

    // Trivia Game state
    public static class TriviaQuestion {
        public final String question;
        public final List<String> options;
        public final String correctAnswer;

        public TriviaQuestion(String question, List<String> options, String correctAnswer) {
            this.question = question;
            this.options = options;
            this.correctAnswer = correctAnswer;
        }
    }

    private final List<TriviaQuestion> triviaQuestions = Arrays.asList(
        new TriviaQuestion("What is the size of an int in Java?", Arrays.asList("A) 16-bit", "B) 32-bit", "C) 64-bit", "D) 8-bit"), "B"),
        new TriviaQuestion("Which keyword prevents a class from being subclassed in Java?", Arrays.asList("A) static", "B) abstract", "C) final", "D) private"), "C"),
        new TriviaQuestion("Which of these is NOT one of the 4 pillars of OOP?", Arrays.asList("A) Compilation", "B) Polymorphism", "C) Encapsulation", "D) Inheritance"), "A"),
        new TriviaQuestion("What is the default value of a boolean primitive in Java?", Arrays.asList("A) true", "B) false", "C) null", "D) 0"), "B"),
        new TriviaQuestion("Which Collection class maintains insertion order?", Arrays.asList("A) HashSet", "B) HashMap", "C) LinkedHashSet", "D) TreeMap"), "C"),
        new TriviaQuestion("What does JVM stand for?", Arrays.asList("A) Java Virtual Machine", "B) Java Variable Method", "C) Java Visual Manager", "D) Joint Vector Machine"), "A"),
        new TriviaQuestion("Which of the following is a checked exception?", Arrays.asList("A) NullPointerException", "B) IOException", "C) ArithmeticException", "D) ArrayIndexOutOfBoundsException"), "B"),
        new TriviaQuestion("Which keyword accesses members of the superclass?", Arrays.asList("A) parent", "B) this", "C) super", "D) extends"), "C"),
        new TriviaQuestion("What is the purpose of garbage collection in Java?", Arrays.asList("A) Disk optimization", "B) Reclaims unused heap memory", "C) Cleans temp files", "D) Compiles code"), "B"),
        new TriviaQuestion("Which interface allows sorting objects using Collections.sort()?", Arrays.asList("A) Comparator", "B) Runnable", "C) Comparable", "D) Serializable"), "C")
    );

    private boolean inTriviaMode = false;
    private int currentTriviaIndex = 0;
    private int triviaScore = 0;
    private final List<TriviaQuestion> activeTriviaSession = new ArrayList<>();

    // Sentiment session tracking
    private int sentimentSum = 0;
    private int sentimentCount = 0;

    // ── Public API ─────────────────────────────────────────────────────────────

    public Message processInput(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return botMessage("Please type something! I'm listening... 👂", 0.9, "empty");
        }

        // If currently in trivia mode, intercept and process as game input
        if (inTriviaMode) {
            return record(processTriviaInput(userInput));
        }

        // Store user message
        String normalInput = nlp.normalize(userInput);
        messageCount++;
        Message userMsg = new Message(userInput, Message.Sender.USER);
        conversationHistory.add(userMsg);

        // Extract user's name if introduced
        detectName(normalInput);

        // Intent & sentiment
        String intent = nlp.detectIntent(normalInput);
        double confidence = nlp.computeConfidence(normalInput, intent);
        int sentiment = nlp.analyzeSentiment(normalInput);

        // Track sentiment for session average
        sentimentSum += sentiment;
        sentimentCount++;

        // Track intent frequency
        intentFrequency.merge(intent, 1, Integer::sum);

        // Check if starting trivia game
        if (normalInput.matches(".*\\b(play|start|game|quiz)\\b.*") && normalInput.contains("trivia")) {
            inTriviaMode = true;
            currentTriviaIndex = 0;
            triviaScore = 0;
            activeTriviaSession.clear();
            
            List<TriviaQuestion> shuffled = new ArrayList<>(triviaQuestions);
            Collections.shuffle(shuffled);
            activeTriviaSession.addAll(shuffled.subList(0, 5));

            TriviaQuestion first = activeTriviaSession.get(0);
            StringBuilder sb = new StringBuilder();
            sb.append("🎮 *Starting Java Trivia Game!* Answer with **A**, **B**, **C**, or **D** (or type **quit** to exit).\n\n");
            sb.append("### Question 1/5:\n");
            sb.append(first.question).append("\n\n");
            for (String option : first.options) {
                sb.append("• ").append(option).append("\n");
            }
            return record(botMessage(sb.toString().trim(), 0.99, "trivia"));
        }

        // ── Math shortcut ────────────────────────────────────────────────────
        Optional<Double> mathResult = nlp.extractAndEvaluateMath(normalInput);
        if (mathResult.isPresent()) {
            double result = mathResult.get();
            String formatted = (result == Math.floor(result))
                ? String.valueOf((long) result)
                : String.format("%.4f", result);
            return record(botMessage("🧮 Result: " + formatted, 0.99, "math"));
        }

        // ── Sentiment-reactive opener ─────────────────────────────────────────
        String opener = buildOpener(sentiment, intent);

        // ── Knowledge base FAQ match ──────────────────────────────────────────
        Optional<KnowledgeBase.KnowledgeEntry> faqMatch = kb.findBestMatch(normalInput, nlp);
        if (faqMatch.isPresent()) {
            KnowledgeBase.KnowledgeEntry entry = faqMatch.get();
            String resp = pickRandom(entry.responses);
            String full = opener.isEmpty() ? resp : opener + "\n\n" + resp;
            lastIntent = entry.intent;
            return record(botMessage(full, Math.max(confidence, 0.82), entry.intent));
        }

        // ── Intent-based response ────────────────────────────────────────────
        Optional<String> intentResp = kb.getIntentResponse(intent);
        if (intentResp.isPresent()) {
            // For time intent, regenerate dynamically
            String resp = intent.equals("time")
                ? "⏰ Current time: " + java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy 'at' HH:mm:ss"))
                : intentResp.get();
            String full = opener.isEmpty() ? resp : opener + "\n\n" + resp;
            lastIntent = intent;
            return record(botMessage(full, confidence, intent));
        }

        // ── Context-aware fallback ───────────────────────────────────────────
        String fallback = buildContextualFallback(normalInput, intent, sentiment);
        lastIntent = intent;
        return record(botMessage(fallback, 0.35, "unknown"));
    }

    public List<Message> getHistory()      { return Collections.unmodifiableList(conversationHistory); }
    public int getMessageCount()           { return messageCount; }
    public String getMostCommonIntent()    {
        return intentFrequency.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey).orElse("none");
    }
    public String getUserName()            { return userName; }

    /** Generate a session summary */
    public String getSessionStats() {
        return String.format(
            "📊 Session Stats:\n• Messages: %d\n• Most discussed: %s\n• User: %s",
            messageCount,
            getMostCommonIntent(),
            userName != null ? userName : "Anonymous"
        );
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String buildOpener(int sentiment, String intent) {
        if (sentiment == -1 && !intent.equals("farewell")) {
            return pickRandom(List.of(
                "I sense some frustration — let me do my best to help! 💪",
                "I hear you — let's sort this out together.",
                "No worries, I'm here to help! Let's tackle this."
            ));
        }
        if (sentiment == 1 && messageCount > 1) {
            return pickRandom(List.of("", "", "Great energy! 😊", "Love the enthusiasm! ✨", ""));
        }
        // Personalized greeting after first message
        if (messageCount == 1 && userName != null) {
            return "Nice to meet you, " + userName + "!";
        }
        return "";
    }

    private String buildContextualFallback(String input, String intent, int sentiment) {
        // TF-IDF similarity against all FAQ triggers
        double bestSim = 0;
        KnowledgeBase.KnowledgeEntry bestEntry = null;
        for (KnowledgeBase.KnowledgeEntry entry : kb.getAllEntries()) {
            for (String trigger : entry.triggers) {
                double sim = nlp.cosineSimilarity(input, trigger);
                if (sim > bestSim) { bestSim = sim; bestEntry = entry; }
            }
        }
        if (bestSim > 0.25 && bestEntry != null) {
            return "Did you mean something about '" + bestEntry.intent + "'? "
                + pickRandom(bestEntry.responses);
        }

        List<String> fallbacks = List.of(
            "Hmm, I'm not sure about that one. Try asking about Java, AI, math calculations, or type 'help'! 🤔",
            "Interesting question! I'm still expanding my knowledge. Could you rephrase, or ask something else?",
            "I don't have a great answer for that yet. You could ask me: 'what is Java', 'tell me a joke', or 'calculate 25 * 4'.",
            "That's outside my current knowledge base. But try: 'what can you do' to see what I know! 🚀"
        );
        return pickRandom(fallbacks);
    }

    private void detectName(String input) {
        if (userName != null) return;
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("(?:my name is|i am|i'm|call me)\\s+([a-zA-Z]+)", java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(input);
        if (m.find()) userName = capitalize(m.group(1));
    }

    private Message botMessage(String content, double confidence, String intent) {
        return new Message(content, Message.Sender.BOT, confidence, intent);
    }

    private Message record(Message msg) {
        conversationHistory.add(msg);
        return msg;
    }

    private String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private String pickRandom(List<String> list) {
        return list.get(random.nextInt(list.size()));
    }

    public String getSessionSentiment() {
        if (sentimentCount == 0) return "neutral";
        double avg = (double) sentimentSum / sentimentCount;
        if (avg >= 0.25) return "positive";
        if (avg <= -0.25) return "negative";
        return "neutral";
    }

    private Message processTriviaInput(String userInput) {
        String answer = userInput.trim().toUpperCase();
        if (answer.equals("QUIT") || answer.equals("EXIT")) {
            inTriviaMode = false;
            return botMessage("👋 You have exited the trivia game. Your final score was **" + triviaScore + "/" + activeTriviaSession.size() + "**. ", 0.99, "trivia");
        }

        if (!answer.equals("A") && !answer.equals("B") && !answer.equals("C") && !answer.equals("D")) {
            return botMessage("⚠️ Invalid choice. Please reply with **A**, **B**, **C**, or **D** (or type **quit** to exit).", 0.99, "trivia");
        }

        TriviaQuestion currentQuestion = activeTriviaSession.get(currentTriviaIndex);
        boolean correct = answer.equals(currentQuestion.correctAnswer);
        if (correct) {
            triviaScore++;
        }

        String resultFeedback = correct 
            ? "✅ **Correct!** The answer was indeed **" + currentQuestion.correctAnswer + "**." 
            : "❌ **Incorrect.** The correct answer was **" + currentQuestion.correctAnswer + "**.";

        currentTriviaIndex++;

        if (currentTriviaIndex < activeTriviaSession.size()) {
            TriviaQuestion nextQuestion = activeTriviaSession.get(currentTriviaIndex);
            StringBuilder sb = new StringBuilder();
            sb.append(resultFeedback).append("\n\n");
            sb.append("### Question ").append(currentTriviaIndex + 1).append("/").append(activeTriviaSession.size()).append(":\n");
            sb.append(nextQuestion.question).append("\n\n");
            for (String option : nextQuestion.options) {
                sb.append("• ").append(option).append("\n");
            }
            return botMessage(sb.toString().trim(), 0.99, "trivia");
        } else {
            inTriviaMode = false;
            StringBuilder sb = new StringBuilder();
            sb.append(resultFeedback).append("\n\n");
            sb.append("🏁 **Game Over!** You scored **").append(triviaScore).append("/").append(activeTriviaSession.size()).append("**.\n\n");
            if (triviaScore == activeTriviaSession.size()) {
                sb.append("🏆 *Perfect score! You are a Java Master!* 🌟");
            } else if (triviaScore >= activeTriviaSession.size() / 2) {
                sb.append("👍 *Good job! You know your Java basics. Keep it up!*");
            } else {
                sb.append("📚 *Keep learning and try again next time!*");
            }
            return botMessage(sb.toString(), 0.99, "trivia");
        }
    }
}
