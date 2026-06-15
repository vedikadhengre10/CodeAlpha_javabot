package com.aichatbot.nlp;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * Natural Language Processing Engine
 * Handles tokenization, stemming, intent detection, entity extraction,
 * sentiment analysis, and TF-IDF similarity scoring.
 */
public class NLPEngine {

    // ── Stopwords ─────────────────────────────────────────────────────────────
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "a","an","the","is","it","in","on","at","to","for","of","and","or",
        "but","not","are","was","were","be","been","being","have","has","had",
        "do","does","did","will","would","could","should","may","might","shall",
        "can","i","you","he","she","we","they","me","him","her","us","them",
        "my","your","his","its","our","their","this","that","these","those",
        "what","which","who","how","when","where","why","with","from","by"
    ));

    // ── Sentiment word banks ──────────────────────────────────────────────────
    private static final Set<String> POSITIVE_WORDS = new HashSet<>(Arrays.asList(
        "good","great","excellent","wonderful","amazing","fantastic","awesome",
        "brilliant","love","like","happy","pleased","thanks","thank","helpful",
        "nice","perfect","best","cool","enjoy","enjoyed","useful","impressive"
    ));

    private static final Set<String> NEGATIVE_WORDS = new HashSet<>(Arrays.asList(
        "bad","terrible","awful","horrible","hate","dislike","unhappy","worst",
        "useless","poor","disappoint","wrong","error","fail","broken","issue",
        "problem","bug","crash","annoying","slow","confused","lost","stuck"
    ));

    // ── Intent patterns ───────────────────────────────────────────────────────
    private static final Map<String, Pattern> INTENT_PATTERNS = new LinkedHashMap<>();
    static {
        INTENT_PATTERNS.put("greeting",
            Pattern.compile("\\b(hello|hi|hey|greetings|howdy|sup|good (morning|evening|afternoon|day))\\b", Pattern.CASE_INSENSITIVE));
        INTENT_PATTERNS.put("farewell",
            Pattern.compile("\\b(bye|goodbye|see you|farewell|quit|exit|later|cya|ttyl)\\b", Pattern.CASE_INSENSITIVE));
        INTENT_PATTERNS.put("thanks",
            Pattern.compile("\\b(thank(s| you)|thx|ty|appreciate|grateful)\\b", Pattern.CASE_INSENSITIVE));
        INTENT_PATTERNS.put("help",
            Pattern.compile("\\b(help|assist|support|guide|how (do|can|to)|what (can|should)|explain)\\b", Pattern.CASE_INSENSITIVE));
        INTENT_PATTERNS.put("weather",
            Pattern.compile("\\b(weather|temperature|forecast|rain|sunny|cloudy|hot|cold|humidity|wind)\\b", Pattern.CASE_INSENSITIVE));
        INTENT_PATTERNS.put("joke",
            Pattern.compile("\\b(joke|funny|laugh|humor|pun|comedy|tell me something funny)\\b", Pattern.CASE_INSENSITIVE));
        INTENT_PATTERNS.put("time",
            Pattern.compile("\\b(time|clock|hour|minute|what time|current time|date|today|day)\\b", Pattern.CASE_INSENSITIVE));
        INTENT_PATTERNS.put("identity",
            Pattern.compile("\\b(who are you|your name|what are you|about you|introduce yourself|tell me about yourself)\\b", Pattern.CASE_INSENSITIVE));
        INTENT_PATTERNS.put("capability",
            Pattern.compile("\\b(what can you do|your (skills|abilities|features|capabilities)|help me with|how do you work)\\b", Pattern.CASE_INSENSITIVE));
        INTENT_PATTERNS.put("math",
            Pattern.compile("\\b(calculate|compute|math|\\d+\\s*[+\\-*/^%]\\s*\\d+|solve|equation|arithmetic)\\b", Pattern.CASE_INSENSITIVE));
        INTENT_PATTERNS.put("definition",
            Pattern.compile("\\b(what is|define|definition|meaning of|explain|describe)\\b", Pattern.CASE_INSENSITIVE));
        INTENT_PATTERNS.put("recommendation",
            Pattern.compile("\\b(recommend|suggest|best|top|should i|advice|tip|idea)\\b", Pattern.CASE_INSENSITIVE));
        INTENT_PATTERNS.put("sentiment_check",
            Pattern.compile("\\b(how are you|how do you feel|are you ok|doing well|feeling)\\b", Pattern.CASE_INSENSITIVE));
        INTENT_PATTERNS.put("opinion",
            Pattern.compile("\\b(do you think|your opinion|your view|what do you (think|believe|feel|say))\\b", Pattern.CASE_INSENSITIVE));
        INTENT_PATTERNS.put("trivia",
            Pattern.compile("\\b(trivia|fun fact|did you know|random fact|interesting fact)\\b", Pattern.CASE_INSENSITIVE));
    }

    // ── Entity patterns ───────────────────────────────────────────────────────
    private static final Pattern EMAIL_PATTERN    = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}");
    private static final Pattern URL_PATTERN      = Pattern.compile("https?://[\\w./%-]+");
    private static final Pattern NUMBER_PATTERN   = Pattern.compile("\\b-?\\d+(\\.\\d+)?\\b");
    private static final Pattern DATE_PATTERN     = Pattern.compile("\\b(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|today|tomorrow|yesterday|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MATH_EXPR_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([+\\-*/^%])\\s*(\\d+(?:\\.\\d+)?)");

    // ── Public API ─────────────────────────────────────────────────────────────

    /** Lowercase, collapse whitespace */
    public String normalize(String text) {
        return text.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    /** Split on non-alphanumeric boundaries */
    public List<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase().split("[^a-zA-Z0-9']+"))
                     .filter(t -> !t.isEmpty())
                     .collect(Collectors.toList());
    }

    /** Remove stopwords */
    public List<String> removeStopWords(List<String> tokens) {
        return tokens.stream().filter(t -> !STOP_WORDS.contains(t)).collect(Collectors.toList());
    }

    /** Naive suffix-stripping stemmer */
    public String stem(String word) {
        if (word.endsWith("ing") && word.length() > 5) return word.substring(0, word.length() - 3);
        if (word.endsWith("tion") && word.length() > 5) return word.substring(0, word.length() - 4);
        if (word.endsWith("ed")  && word.length() > 4) return word.substring(0, word.length() - 2);
        if (word.endsWith("ly")  && word.length() > 4) return word.substring(0, word.length() - 2);
        if (word.endsWith("er")  && word.length() > 4) return word.substring(0, word.length() - 2);
        if (word.endsWith("est") && word.length() > 5) return word.substring(0, word.length() - 3);
        if (word.endsWith("ies") && word.length() > 4) return word.substring(0, word.length() - 3) + "y";
        if (word.endsWith("s")   && word.length() > 3 && !word.endsWith("ss")) return word.substring(0, word.length() - 1);
        return word;
    }

    /** Return best matching intent label or "unknown" */
    public String detectIntent(String text) {
        for (Map.Entry<String, Pattern> entry : INTENT_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(text).find()) return entry.getKey();
        }
        return "unknown";
    }

    /** Confidence score 0-1 based on keyword density */
    public double computeConfidence(String text, String intent) {
        if ("unknown".equals(intent)) return 0.3;
        List<String> tokens = removeStopWords(tokenize(text));
        if (tokens.isEmpty()) return 0.5;

        Pattern p = INTENT_PATTERNS.get(intent);
        if (p == null) return 0.5;

        long matches = tokens.stream()
            .filter(t -> p.matcher(t).find())
            .count();
        double raw = (double) matches / tokens.size();
        return Math.min(0.98, 0.5 + raw * 2.5);
    }

    /** -1 = negative, 0 = neutral, 1 = positive */
    public int analyzeSentiment(String text) {
        List<String> tokens = tokenize(text);
        long pos = tokens.stream().filter(POSITIVE_WORDS::contains).count();
        long neg = tokens.stream().filter(NEGATIVE_WORDS::contains).count();
        if (pos > neg) return 1;
        if (neg > pos) return -1;
        return 0;
    }

    /** Extract named entities from text */
    public Map<String, List<String>> extractEntities(String text) {
        Map<String, List<String>> entities = new LinkedHashMap<>();
        addMatches(entities, "EMAIL",  EMAIL_PATTERN,  text);
        addMatches(entities, "URL",    URL_PATTERN,    text);
        addMatches(entities, "NUMBER", NUMBER_PATTERN, text);
        addMatches(entities, "DATE",   DATE_PATTERN,   text);
        return entities;
    }

    /** Attempt to evaluate a simple math expression from user input */
    public Optional<Double> extractAndEvaluateMath(String text) {
        Matcher m = MATH_EXPR_PATTERN.matcher(text);
        if (!m.find()) return Optional.empty();
        try {
            double a = Double.parseDouble(m.group(1));
            double b = Double.parseDouble(m.group(3));
            double result = switch (m.group(2)) {
                case "+" -> a + b;
                case "-" -> a - b;
                case "*" -> a * b;
                case "/" -> b != 0 ? a / b : Double.NaN;
                case "^" -> Math.pow(a, b);
                case "%" -> a % b;
                default  -> Double.NaN;
            };
            return Double.isNaN(result) ? Optional.empty() : Optional.of(result);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** Cosine similarity between two texts using TF vectors */
    public double cosineSimilarity(String text1, String text2) {
        Map<String, Integer> tf1 = termFrequency(text1);
        Map<String, Integer> tf2 = termFrequency(text2);
        Set<String> allTerms = new HashSet<>(tf1.keySet());
        allTerms.addAll(tf2.keySet());

        double dot = 0, mag1 = 0, mag2 = 0;
        for (String term : allTerms) {
            int v1 = tf1.getOrDefault(term, 0);
            int v2 = tf2.getOrDefault(term, 0);
            dot  += v1 * v2;
            mag1 += v1 * v1;
            mag2 += v2 * v2;
        }
        if (mag1 == 0 || mag2 == 0) return 0;
        return dot / (Math.sqrt(mag1) * Math.sqrt(mag2));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, Integer> termFrequency(String text) {
        Map<String, Integer> tf = new HashMap<>();
        removeStopWords(tokenize(text)).stream()
            .map(this::stem)
            .forEach(t -> tf.merge(t, 1, Integer::sum));
        return tf;
    }

    private void addMatches(Map<String, List<String>> map, String label, Pattern p, String text) {
        Matcher m = p.matcher(text);
        List<String> found = new ArrayList<>();
        while (m.find()) found.add(m.group());
        if (!found.isEmpty()) map.put(label, found);
    }
}
