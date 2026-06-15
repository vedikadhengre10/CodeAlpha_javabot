# 🤖 Java AI Chatbot — JavaBot

A full-featured, production-quality AI chatbot built entirely in Java with NLP techniques,
rule-based machine learning logic, a beautiful Swing desktop GUI, and a built-in web server
with a modern browser chat interface.

---

## ✨ Features

| Feature | Description |
|---|---|
| **NLP Engine** | Tokenization, stemming, stopword removal, intent detection |
| **Sentiment Analysis** | Detects positive / neutral / negative tone in each message |
| **Intent Detection** | 15+ intent categories (greeting, farewell, math, help, jokes…) |
| **Named Entity Recognition** | Extracts emails, URLs, numbers, dates from input |
| **Math Calculator** | Evaluates arithmetic expressions (+, -, *, /, ^, %) inline |
| **TF-IDF Similarity** | Cosine similarity fallback when exact patterns fail |
| **Knowledge Base** | 11 FAQ topics: Java, OOP, NLP, AI, Spring Boot, career & more |
| **Context Tracking** | Remembers user name, tracks most-discussed intents, message count |
| **Session Stats** | Live stats: message count, top intent, user name |
| **Desktop GUI** | Dark-themed Swing GUI with chat bubbles, typing animation |
| **Web Server** | Built-in HTTP server — no dependencies; serves full chat UI |
| **Response Variety** | Multiple response variants per topic, randomly selected |

---

## 🗂️ Project Structure

```
chatbot/
├── src/main/java/com/aichatbot/
│   ├── ChatBotApplication.java       ← Main entry point
│   ├── ChatBotWebServer.java         ← Built-in HTTP server + HTML UI
│   ├── model/
│   │   └── Message.java              ← Message model with JSON serialization
│   ├── nlp/
│   │   └── NLPEngine.java            ← Full NLP pipeline
│   └── service/
│       ├── KnowledgeBase.java        ← FAQ data + intent responses
│       └── ChatBotService.java       ← Core orchestration & ML logic
├── JavaAIChatBot.jar                 ← Compiled executable JAR
├── pom.xml                           ← Maven build file
└── README.md
```

---

## 🚀 How to Run

### Prerequisites
- Java 17+ (OpenJDK or Oracle)

### Web Mode (Browser UI) ← Recommended

```bash
java -jar JavaAIChatBot.jar --web
# Open http://localhost:8080 in your browser
```

### Desktop GUI Mode (Swing)

```bash
java -jar JavaAIChatBot.jar
```
> Note: Desktop mode requires a display (not headless servers).

---

## 🏗️ Compile from Source

```bash
# Using javac directly
find src -name "*.java" | xargs javac -d out --release 17
echo "Main-Class: com.aichatbot.ChatBotApplication" > manifest.txt
jar cfm JavaAIChatBot.jar manifest.txt -C out .

# Or using Maven
mvn package
```

---

## 🧠 NLP Pipeline

```
User Input
    ↓
normalize()          →  lowercase, collapse whitespace
tokenize()           →  split on non-alphanumeric boundaries
removeStopWords()    →  filter common words (a, the, is…)
stem()               →  suffix-strip (running→run, playing→play)
detectIntent()       →  regex pattern match across 15 intent categories
computeConfidence()  →  keyword density score (0.0–1.0)
analyzeSentiment()   →  positive/neutral/negative word bank
extractEntities()    →  regex NER for emails, URLs, numbers, dates
cosineSimilarity()   →  TF-IDF vector fallback matching
```

---

## 💬 Example Conversations

```
You:  hello
Bot:  Hello! 👋 I'm JavaBot, your AI assistant. How can I help you today?

You:  calculate 144 / 12
Bot:  🧮 Result: 12

You:  what is nlp
Bot:  NLP (Natural Language Processing) is a branch of AI that helps computers...

You:  tell me a joke
Bot:  Why do Java programmers wear glasses? Because they don't C#! 😄

You:  my name is Riya
Bot:  Nice to meet you, Riya!

You:  what is spring boot
Bot:  Spring Boot is a Java framework that simplifies building production-ready...

You:  I hate bugs in my code
Bot:  I sense some frustration — let me do my best to help! 💪
      (sentiment: negative detected)
```

---

## 🎯 Intent Categories

`greeting` · `farewell` · `thanks` · `help` · `weather` · `joke` · `time`
`identity` · `capability` · `math` · `definition` · `recommendation`
`sentiment_check` · `opinion` · `trivia`

---

## 🔧 Extending the Bot

### Add a new FAQ topic (KnowledgeBase.java)
```java
add("databases",
    List.of("what is sql", "mysql", "database", "jdbc"),
    List.of(
        "SQL is a language for managing relational databases...",
        "Java connects to databases via JDBC — the Java Database Connectivity API!"
    )
);
```

### Add a new intent (NLPEngine.java)
```java
INTENT_PATTERNS.put("poetry",
    Pattern.compile("\\b(poem|poetry|rhyme|verse|haiku)\\b", Pattern.CASE_INSENSITIVE));
```

### Add intent responses (KnowledgeBase.java — loadIntentResponses)
```java
intentResponses.put("poetry", List.of(
    "Roses are red, code is blue, Java is awesome, and so are you! 🌹"
));
```

---

## 📦 Dependencies

**Zero external dependencies.** Everything uses Java standard library only:
- `java.util` — Collections, Regex, Random
- `java.net` — ServerSocket for HTTP server
- `java.time` — LocalDateTime for timestamps
- `javax.swing` — Desktop GUI components

---

## 📄 License
MIT — free to use, modify, and distribute.
