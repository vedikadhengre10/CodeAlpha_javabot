package com.aichatbot.service;

import java.util.*;

/**
 * Knowledge Base — stores FAQ patterns and intent-based response templates.
 * Acts as the "trained" data for the rule-based ML engine.
 */
public class KnowledgeBase {

    /** A knowledge entry: a set of trigger phrases + a list of varied responses */
    public static class KnowledgeEntry {
        public final List<String> triggers;
        public final List<String> responses;
        public final String intent;

        public KnowledgeEntry(String intent, List<String> triggers, List<String> responses) {
            this.intent    = intent;
            this.triggers  = triggers;
            this.responses = responses;
        }
    }

    private final List<KnowledgeEntry> entries = new ArrayList<>();
    private final Map<String, List<String>> intentResponses = new LinkedHashMap<>();
    private final Random random = new Random();

    public KnowledgeBase() {
        loadFAQ();
        loadIntentResponses();
    }

    // ── Lookup ────────────────────────────────────────────────────────────────

        /** Find the best-matching FAQ entry using TF-IDF cosine similarity */
    public Optional<KnowledgeEntry> findBestMatch(String normalizedInput, com.aichatbot.nlp.NLPEngine nlp) {
        KnowledgeEntry best = null;
        double bestSim = 0.0;

        for (KnowledgeEntry entry : entries) {
            for (String trigger : entry.triggers) {
                double sim = nlp.cosineSimilarity(normalizedInput, trigger);
                if (sim > bestSim) {
                    bestSim = sim;
                    best = entry;
                }
            }
        }
        // Requiring at least 0.50 similarity ensures that at least 50% of the content words match.
        return bestSim >= 0.50 ? Optional.ofNullable(best) : Optional.empty();
    }

    /** Pick a random response for a given intent */
    public Optional<String> getIntentResponse(String intent) {
        List<String> responses = intentResponses.get(intent);
        if (responses == null || responses.isEmpty()) return Optional.empty();
        return Optional.of(responses.get(random.nextInt(responses.size())));
    }

    public List<KnowledgeEntry> getAllEntries() { return Collections.unmodifiableList(entries); }

