/* Smart Tool BD: offline support (service worker).
   Keeps index.html (the nasheed is inside it) on the phone so the site and the nasheed work without internet.
   When you change index.html, change VERSION below so every phone downloads the new copy. */
const VERSION = "stb-v3";
const CORE = ["./", "./index.html"];

self.addEventListener("install", e => {
  e.waitUntil((async () => {
    const c = await caches.open(VERSION);
    const res = await Promise.all(CORE.map(u => c.add(new Request(u, { cache: "reload" })).then(() => true, () => false)));
    self.skipWaiting();
    const all = await self.clients.matchAll({ includeUncontrolled: true });
    all.forEach(cl => cl.postMessage({ type: "offline-ready", ok: res.every(Boolean) }));
  })());
});

self.addEventListener("activate", e => {
  e.waitUntil((async () => {
    for (const k of await caches.keys()) if (k !== VERSION) await caches.delete(k);
    await self.clients.claim();
  })());
});

async function rangeResponse(req, cached) {          // audio needs "Range" answers, the cache does not do that by itself
  const buf = await cached.arrayBuffer();
  const size = buf.byteLength, m = /bytes=(\d*)-(\d*)/.exec(req.headers.get("range") || "");
  let start = m && m[1] ? parseInt(m[1], 10) : 0, end = m && m[2] ? parseInt(m[2], 10) : size - 1;
  if (m && !m[1] && m[2]) { start = Math.max(0, size - parseInt(m[2], 10)); end = size - 1; }
  end = Math.min(end, size - 1);
  if (start > end) return new Response(null, { status: 416, headers: { "Content-Range": "bytes */" + size } });
  return new Response(buf.slice(start, end + 1), { status: 206, statusText: "Partial Content", headers: {
    "Content-Type": cached.headers.get("Content-Type") || "audio/mpeg", "Content-Length": String(end - start + 1),
    "Content-Range": `bytes ${start}-${end}/${size}`, "Accept-Ranges": "bytes" } });
}

self.addEventListener("fetch", e => {
  const req = e.request;
  if (req.method !== "GET") return;
  const url = new URL(req.url);
  const sameOrigin = url.origin === self.location.origin;
  const fonts = url.hostname === "fonts.googleapis.com" || url.hostname === "fonts.gstatic.com";
  if (!sameOrigin && !fonts) return;                    // ads and everything else: normal network
  e.respondWith((async () => {
    const cache = await caches.open(VERSION);
    // page: network first (so updates arrive), cached copy when offline
    if (req.mode === "navigate") {
      try { const r = await fetch(req); if (r.ok) cache.put("./index.html", r.clone()); return r; }
      catch (err) { return (await cache.match("./index.html")) || (await cache.match("./")) || Response.error(); }
    }
    // the nasheed: cache first, with Range support
    if (sameOrigin && url.pathname.endsWith("/nasheed.mp3")) {
      const hit = await cache.match("./nasheed.mp3");
      if (hit) return req.headers.get("range") ? rangeResponse(req, hit) : hit;
      try {
        const full = await fetch(new Request(req.url, { cache: "reload" }));
        if (full.ok && full.status === 200) { cache.put("./nasheed.mp3", full.clone()); return req.headers.get("range") ? rangeResponse(req, full.clone()) : full; }
        return full;
      } catch (err) { return Response.error(); }
    }
    // everything else on this site and the fonts: use the saved copy, refresh it in the background
    const hit = await cache.match(req);
    const net = fetch(req).then(r => { if (r && (r.ok || r.type === "opaque")) cache.put(req, r.clone()); return r; }).catch(() => null);
    return hit || (await net) || Response.error();
  })());
});
