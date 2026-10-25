// SmartToolBD service worker. Change VERSION whenever you publish new files.
const VERSION = "2026-10-25-1";
const CACHE = "smarttoolbd-" + VERSION;
const SHELL = ["./", "index.html", "manifest.json", "icon-192.png", "icon-512.png"];
const SCOPE_PATH = new URL("./", self.location).pathname;

self.addEventListener("install", e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(SHELL)));
});

self.addEventListener("activate", e => {
  e.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys.filter(k => k.startsWith("smarttoolbd-") && k !== CACHE).map(k => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

// The page asks for activation only when the user taps "Update".
self.addEventListener("message", e => {
  if (e.data && e.data.type === "SKIP_WAITING") self.skipWaiting();
});

self.addEventListener("fetch", e => {
  const req = e.request;
  const url = new URL(req.url);
  if (req.method !== "GET" || url.origin !== self.location.origin || !url.pathname.startsWith(SCOPE_PATH)) return;

  if (req.mode === "navigate") {
    // Network first so updates show; cached shell when offline.
    e.respondWith(
      fetch(req)
        .then(res => { if (res.ok) { const copy = res.clone(); caches.open(CACHE).then(c => c.put("index.html", copy)); } return res; })
        .catch(() => caches.match("index.html"))
    );
    return;
  }

  // Same-origin static files: cached copy first, refreshed in the background.
  e.respondWith(
    caches.match(req).then(hit => {
      const fresh = fetch(req)
        .then(res => { if (res.ok && res.type === "basic") { const copy = res.clone(); caches.open(CACHE).then(c => c.put(req, copy)); } return res; })
        .catch(() => hit);
      return hit || fresh;
    })
  );
});