    // ── Private helpers ───────────────────────────────────────────────────────

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadFAQ() {
        add("identity",
            List.of("who are you", "what are you", "tell me about yourself", "your name", "introduce yourself"),
            List.of(
                "I'm an AI Chatbot built in Java, powered by NLP and rule-based machine learning. I'm here to help!",
                "My name is JavaBot — an intelligent assistant crafted with NLP techniques to understand and respond to you.",
                "I'm your AI companion, built using Java with natural language understanding capabilities!"
            )
        );

        add("capability",
            List.of("what can you do", "your features", "your abilities", "how can you help", "what do you know"),
            List.of(
                "I can:\n• Answer questions & FAQs\n• Perform math calculations\n• Analyze your sentiment\n• Tell jokes\n• Provide time & date\n• Recommend resources\n• And much more!",
                "My superpowers include NLP-based understanding, intent detection, math solving, sentiment analysis, knowledge retrieval, and friendly conversation!"
            )
        );

        add("java",
            List.of("what is java", "tell me about java", "java programming", "java language", "java features"),
            List.of(
                "Java is a high-level, object-oriented programming language developed by Sun Microsystems in 1995. Key features:\n• Platform independent (Write Once, Run Anywhere)\n• Object-Oriented\n• Strongly typed\n• Garbage collected\n• Huge ecosystem (Spring, Hibernate, Maven...)",
                "Java is one of the world's most popular languages! It's used in Android apps, enterprise backend systems, big data (Hadoop, Spark), and of course — AI chatbots like me! 😄"
            )
        );

        add("nlp",
            List.of("what is nlp", "natural language processing", "explain nlp", "how does nlp work"),
            List.of(
                "NLP (Natural Language Processing) is a branch of AI that helps computers understand, interpret, and generate human language.\nKey techniques include:\n• Tokenization\n• Stemming & Lemmatization\n• Intent Detection\n• Sentiment Analysis\n• Named Entity Recognition (NER)\n• TF-IDF & Vector Similarity",
                "NLP bridges the gap between human communication and machine understanding. I use tokenization, stopword removal, stemming, intent matching, and cosine similarity — all classic NLP techniques!"
            )
        );

        add("ml",
            List.of("machine learning", "what is ml", "explain machine learning", "ml in chatbots"),
            List.of(
                "Machine Learning (ML) enables systems to learn from data without explicit programming.\nIn chatbots, ML powers:\n• Intent classification\n• Response ranking\n• Sentiment models\n• Context tracking\n• Personalization",
                "I use rule-based ML logic: pattern matching, scoring algorithms, and knowledge retrieval — a lightweight but effective approach!"
            )
        );

        add("ai",
            List.of("what is ai", "artificial intelligence", "explain ai", "define ai"),
            List.of(
                "Artificial Intelligence (AI) is the simulation of human intelligence in machines. It encompasses:\n• Machine Learning\n• Natural Language Processing\n• Computer Vision\n• Robotics\n• Expert Systems",
                "AI is the broader field of making machines smart. I'm a small but proud member of the AI family — conversational AI! 🤖"
            )
        );

        add("oop",
            List.of("what is oop", "object oriented", "oop concepts", "explain oop"),
            List.of(
                "OOP (Object-Oriented Programming) organizes code into objects.\nThe 4 pillars:\n• Encapsulation — hide internal state\n• Abstraction — expose only essentials\n• Inheritance — reuse code via parent classes\n• Polymorphism — many forms, one interface",
                "Java is built on OOP! Classes, objects, interfaces, and inheritance form the backbone of Java development."
            )
        );

        add("interview",
            List.of("java interview", "interview questions", "java tips", "java interview prep"),
            List.of(
                "Top Java Interview Topics:\n• JVM, JDK, JRE differences\n• Collections Framework\n• Multithreading & Concurrency\n• Exception Handling\n• Streams & Lambdas\n• Spring Framework\n• Design Patterns\n• SQL & JDBC",
                "For Java interviews, focus on core OOP, data structures, Collections, Strings, multithreading, and Java 8+ features like streams and lambdas!"
            )
        );

        add("study",
            List.of("how to learn java", "java resources", "learn programming", "study tips", "best way to learn"),
            List.of(
                "Tips to master Java:\n1. Learn fundamentals (variables, loops, OOP)\n2. Build small projects\n3. Read official docs (docs.oracle.com)\n4. Practice on LeetCode/HackerRank\n5. Contribute to open source\n6. Explore frameworks like Spring Boot",
                "Best learning resources:\n• Oracle Java Docs\n• Codecademy / Coursera\n• 'Head First Java' book\n• YouTube: Telusko, Programming with Mosh\n• GitHub — read real code!"
            )
        );

        add("spring",
            List.of("spring boot", "what is spring", "spring framework", "spring mvc"),
            List.of(
                "Spring Boot is a Java framework that simplifies building production-ready applications.\nKey features:\n• Auto-configuration\n• Embedded server (Tomcat)\n• REST API support\n• Dependency Injection\n• Microservices-ready",
                "Spring is the most popular Java framework! Spring Boot lets you go from idea to running server in minutes."
            )
        );

        add("career",
            List.of("java career", "developer salary", "it career", "become a developer", "software engineer"),
            List.of(
                "Java Developer Career Path:\n1. Junior Developer (0-2 yrs)\n2. Mid-level Developer (2-5 yrs)\n3. Senior Developer (5+ yrs)\n4. Tech Lead / Architect\n\nSalary ranges vary by country — in India ₹4L-₹40L, in US $80K-$180K+",
                "Java skills open doors to backend development, Android, data engineering, fintech, and cloud computing. Great career choice! 🚀"
            )
        );
    }

