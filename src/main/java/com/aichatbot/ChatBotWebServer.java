package com.aichatbot;

import com.aichatbot.model.Message;
import com.aichatbot.service.ChatBotService;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/**
 * Minimal built-in HTTP server (no external dependencies).
 * Serves the chat UI and handles /api/chat POST requests.
 */
public class ChatBotWebServer {

    private final ChatBotService service = new ChatBotService();
    private final ExecutorService pool = Executors.newFixedThreadPool(4);

    public void start(int port) {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("[Server] Listening on http://localhost:" + port);
            while (true) {
                Socket client = server.accept();
                pool.submit(() -> handle(client));
            }
        } catch (IOException e) {
            System.err.println("[Server] Error: " + e.getMessage());
        }
    }

    private void handle(Socket client) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
            OutputStream out = client.getOutputStream()
        ) {
            // Read request line
            String requestLine = in.readLine();
            if (requestLine == null) return;

            String[] parts = requestLine.split(" ");
            String method = parts[0];
            String path   = parts.length > 1 ? parts[1] : "/";

            // Read headers
            int contentLength = 0;
            String line;
            while (!(line = in.readLine()).isEmpty()) {
                if (line.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.split(":")[1].trim());
                }
            }

            if ("GET".equals(method) && (path.equals("/") || path.equals("/index.html"))) {
                sendHtml(out, buildHtml());
            } else if ("POST".equals(method) && path.equals("/api/chat")) {
                char[] body = new char[contentLength];
                in.read(body, 0, contentLength);
                String json = new String(body);
                String userMsg = extractJson(json, "message");
                Message response = service.processInput(userMsg);
                sendJson(out, response.toJson());
            } else if ("GET".equals(method) && path.equals("/api/stats")) {
                String stats = String.format(
                    "{\"messages\":%d,\"topIntent\":\"%s\",\"user\":\"%s\",\"sentiment\":\"%s\"}",
                    service.getMessageCount(),
                    service.getMostCommonIntent(),
                    service.getUserName() != null ? service.getUserName() : "Anonymous",
                    service.getSessionSentiment()
                );
                sendJson(out, stats);
            } else {
                send404(out);
            }
        } catch (IOException e) {
            // Client disconnected
        }
    }

    private String extractJson(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return "";
        return json.substring(start, end)
                   .replace("\\n", "\n")
                   .replace("\\\"", "\"");
    }

    private void sendHtml(OutputStream out, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 200 OK\r\n" +
                         "Content-Type: text/html; charset=UTF-8\r\n" +
                         "Content-Length: " + bytes.length + "\r\n\r\n";
        out.write(headers.getBytes());
        out.write(bytes);
        out.flush();
    }

    private void sendJson(OutputStream out, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 200 OK\r\n" +
                         "Content-Type: application/json; charset=UTF-8\r\n" +
                         "Content-Length: " + bytes.length + "\r\n" +
                         "Access-Control-Allow-Origin: *\r\n\r\n";
        out.write(headers.getBytes());
        out.write(bytes);
        out.flush();
    }

    private void send404(OutputStream out) throws IOException {
        String body = "404 Not Found";
        byte[] bytes = body.getBytes();
        String h = "HTTP/1.1 404 Not Found\r\nContent-Length: " + bytes.length + "\r\n\r\n";
        out.write(h.getBytes());
        out.write(bytes);
        out.flush();
    }

    // ── Embedded HTML/CSS/JS UI ───────────────────────────────────────────────

    private String buildHtml() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1"/>
