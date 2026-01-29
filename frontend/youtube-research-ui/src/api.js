const API_URL = "/api";

async function readJson(res) {
  const text = await res.text().catch(() => "");
  try {
    return text ? JSON.parse(text) : {};
  } catch {
    return { raw: text };
  }
}

export async function register(username, password) {
  const res = await fetch(`${API_URL}/users/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });
  const data = await readJson(res);
  if (!res.ok) throw new Error(data.message || "Registration failed");
  return data;
}

export async function login(username, password) {
  const res = await fetch(`${API_URL}/users/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });
  const data = await readJson(res);
  if (!res.ok) throw new Error(data.message || "Invalid credentials");
  if (!data.token) throw new Error("No token returned from server");
  return data.token;
}

export async function createConversation(token, title) {
  const res = await fetch(`${API_URL}/conversations`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ title }),
  });
  const data = await readJson(res);
  if (!res.ok) throw new Error(data.message || "Failed to create conversation");
  return data; // expects {id, title, ...}
}

export async function listConversations(token) {
  const res = await fetch(`${API_URL}/conversations`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const data = await readJson(res);
  if (!res.ok) throw new Error(data.message || "Failed to load conversations");
  // allow either {items:[...]} or [...]
  return Array.isArray(data) ? data : data.items || [];
}

export async function deleteConversation(token, conversationId) {
  const res = await fetch(`${API_URL}/conversations/${conversationId}`, {
    method: "DELETE",
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) {
    const data = await readJson(res);
    throw new Error(data.message || "Failed to delete conversation");
  }
  return true;
}

export async function getConversationMessages(token, conversationId) {
  const res = await fetch(
    `${API_URL}/conversations/${conversationId}/messages`,
    {
      headers: { Authorization: `Bearer ${token}` },
    },
  );
  const data = await readJson(res);
  if (!res.ok) throw new Error(data.message || "Failed to load messages");
  return Array.isArray(data) ? data : data.items || [];
}

export function streamMessageUrl(conversationId) {
  return `${API_URL}/conversations/${conversationId}/send-message/stream`;
}