    private void loadIntentResponses() {
        intentResponses.put("greeting", List.of(
            "Hello! 👋 I'm JavaBot, your AI assistant. How can I help you today?",
            "Hey there! Great to see you. What's on your mind?",
            "Hi! I'm here and ready to chat. What would you like to explore?",
            "Good to have you here! I'm JavaBot — ask me anything!"
        ));

        intentResponses.put("identity", List.of(
            "I'm an AI Chatbot built in Java, powered by NLP and rule-based machine learning. I'm here to help!",
            "My name is JavaBot — an intelligent assistant crafted with NLP techniques to understand and respond to you.",
            "I'm your AI companion, built using Java with natural language understanding capabilities!"
        ));

        intentResponses.put("capability", List.of(
            "I can:\n• Answer questions & FAQs\n• Perform math calculations\n• Analyze your sentiment\n• Tell jokes\n• Provide time & date\n• Recommend resources\n• And much more!",
            "My superpowers include NLP-based understanding, intent detection, math solving, sentiment analysis, knowledge retrieval, and friendly conversation!"
        ));

        intentResponses.put("farewell", List.of(
            "Goodbye! It was a pleasure chatting with you. Come back anytime! 👋",
            "See you later! Hope I was helpful today. Take care! 😊",
            "Farewell! Keep learning and keep coding! 🚀",
            "Bye! Remember, I'm always here when you need me. ✨"
        ));

        intentResponses.put("thanks", List.of(
            "You're very welcome! 😊 Happy to help!",
            "My pleasure! That's what I'm here for.",
            "Anytime! Don't hesitate to ask more questions.",
            "Glad I could assist! Keep those questions coming! 🎯"
        ));

        intentResponses.put("joke", List.of(
            "Why do Java programmers wear glasses? Because they don't C#! 😄",
            "What's a computer's favorite snack? Microchips! 🍟",
            "Why did the developer go broke? Because he used up all his cache! 💸",
            "How do you comfort a JavaScript bug? You console it! 🐛",
            "Why do programmers prefer dark mode? Because light attracts bugs! 🌙",
            "A SQL query walks into a bar, walks up to two tables and asks... Can I join you? 😂"
        ));

        intentResponses.put("time", List.of(
            "The current time is: " + new java.util.Date().toString(),
            "Right now it's: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' HH:mm:ss"))
        ));

        intentResponses.put("weather", List.of(
            "I don't have live weather data, but I can tell you: Always code in good weather and great spirits! ☀️",
            "For real-time weather, check weather.com or your phone's weather app! I specialize in Java, not meteorology 😄"
        ));

        intentResponses.put("sentiment_check", List.of(
            "I'm doing wonderfully! I love chatting with curious minds like you. 😊",
            "I'm fantastic, thanks for asking! Every conversation makes me smarter. How about you?",
            "I'm great! Running at full NLP capacity today. 🤖✨"
        ));

        intentResponses.put("help", List.of(
            "I'm here to help! You can ask me about:\n• Java programming\n• AI & Machine Learning\n• Math calculations\n• Jokes & fun facts\n• Career advice\n• And much more!\n\nJust type your question!",
            "Sure! Here's what I can assist with:\n💡 Programming concepts\n🧮 Math problems\n📚 Learning resources\n😄 Jokes & trivia\n🤖 AI explanations\n\nFire away!"
        ));

        intentResponses.put("opinion", List.of(
            "As an AI, I don't have personal opinions, but I can share insights based on data and patterns! What would you like to explore?",
            "That's a great question for reflection! I process patterns rather than form opinions, but I'm happy to analyze different perspectives with you."
        ));

        intentResponses.put("trivia", List.of(
            "Fun fact: Java was originally called 'Oak' after an oak tree outside James Gosling's office! 🌳",
            "Did you know? The first computer bug was an actual bug — a moth found in a Harvard Mark II relay in 1947! 🦗",
            "Fun fact: There are over 700 programming languages in existence, but Java has been in the Top 3 for 25+ years! 🏆",
            "Did you know? 'Hello, World!' was first used in a 1972 C programming tutorial by Brian Kernighan! 👋",
            "Fun fact: The average programmer writes about 10-12 lines of production code per day after debugging and review! 📝"
        ));

        intentResponses.put("recommendation", List.of(
            "My top recommendations for aspiring developers:\n1. 📖 Read 'Clean Code' by Robert Martin\n2. 🎯 Practice data structures daily\n3. 🔨 Build real projects\n4. 🤝 Join open source communities\n5. 🚀 Never stop learning!",
            "For learning resources, I recommend:\n• Oracle Java Documentation\n• Spring.io guides\n• Baeldung.com for Java tutorials\n• Coursera / Udemy for structured courses\n• GitHub for real-world code examples"
        ));

        intentResponses.put("unknown", List.of(
            "Hmm, I'm not quite sure I understand that. Could you rephrase? You can ask me about Java, AI, math, or just chat! 😊",
            "Interesting! I'm still learning. Could you ask that differently? Try: 'what is Java', 'tell me a joke', or 'calculate 15 * 8'",
            "I didn't quite catch that. My expertise is in Java, NLP, AI, and general knowledge. Want to try a different question?",
            "That's a bit beyond my current knowledge base! But I'm always growing. Try asking about programming, AI, or anything tech-related!"
        ));
    }

    private void add(String intent, List<String> triggers, List<String> responses) {
        entries.add(new KnowledgeEntry(intent, triggers, responses));
    }
}
