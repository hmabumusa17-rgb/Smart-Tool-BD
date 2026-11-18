/* SmartToolBD offline app shell — keep this file beside index.html */
const CACHE_NAME = "smarttoolbd-shell-v2";
const APP_SHELL = ["./", "./index.html"];

self.addEventListener("install", event => {
  event.waitUntil((async () => {
    const cache = await caches.open(CACHE_NAME);
    await cache.addAll(APP_SHELL);
    await self.skipWaiting();
  })());
});

self.addEventListener("activate", event => {
  event.waitUntil((async () => {
    const keys = await caches.keys();
    await Promise.all(
      keys
        .filter(key => key.startsWith("smarttoolbd-shell-") && key !== CACHE_NAME)
        .map(key => caches.delete(key))
    );
    await self.clients.claim();

    const clients = await self.clients.matchAll({
      type: "window",
      includeUncontrolled: true
    });

    for (const client of clients) {
      client.postMessage({ type: "offline-ready", ok: true });
    }
  })());
});

self.addEventListener("fetch", event => {
  const request = event.request;
  const url = new URL(request.url);

  if (request.method !== "GET" || url.origin !== self.location.origin) return;

  if (request.mode === "navigate") {
    event.respondWith((async () => {
      try {
        const response = await fetch(request);

        if (response && response.ok) {
          const cache = await caches.open(CACHE_NAME);
          cache.put("./index.html", response.clone());
        }

        return response;
      } catch (error) {
        return (await caches.match("./index.html")) ||
               (await caches.match("./"));
      }
    })());

    return;
  }

  event.respondWith((async () => {
    const cached = await caches.match(request);
    if (cached) return cached;

    try {
      const response = await fetch(request);

      if (response && response.ok) {
        const cache = await caches.open(CACHE_NAME);
        cache.put(request, response.clone());
      }

      return response;
    } catch (error) {
      return Response.error();
    }
  })());
});
