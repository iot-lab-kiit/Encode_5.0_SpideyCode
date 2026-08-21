const CACHE_NAME = 'spideycode-pwa-v1';
const ASSETS_TO_CACHE = [
  '/',
  '/manifest.webmanifest',
  '/assets/masks/spidey_mask.png',
  '/assets/logos/algozenith_logo.png',
  '/assets/logos/iot_logo.png',
  '/assets/logos/ksac_logo.webp',
  '/assets/logos/encodexzenith_logo.png',
  '/assets/images/frame_1_2.png',
  '/assets/images/frame_readymade.png',
  '/assets/images/frame_minispider.png',
  '/assets/images/frame_spidergirl.png',
  '/assets/images/frame_event_squad.png',
  '/assets/images/frame_spidey_party.png',
  '/assets/images/frame_ghost_spider.png',
  '/assets/images/spider_verse_bg.jpg',
  '/assets/animations/spider_walk.json',
  '/assets/branding.json',
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(ASSETS_TO_CACHE);
    })
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.map((key) => {
          if (key !== CACHE_NAME) {
            return caches.delete(key);
          }
        })
      );
    })
  );
  self.clients.claim();
});

self.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET') return;

  const url = new URL(event.request.url);

  // Cache-first strategy for static assets
  if (url.pathname.startsWith('/assets/')) {
    event.respondWith(
      caches.match(event.request).then((cachedResponse) => {
        if (cachedResponse) return cachedResponse;
        return fetch(event.request).then((networkResponse) => {
          if (networkResponse && networkResponse.status === 200) {
            const responseClone = networkResponse.clone();
            caches.open(CACHE_NAME).then((cache) => {
              cache.put(event.request, responseClone);
            });
          }
          return networkResponse;
        });
      })
    );
    return;
  }

  // Network-first for pages and other requests
  event.respondWith(
    fetch(event.request)
      .then((networkResponse) => {
        return networkResponse;
      })
      .catch(() => {
        return caches.match(event.request).then((cachedResponse) => {
          return cachedResponse || caches.match('/');
        });
      })
  );
});
