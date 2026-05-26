// App.jsx
import React, { useEffect, useMemo, useRef, useState } from "react";

const API_BASE = import.meta.env.VITE_API_URL || "/api";

// ---------- helpers ----------
const safeJsonParse = (s) => {
  if (typeof s !== "string") return null;
  try {
    return JSON.parse(s);
  } catch {
    return null;
  }
};

const uid = () => {
  if (typeof crypto !== "undefined" && crypto.randomUUID) return crypto.randomUUID();
  return `id-${Date.now()}-${Math.floor(Math.random() * 1e9)}`;
};

// Backend stores assistant messages as JSON string like:
// {"type":"search_results","text":"...","videos":[...]}
// We normalize everything to: { role, text, type, meta, id/clientId }
const normalizeMessage = (m) => {
  const raw = m?.content ?? m?.text ?? m?.message ?? "";
  const parsed = safeJsonParse(raw);

  // If stored structured JSON
  if (parsed && typeof parsed === "object" && typeof parsed.text === "string") {
    return {
      id: m?.id ?? null,
      clientId: m?.clientId ?? uid(),
      role: m?.role ?? "assistant",
      text: parsed.text,
      type: parsed.type ?? "text",
      meta: parsed,
      createdAt: m?.createdAt ?? null,
    };
  }

  return {
    id: m?.id ?? null,
    clientId: m?.clientId ?? uid(),
    role: m?.role ?? "assistant",
    text: typeof raw === "string" ? raw : String(raw ?? ""),
    type: "text",
    meta: null,
    createdAt: m?.createdAt ?? null,
  };
};

const formatNumber = (n) => {
  if (n == null) return "0";
  const x = Number(n);
  if (Number.isFinite(x)) return x.toLocaleString();
  return String(n);
};

function MetaBlock({ meta }) {
  if (!meta) return null;
  const type = meta.type ?? "text";

  if (type === "search_results") {
    const videos = Array.isArray(meta.videos) ? meta.videos : [];
    return (
      <div className="meta">
        <div className="metaHeader">
          <div className="metaTitle">🔎 Search Results</div>
          <div className="metaSub">
            “{meta.query ?? ""}” • {videos.length} videos
          </div>
        </div>

        <div className="videoGrid">
          {videos.map((v, idx) => (
            <VideoCard key={v.id ?? `${meta.query ?? "videos"}-${idx}`} v={v} />
          ))}
        </div>
      </div>
    );
  }

  if (type === "comments") {
    const comments = Array.isArray(meta.comments) ? meta.comments : [];
    return (
      <div className="meta">
        <div className="metaHeader">
          <div className="metaTitle">💬 Comments</div>
          <div className="metaSub">
            Video: {meta.video_id ?? ""} • {comments.length} comments
          </div>
        </div>

        <div className="commentList">
          {comments.map((c, idx) => (
            <CommentCard key={`${c.author ?? "a"}-${c.published ?? idx}-${idx}`} c={c} />
          ))}
        </div>
      </div>
    );
  }

  if (type === "video_details") {
    const v = Array.isArray(meta.videos) ? meta.videos[0] : meta.video_data;
    if (!v) return null;
    return (
      <div className="meta">
        <div className="metaHeader">
          <div className="metaTitle">📺 Video Details</div>
        </div>
        <div className="videoGrid">
          <VideoCard key={v.id ?? "video"} v={v} />
        </div>
      </div>
    );
  }

  return null;
}

function VideoCard({ v }) {
  const stats = v.stats ?? {};
  const url = v.id ? `https://www.youtube.com/watch?v=${v.id}` : null;

  return (
    <div className="card videoCard">
      <div className="thumbWrap">
        {v.thumbnail ? (
          <img className="thumb" src={v.thumbnail} alt={v.title ?? "thumbnail"} loading="lazy" />
        ) : (
          <div className="thumb thumbPlaceholder" />
        )}
      </div>

      <div className="videoInfo">
        <div className="videoTitle" title={v.title ?? ""}>
          {v.title ?? "Untitled"}
        </div>

        <div className="videoMeta">
          <span className="muted">Channel:</span> {v.channel ?? "N/A"}
          <span className="sep">•</span>
          <span className="muted">Views:</span> {formatNumber(stats.views)}
          <span className="sep">•</span>
          <span className="muted">Likes:</span> {formatNumber(stats.likes)}
        </div>

        {v.description ? (
          <div className="videoDesc" title={v.description}>
            {v.description}
          </div>
        ) : null}

        <div className="videoActions">
          {url ? (
            <a className="link" href={url} target="_blank" rel="noreferrer">
              Open on YouTube
            </a>
          ) : null}
          {v.id ? <span className="mutedSmall">ID: {v.id}</span> : null}
        </div>
      </div>
    </div>
  );
}

