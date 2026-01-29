export async function streamSSE(url, fetchOptions, onEvent) {
  const res = await fetch(url, {
    ...fetchOptions,
    headers: {
      Accept: "text/event-stream",
      "Content-Type": "application/json",
      ...(fetchOptions.headers || {}),
    },
  });

  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(`HTTP ${res.status}: ${text || res.statusText}`);
  }

  if (!res.body) throw new Error("Streaming not supported");

  const reader = res.body.getReader();
  const decoder = new TextDecoder("utf-8");

  let buffer = "";

  try {
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });

      let sep;
      while ((sep = buffer.indexOf("\n\n")) !== -1) {
        const rawEvent = buffer.slice(0, sep);
        buffer = buffer.slice(sep + 2);

        let event = "message";
        let data = "";

        for (const line of rawEvent.split("\n")) {
          if (line.startsWith("event:")) {
            event = line.slice(6).trim();
          } else if (line.startsWith("data:")) {
            // IMPORTANT: keep EXACTLY what server sends
            data += line.slice(5);
          }
        }

        // DO NOT DROP newline tokens
        if (data !== undefined) {
          onEvent({ event, data });
        }
      }
    }
  } finally {
    reader.releaseLock();
  }
}