<title>JavaBot AI — Chat</title>
<link rel="preconnect" href="https://fonts.googleapis.com"/>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&family=Fira+Code:wght@400;500&display=swap" rel="stylesheet"/>
<style>
  :root {
    --bg:        #0f0f1a;
    --surface:   #1a1a2e;
    --surface2:  #252540;
    --accent:    #6366f1;
    --accent-h:  #818cf8;
    --success:   #34d399;
    --danger:    #f87171;
    --text:      #e8e8ff;
    --muted:     #8888b8;
    --bot-bub:   #1e1e38;
    --user-bub:  #4338ca;
    --radius:    14px;
  }
  *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    font-family: 'Inter', sans-serif;
    background: var(--bg);
    color: var(--text);
    height: 100vh;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  /* ── Header ── */
  header {
    display: flex; align-items: center; justify-content: space-between;
    padding: 14px 20px;
    background: var(--surface);
    border-bottom: 1px solid rgba(255,255,255,.06);
    backdrop-filter: blur(10px);
  }
  .bot-info { display: flex; align-items: center; gap: 12px; }
  .avatar {
    width: 42px; height: 42px; border-radius: 50%;
    background: linear-gradient(135deg, var(--accent), #a78bfa);
    display: flex; align-items: center; justify-content: center;
    font-size: 20px;
    box-shadow: 0 0 18px rgba(99,102,241,.4);
    animation: pulse 3s infinite;
  }
  @keyframes pulse {
    0%,100% { box-shadow: 0 0 18px rgba(99,102,241,.4); }
    50%      { box-shadow: 0 0 28px rgba(99,102,241,.7); }
  }
  .bot-name { font-weight: 600; font-size: 15px; }
  .status   { font-size: 11px; color: var(--success); margin-top: 2px; }
  .header-actions { display: flex; gap: 8px; }
  .hbtn {
    background: var(--surface2); border: none; color: var(--muted);
    padding: 7px 12px; border-radius: 8px; cursor: pointer; font-size: 13px;
    transition: all .2s;
  }
  .hbtn:hover { background: var(--accent); color: #fff; }

  /* ── Chat Area ── */
  #chat {
    flex: 1; overflow-y: auto; padding: 20px 16px;
    display: flex; flex-direction: column; gap: 12px;
    scroll-behavior: smooth;
  }
  #chat::-webkit-scrollbar { width: 5px; }
  #chat::-webkit-scrollbar-track { background: transparent; }
  #chat::-webkit-scrollbar-thumb { background: var(--surface2); border-radius: 4px; }

  .msg-row { display: flex; align-items: flex-end; gap: 8px; animation: fadeUp .3s ease; }
  .msg-row.user { flex-direction: row-reverse; }
  @keyframes fadeUp { from { opacity:0; transform:translateY(10px) } to { opacity:1; transform:translateY(0) } }

  .bubble {
    max-width: min(70%, 520px);
    padding: 11px 16px;
    border-radius: var(--radius);
    font-size: 13.5px; line-height: 1.6;
    white-space: pre-wrap; word-break: break-word;
  }
  .bubble.bot  {
    background: var(--bot-bub);
    border-bottom-left-radius: 4px;
    border: 1px solid rgba(99,102,241,.15);
  }
  .bubble.user {
    background: var(--user-bub);
    border-bottom-right-radius: 4px;
    color: #fff;
  }
  .mini-avatar {
    width: 28px; height: 28px; border-radius: 50%; font-size: 14px;
    display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  }
  .mini-avatar.bot  { background: linear-gradient(135deg, var(--accent), #a78bfa); }
  .mini-avatar.user { background: var(--surface2); }

  .meta {
    font-size: 10px; color: var(--muted); margin-top: 2px; padding: 0 4px;
    font-family: 'Fira Code', monospace;
  }
  .msg-row.user .meta { text-align: right; }

  /* ── Typing Indicator ── */
  .typing { display: flex; align-items: center; gap: 5px; padding: 10px 14px; }
  .typing span {
    width: 7px; height: 7px; border-radius: 50%;
    background: var(--accent);
    animation: bounce .8s infinite;
  }
  .typing span:nth-child(2) { animation-delay: .15s; }
  .typing span:nth-child(3) { animation-delay: .30s; }
  @keyframes bounce { 0%,60%,100% { transform:translateY(0) } 30% { transform:translateY(-8px) } }

  /* ── Input Area ── */
  footer {
    padding: 12px 16px 16px;
    background: var(--surface);
    border-top: 1px solid rgba(255,255,255,.06);
  }
  #typing-status { font-size: 11px; color: var(--accent-h); margin-bottom: 8px; min-height: 16px; }
  .input-row { display: flex; gap: 8px; }
  #msg-input {
    flex: 1; background: var(--surface2); border: 1px solid rgba(255,255,255,.08);
    color: var(--text); border-radius: 12px; padding: 11px 16px;
    font-size: 13.5px; font-family: 'Inter', sans-serif;
    outline: none; transition: border-color .2s;
  }
  #msg-input:focus { border-color: var(--accent); }
  #msg-input::placeholder { color: var(--muted); }
  #send-btn {
    background: var(--accent); color: #fff; border: none;
    padding: 11px 22px; border-radius: 12px; font-size: 13px;
    font-weight: 600; cursor: pointer; transition: all .2s;
    font-family: 'Inter', sans-serif;
  }
  #send-btn:hover:not(:disabled) { background: var(--accent-h); transform: translateY(-1px); }
  #send-btn:disabled { opacity: .5; cursor: default; transform: none; }

  /* ── Stats Modal ── */
  .modal-overlay {
    display: none; position: fixed; inset: 0;
    background: rgba(0,0,0,.6); backdrop-filter: blur(6px);
    align-items: center; justify-content: center; z-index: 100;
  }
  .modal-overlay.open { display: flex; }
  .modal {
    background: var(--surface); border: 1px solid rgba(99,102,241,.3);
    border-radius: 18px; padding: 28px; width: 320px;
    box-shadow: 0 20px 60px rgba(0,0,0,.5);
    animation: fadeUp .3s ease;
  }
  .modal h3 { font-size: 16px; margin-bottom: 16px; }
  .stat-item { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid rgba(255,255,255,.05); font-size: 13px; }
  .stat-item:last-child { border: none; }
  .stat-val { color: var(--accent-h); font-weight: 600; font-family: 'Fira Code', monospace; }
  .close-btn { margin-top: 18px; width: 100%; padding: 10px; background: var(--surface2); border: none; color: var(--text); border-radius: 10px; cursor: pointer; font-size: 13px; }
  .close-btn:hover { background: var(--accent); }

  /* ── Suggestions ── */
  .suggestions { display: flex; gap: 7px; flex-wrap: wrap; margin-bottom: 10px; }
  .sug {
    background: var(--surface2); border: 1px solid rgba(99,102,241,.2);
    color: var(--muted); padding: 5px 12px; border-radius: 20px;
    font-size: 11.5px; cursor: pointer; transition: all .2s;
  }
  .sug:hover { border-color: var(--accent); color: var(--text); background: rgba(99,102,241,.15); }

  /* ── Markdown & Code Styles ── */
  .bubble strong { font-weight: 600; color: #fff; }
  .bubble em { font-style: italic; color: #a78bfa; }
  .bubble ul { margin-left: 18px; margin-top: 6px; margin-bottom: 6px; list-style-type: disc; }
  .bubble li { margin-bottom: 4px; }
  .inline-code {
    font-family: 'Fira Code', monospace;
    background: rgba(255, 255, 255, 0.08);
    color: #f472b6;
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 12px;
  }
  .code-block-wrapper {
    margin: 10px 0;
    border-radius: 8px;
    background: #1e1e2f;
    border: 1px solid rgba(255, 255, 255, 0.08);
    overflow: hidden;
  }
  .code-block-header {
    background: #151522;
    padding: 6px 12px;
    font-size: 11px;
    color: var(--muted);
    font-family: 'Inter', sans-serif;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  }
  .code-block {
    font-family: 'Fira Code', monospace;
    font-size: 12.5px;
    color: #cbd5e1;
    padding: 12px;
    display: block;
    overflow-x: auto;
    white-space: pre;
    line-height: 1.5;
  }

  /* ── Sentiment Glow ── */
  .avatar.positive {
    background: linear-gradient(135deg, #10b981, #34d399);
    box-shadow: 0 0 18px rgba(16,185,129,.5);
  }
  .avatar.negative {
    background: linear-gradient(135deg, #ef4444, #f87171);
    box-shadow: 0 0 18px rgba(239,68,68,.5);
  }
  .avatar.neutral {
    background: linear-gradient(135deg, var(--accent), #a78bfa);
    box-shadow: 0 0 18px rgba(99,102,241,.4);
  }
</style>
</head>
<body>

<header>
  <div class="bot-info">
    <div class="avatar neutral">🤖</div>
    <div>
      <div class="bot-name">JavaBot AI</div>
      <div class="status" id="status-text">● Online</div>
    </div>
  </div>
  <div class="header-actions">
    <button class="hbtn" onclick="openStats()">📊 Stats</button>
    <button class="hbtn" onclick="exportChat()">📥 Export</button>
    <button class="hbtn" onclick="clearChat()">🗑️ Clear</button>
  </div>
</header>

<div id="chat"></div>

<footer>
  <div id="typing-status"></div>
  <div class="suggestions" id="suggestions">
    <span class="sug" onclick="quickSend(this)">Who are you?</span>
    <span class="sug" onclick="quickSend(this)">What can you do?</span>
    <span class="sug" onclick="quickSend(this)">Play Trivia 🎮</span>
    <span class="sug" onclick="quickSend(this)">Calculate 15 * 8</span>
    <span class="sug" onclick="quickSend(this)">What is Java?</span>
    <span class="sug" onclick="quickSend(this)">Fun trivia</span>
  </div>
  <div class="input-row">
    <input id="msg-input" type="text" placeholder="Type a message..." autocomplete="off"/>
    <button id="send-btn" disabled>Send ➤</button>
  </div>
</footer>

<!-- Stats Modal -->
<div class="modal-overlay" id="stats-modal">
  <div class="modal">
    <h3>📊 Session Statistics</h3>
    <div id="stats-content"></div>
    <button class="close-btn" onclick="closeStats()">Close</button>
  </div>
</div>

<script>
const chat    = document.getElementById('chat');
const input   = document.getElementById('msg-input');
const sendBtn = document.getElementById('send-btn');
const typingS = document.getElementById('typing-status');
let busy = false;

input.addEventListener('input', () => {
  sendBtn.disabled = input.value.trim() === '' || busy;
});
input.addEventListener('keydown', e => {
  if (e.key === 'Enter' && !e.shiftKey && !busy) { e.preventDefault(); sendMessage(); }
});
sendBtn.addEventListener('click', sendMessage);

function formatMarkdown(text) {
  let escaped = text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");

  // Code blocks: ```language ... ``` or ``` ... ```
  escaped = escaped.replace(/```([a-zA-Z0-9]+)?\\n([\\s\\S]*?)\\n```/g, (match, lang, code) => {
    return `<div class="code-block-wrapper"><div class="code-block-header">${lang || 'Code'}</div><pre><code class="code-block">${code}</code></pre></div>`;
  });

  // Inline code: `code`
  escaped = escaped.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>');

  // Bold: **text**
  escaped = escaped.replace(/\\*\\*([^*]+)\\*\\*/g, '<strong>$1</strong>');

  // Italics: *text*
  escaped = escaped.replace(/\\*([^*]+)\\*/g, '<em>$1</em>');

  // Split by newline and wrap lists
  let lines = escaped.split('\\n');
  let inList = false;
  for (let i = 0; i < lines.length; i++) {
    let line = lines[i].trim();
    if (line.startsWith('•') || line.startsWith('-')) {
      let content = line.substring(1).trim();
      if (!inList) {
        lines[i] = '<ul><li>' + content + '</li>';
        inList = true;
      } else {
        lines[i] = '<li>' + content + '</li>';
      }
    } else {
      if (inList) {
        lines[i] = '</ul>' + lines[i];
        inList = false;
      }
    }
  }
  if (inList) {
    lines[lines.length - 1] += '</ul>';
  }
  return lines.join('\\n');
}

function appendMsg(content, role, meta) {
  const row = document.createElement('div');
  row.className = 'msg-row ' + role;

  const av = document.createElement('div');
  av.className = 'mini-avatar ' + role;
  av.textContent = role === 'bot' ? '🤖' : '👤';

  const wrap = document.createElement('div');
  const bub = document.createElement('div');
  bub.className = 'bubble ' + role;

  if (role === 'bot') {
    bub.innerHTML = formatMarkdown(content);
  } else {
    bub.textContent = content;
  }
  wrap.appendChild(bub);

  if (meta) {
    const m = document.createElement('div');
    m.className = 'meta';
    m.textContent = `intent: ${meta.intent} | confidence: ${(meta.confidence*100).toFixed(0)}% | ${meta.timestamp}`;
    wrap.appendChild(m);
  }

  row.appendChild(av);
  row.appendChild(wrap);
  chat.appendChild(row);
  chat.scrollTop = chat.scrollHeight;
}

// Stats and visualizer update helper
async function updateStatsAndSentiment() {
  try {
    const res = await fetch('/api/stats');
    const d = await res.json();
    const avatarEl = document.querySelector('.avatar');
    if (avatarEl) {
      avatarEl.className = 'avatar ' + (d.sentiment || 'neutral');
    }
  } catch(e) {}
}

function showTyping() {
  const row = document.createElement('div');
  row.className = 'msg-row bot'; row.id = 'typing-row';
  const av = document.createElement('div');
  av.className = 'mini-avatar bot'; av.textContent = '🤖';
  const t = document.createElement('div');
  t.className = 'bubble bot typing';
  t.innerHTML = '<span></span><span></span><span></span>';
  row.appendChild(av); row.appendChild(t);
  chat.appendChild(row);
  chat.scrollTop = chat.scrollHeight;
  typingS.textContent = 'JavaBot is typing...';
}
function hideTyping() {
  const t = document.getElementById('typing-row');
  if (t) t.remove();
  typingS.textContent = '';
}

async function sendMessage() {
  const text = input.value.trim();
  if (!text || busy) return;

  input.value = '';
  sendBtn.disabled = true;
  busy = true;

  appendMsg(text, 'user', null);
  showTyping();

  try {
    const res = await fetch('/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: text })
    });
    const data = await res.json();
    hideTyping();
    appendMsg(data.content, 'bot', { intent: data.intent, confidence: data.confidence, timestamp: data.timestamp });
    await updateStatsAndSentiment();
  } catch(err) {
    hideTyping();
    appendMsg('⚠️ Connection error. Make sure the Java server is running!', 'bot', null);
  }

  busy = false;
  sendBtn.disabled = input.value.trim() === '';
  input.focus();
}

function quickSend(el) {
  input.value = el.textContent;
  sendBtn.disabled = false;
  sendMessage();
}

async function openStats() {
  try {
    const res = await fetch('/api/stats');
    const d = await res.json();
    document.getElementById('stats-content').innerHTML = `
      <div class="stat-item"><span>Messages sent</span><span class="stat-val">${d.messages}</span></div>
      <div class="stat-item"><span>Top intent</span><span class="stat-val">${d.topIntent}</span></div>
      <div class="stat-item"><span>User name</span><span class="stat-val">${d.user}</span></div>
      <div class="stat-item"><span>Session Sentiment</span><span class="stat-val">${d.sentiment}</span></div>
    `;
  } catch(e) {
    document.getElementById('stats-content').innerHTML = '<p style="color:var(--muted)">Could not fetch stats.</p>';
  }
  document.getElementById('stats-modal').classList.add('open');
}
function closeStats() { document.getElementById('stats-modal').classList.remove('open'); }

function clearChat() {
  chat.innerHTML = '';
  input.value = '';
  sendBtn.disabled = true;
  input.focus();
}

function exportChat() {
  const bubbles = document.querySelectorAll('.bubble');
  const rows = [];
  bubbles.forEach(bub => {
    const isUser = bub.classList.contains('user');
    const sender = isUser ? 'You' : 'JavaBot';
    rows.push(`${sender}: ${bub.textContent.trim()}`);
    rows.push('----------------------------------------');
  });

  const text = rows.join('\\n');
  const blob = new Blob([text], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `JavaBot_Chat_History_${new Date().toISOString().slice(0, 10)}.txt`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

// Greet on load
window.addEventListener('load', async () => {
  showTyping();
  await new Promise(r => setTimeout(r, 800));
  hideTyping();
  try {
    const res = await fetch('/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: 'hello' })
    });
    const d = await res.json();
    appendMsg(d.content, 'bot', { intent: d.intent, confidence: d.confidence, timestamp: d.timestamp });
    await updateStatsAndSentiment();
  } catch(e) {}
  input.focus();
});
</script>
</body>
</html>
""";
    }
}