function CommentCard({ c }) {
  return (
    <div className="card commentCard">
      <div className="commentTop">
        <div className="commentAuthor">{c.author ?? "Unknown"}</div>
        <div className="commentLikes">❤️ {formatNumber(c.likes ?? 0)}</div>
      </div>
      <div className="commentText">
        {c.text ?? c.comment ?? c.content ?? "No text"}
      </div>
    </div>
  );
}

// ---------- main ----------
export default function App() {
  const [token, setToken] = useState(() => localStorage.getItem("yt_token") ?? "");
  const [me, setMe] = useState(() => localStorage.getItem("yt_user") ?? "");

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  const [conversations, setConversations] = useState([]);
  const [activeConvId, setActiveConvId] = useState(null);

  const [newTitle, setNewTitle] = useState("YouTube Research");
  const [messages, setMessages] = useState([]);
  const [draft, setDraft] = useState("");

  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const chatRef = useRef(null);
  const streamAbortRef = useRef(null);

  const authHeaders = useMemo(() => {
    return token
      ? {
          Authorization: `Bearer ${token}`,
        }
      : {};
  }, [token]);

  // scroll helper
  const scrollToBottom = () => {
    const el = chatRef.current;
    if (!el) return;
    requestAnimationFrame(() => {
      el.scrollTop = el.scrollHeight;
    });
  };

  // ---------- API calls ----------
  const api = {
    register: async (u, p) => {
      const res = await fetch(`${API_BASE}/users/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: u, password: p }),
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data.message || "Registration failed");
      return data;
    },

    login: async (u, p) => {
      const res = await fetch(`${API_BASE}/users/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: u, password: p }),
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data.message || "Invalid credentials");
      if (!data.token) throw new Error("No token returned from server");
      return data.token;
    },

    listConversations: async () => {
      const res = await fetch(`${API_BASE}/conversations`, {
        headers: { ...authHeaders },
      });
      const data = await res.json().catch(() => []);
      if (!res.ok) throw new Error(data.message || "Failed to load conversations");
      return Array.isArray(data) ? data : data.conversations ?? [];
    },

    createConversation: async (title) => {
      const res = await fetch(`${API_BASE}/conversations`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...authHeaders },
        body: JSON.stringify({ title }),
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data.message || "Failed to create conversation");
      return data; // expects {id, title, ...}
    },

    deleteConversation: async (id) => {
      const res = await fetch(`${API_BASE}/conversations/${id}`, {
        method: "DELETE",
        headers: { ...authHeaders },
      });
      // backend might return empty
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data.message || "Failed to delete conversation");
      }
      return true;
    },

    // IMPORTANT: adjust this endpoint if your backend path differs
    fetchMessages: async (conversationId) => {
      const res = await fetch(`${API_BASE}/conversations/${conversationId}/messages`, {
        headers: { ...authHeaders },
      });
      const data = await res.json().catch(() => []);
      if (!res.ok) throw new Error(data.message || "Failed to load messages");

      const list = Array.isArray(data) ? data : data.messages ?? [];
      return list.map(normalizeMessage);
    },
  };

  // ---------- lifecycle ----------
  const refreshConversations = async () => {
    if (!token) return;
    setError("");
    try {
      const list = await api.listConversations();
      // make newest first if backend doesn't
      const sorted = [...list].sort((a, b) => (b.id ?? 0) - (a.id ?? 0));
      setConversations(sorted);

      // auto-select first if none
      if (!activeConvId && sorted.length) setActiveConvId(sorted[0].id);
    } catch (e) {
      setError(String(e.message || e));
    }
  };

  useEffect(() => {
    refreshConversations();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  useEffect(() => {
    // stop any active stream when switching conversations
    if (streamAbortRef.current) {
      streamAbortRef.current.abort();
      streamAbortRef.current = null;
    }

    const load = async () => {
      if (!token || !activeConvId) {
        setMessages([]);
        return;
      }
      setError("");
      setBusy(true);
      try {
        const msgs = await api.fetchMessages(activeConvId);
        setMessages(msgs);
        setTimeout(scrollToBottom, 0);
      } catch (e) {
        setError(String(e.message || e));
      } finally {
        setBusy(false);
      }
    };

    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeConvId, token]);

  // ---------- actions ----------
  const onRegister = async () => {
    setError("");
    try {
      if (!username || !password) throw new Error("Enter username + password");
      await api.register(username, password);
      setPassword("");
    } catch (e) {
      setError(String(e.message || e));
    }
  };

  const onLogin = async () => {
    setError("");
    try {
      if (!username || !password) throw new Error("Enter username + password");
      const t = await api.login(username, password);
      setToken(t);
      setMe(username);
      localStorage.setItem("yt_token", t);
      localStorage.setItem("yt_user", username);
      setPassword("");
    } catch (e) {
      setError(String(e.message || e));
    }
  };

  const onLogout = () => {
    if (streamAbortRef.current) {
      streamAbortRef.current.abort();
      streamAbortRef.current = null;
    }
    setToken("");
    setMe("");
    setConversations([]);
    setActiveConvId(null);
    setMessages([]);
    localStorage.removeItem("yt_token");
    localStorage.removeItem("yt_user");
  };

  const onCreateConversation = async () => {
    setError("");
    try {
      if (!token) throw new Error("Login first");
      const title = (newTitle || "").trim() || "New Chat";
      const conv = await api.createConversation(title);

      // refresh list and select new one
      await refreshConversations();
      setActiveConvId(conv.id);
      setMessages([]);
      setDraft("");
    } catch (e) {
      setError(String(e.message || e));
    }
  };

  const onDeleteConversation = async (id) => {
    setError("");
    try {
      if (!token) throw new Error("Login first");
      const ok = window.confirm("Delete this conversation? This cannot be undone.");
      if (!ok) return;

      await api.deleteConversation(id);

      // if we deleted active, select another
      setConversations((prev) => prev.filter((c) => c.id !== id));
      if (activeConvId === id) {
        const remaining = conversations.filter((c) => c.id !== id);
        setActiveConvId(remaining[0]?.id ?? null);
        setMessages([]);
      }
    } catch (e) {
      // NOTE: if backend doesn't cascade delete messages, you'll get 500 here.
      // That must be fixed in DB (ON DELETE CASCADE) or delete messages first server-side.
      setError(String(e.message || e));
    }
  };

  // Streaming with Authorization headers (EventSource cannot do headers)
  const streamSendMessage = async (conversationId, message) => {
    const controller = new AbortController();
    streamAbortRef.current = controller;

    // optimistic UI: user msg
    const userMsg = {
      clientId: uid(),
      role: "user",
      text: message,
      type: "text",
      meta: null,
    };

    // assistant placeholder
    const assistantClientId = uid();
    const assistantMsg = {
      clientId: assistantClientId,
      role: "assistant",
      text: "",
      type: "text",
      meta: null,
    };

    setMessages((prev) => [...prev, userMsg, assistantMsg]);
    setTimeout(scrollToBottom, 0);

    let currentText = "";
    let currentMeta = null;

    const res = await fetch(`${API_BASE}/conversations/${conversationId}/send-message/stream`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "text/event-stream",
        ...authHeaders,
      },
      body: JSON.stringify({ message }),
      signal: controller.signal,
    });

    if (!res.ok || !res.body) {
      const t = await res.text().catch(() => "");
      throw new Error(`Stream failed: ${res.status} ${t}`);
    }

    const reader = res.body.getReader();
    const decoder = new TextDecoder("utf-8");

    let buffer = "";

    const applyAssistantUpdate = () => {
      setMessages((prev) =>
        prev.map((m) => {
          if (m.clientId !== assistantClientId) return m;
          return {
            ...m,
            text: currentText,
            type: currentMeta?.type ?? m.type,
            meta: currentMeta ?? m.meta,
          };
        })
      );
      scrollToBottom();
    };

    const handleEvent = (eventName, eventData) => {
      if (eventName === "token") {
        const token = eventData;
        
        // Add space between tokens intelligently
        if (currentText.length > 0) {
          const lastChar = currentText[currentText.length - 1];
          const firstChar = token[0];
          
          // Don't add space before punctuation and common symbols
          const isPunctuation = /[!?,.:;)\]}'"/-]/.test(firstChar);
          
          const needsSpace =
            lastChar !== " " &&
            lastChar !== "\n" &&
            !isPunctuation &&
            token !== "\n";
          
          if (needsSpace) {
            currentText += " ";
          }
        }
        
        currentText += token;
        applyAssistantUpdate();
        return;
      }

      if (eventName === "metadata") {
        const parsed = safeJsonParse(eventData);
        if (parsed) currentMeta = parsed;
        applyAssistantUpdate();
        return;
      }

      if (eventName === "error") {
        throw new Error(eventData || "Streaming error");
      }

      if (eventName === "done") {
        return;
      }
    };

    // Parse SSE: supports event + data lines
    // Example:
    // event: token
    // data: hello
    //
    let currentEventName = "message";
    let currentData = "";

    while (true) {
      const { value, done } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });

      // SSE events end with blank line
      let idx;
      while ((idx = buffer.indexOf("\n\n")) !== -1) {
        const rawEvent = buffer.slice(0, idx);
        buffer = buffer.slice(idx + 2);

        const lines = rawEvent.split("\n").map((l) => l.trimEnd());
        currentEventName = "message";
        currentData = "";

        for (const line of lines) {
          if (line.startsWith("event:")) {
            currentEventName = line.slice(6).trim();
          } else if (line.startsWith("data:")) {
            // data can repeat; concatenate with newline like SSE spec
            const part = line.slice(5);
            currentData += (currentData ? "\n" : "") + part;
          }
        }

        const dataStr = currentData?.trimStart() ?? "";
        handleEvent(currentEventName, dataStr);

        if (currentEventName === "done") {
          buffer = "";
          break;
        }
      }
    }

    streamAbortRef.current = null;
  };

  const onSend = async () => {
    setError("");
    try {
      if (!token) throw new Error("Login first");
      if (!activeConvId) throw new Error("Create/select a conversation first");
      const msg = draft.trim();
      if (!msg) return;
      setDraft("");
      setBusy(true);
      await streamSendMessage(activeConvId, msg);

      // optional: re-fetch messages from server to keep perfect parity with DB
      // const msgs = await api.fetchMessages(activeConvId);
      // setMessages(msgs);

    } catch (e) {
      setError(String(e.message || e));
    } finally {
      setBusy(false);
    }
  };

  const activeTitle = useMemo(() => {
    const c = conversations.find((x) => x.id === activeConvId);
    return c?.title ?? "—";
  }, [conversations, activeConvId]);

  // ---------- render ----------
  return (
    <div className="app">
      <aside className="sidebar">
        <div className="brand">
          <div className="brandTitle">YouTube Research</div>
          <div className="brandSub">{me ? `✅ Logged in as ${me}` : "🔴 Not logged in"}</div>
        </div>

        <div className="auth">
          <input className="input" placeholder="Username" value={username} onChange={(e) => setUsername(e.target.value)} />
          <input className="input" placeholder="Password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />

          {/* Top row: Register & Login */}
          <div className="row">
            <button className="btn success" onClick={onRegister} disabled={busy}>
              Register
            </button>
            <button className="btn primary" onClick={onLogin} disabled={busy}>
              Login
            </button>
          </div>

          {/* Bottom row: Logout (separate, full-width) */}
          <button 
            className="btn" 
            onClick={onLogout} 
            disabled={!token || busy}
            style={{ marginTop: "8px", width: "100%" }} // Full width, below
          >
            Logout
          </button>
        </div>

        <div className="newChat">
          <div className="sectionTitle">NEW CHAT</div>
          <div className="row">
            <input
              className="input"
              placeholder="Conversation title"
              value={newTitle}
              onChange={(e) => setNewTitle(e.target.value)}
            />
            <button className="btn warning" onClick={onCreateConversation} disabled={!token || busy}>
              Create
            </button>
          </div>
        </div>

        <div className="sectionTitle">CHATS</div>
        <div className="convList">
          {conversations.map((c) => {
            const active = c.id === activeConvId;
            return (
              <div
                key={c.id}
                className={`convItem ${active ? "active" : ""}`}
                onClick={() => setActiveConvId(c.id)}
                role="button"
                tabIndex={0}
              >
                <div className="convTop">
                  <div className="convTitle">{c.title ?? `Conversation ${c.id}`}</div>
                  <button
                    className="iconBtn"
                    title="Delete"
                    onClick={(e) => {
                      e.stopPropagation();
                      onDeleteConversation(c.id);
                    }}
                  >
                    🗑️
                  </button>
                </div>
                <div className="convSub">ID: {c.id}</div>
              </div>
            );
          })}
          {!token ? <div className="mutedSmall">Login to see your conversations.</div> : null}
          {token && conversations.length === 0 ? (
            <div className="mutedSmall">No conversations yet. Create one.</div>
          ) : null}
        </div>

        {error ? <div className="errorBox">❌ {error}</div> : null}
      </aside>

      <main className="main">
        <div className="topbar">
          <div className="crumb">
            Conversation <span className="pill">{activeTitle}</span>
          </div>
          {busy ? <div className="pill">Working…</div> : null}
        </div>

        <div className="chat" ref={chatRef}>
          {messages.map((m) => {
            const key = m.id ?? m.clientId;
            return (
              <div key={key} className="msgWrap">
                {/* Render meta block above assistant bubble (and when reopening history) */}
                {m.role === "assistant" && m.meta ? <MetaBlock meta={m.meta} /> : null}

                <div className={`bubble ${m.role === "user" ? "user" : "assistant"}`}>
                  <div className="bubbleHeader">{m.role === "user" ? "👤 You" : "🤖 Assistant"}</div>
                  <div className="bubbleBody">{m.text}</div>
                </div>
              </div>
            );
          })}
        </div>

        <div className="composer">
          <input
            className="composerInput"
            placeholder={token ? "Type a message…" : "Login to chat…"}
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") onSend();
            }}
            disabled={!token || !activeConvId || busy}
          />
          <button className="btn primary" onClick={onSend} disabled={!token || !activeConvId || busy}>
            Send
          </button>
        </div>
      </main>
    </div>
  );
}
