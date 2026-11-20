/* eslint-disable no-undef */
// Service Worker لإشعارات الدفع الفورية (Firebase Cloud Messaging) - منصة طب الحجايزة (v1.0.0)
// معزول تماماً تحت مسار/مساحة: teb_alhajayzah

importScripts('https://www.gstatic.com/firebasejs/10.13.2/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.13.2/firebase-messaging-compat.js');

const TEB_FCM_DB = 'teb_alhajayzah_fcm_db';
const TEB_FCM_STORE = 'config_store';
const TEB_FCM_KEY = 'teb_alhajayzah/config/firebase';
let initializedMessaging = null;
let cachedFirebaseConfig = null;

function openConfigDb() {
  return new Promise((resolve, reject) => {
    try {
      const req = indexedDB.open(TEB_FCM_DB, 1);
      req.onupgradeneeded = function (e) {
        const db = e.target.result;
        if (!db.objectStoreNames.contains(TEB_FCM_STORE)) {
          db.createObjectStore(TEB_FCM_STORE);
        }
      };
      req.onsuccess = (e) => resolve(e.target.result);
      req.onerror = (e) => reject(e.target.error);
    } catch (err) {
      reject(err);
    }
  });
}

async function saveDynamicConfig(cfg) {
  if (!cfg || !cfg.apiKey || !cfg.projectId || !cfg.messagingSenderId || !cfg.appId) return;
  cachedFirebaseConfig = cfg;
  try {
    const db = await openConfigDb();
    await new Promise((resolve) => {
      const tx = db.transaction(TEB_FCM_STORE, 'readwrite');
      tx.objectStore(TEB_FCM_STORE).put(cfg, TEB_FCM_KEY);
      tx.oncomplete = () => resolve();
      tx.onerror = () => resolve();
    });
  } catch (e) { /* ignore idb write error */ }
}

async function loadDynamicConfig() {
  if (cachedFirebaseConfig) return cachedFirebaseConfig;
  try {
    const url = new URL(self.location.href);
    const paramCfg = {
      apiKey: url.searchParams.get('apiKey') || '',
      authDomain: url.searchParams.get('authDomain') || '',
      projectId: url.searchParams.get('projectId') || '',
      storageBucket: url.searchParams.get('storageBucket') || '',
      messagingSenderId: url.searchParams.get('messagingSenderId') || '',
      appId: url.searchParams.get('appId') || ''
    };
    if (paramCfg.apiKey && paramCfg.projectId && paramCfg.messagingSenderId && paramCfg.appId) {
      cachedFirebaseConfig = paramCfg;
      return paramCfg;
    }
  } catch (e) { /* ignore */ }
  try {
    const db = await openConfigDb();
    const cfg = await new Promise((resolve) => {
      const tx = db.transaction(TEB_FCM_STORE, 'readonly');
      const req = tx.objectStore(TEB_FCM_STORE).get(TEB_FCM_KEY);
      req.onsuccess = () => resolve(req.result || null);
      req.onerror = () => resolve(null);
    });
    if (cfg && cfg.apiKey && cfg.projectId && cfg.messagingSenderId && cfg.appId) {
      cachedFirebaseConfig = cfg;
      return cfg;
    }
  } catch (e) { /* ignore */ }
  return null;
}

function initFirebaseMessagingWithConfig(cfg) {
  if (!cfg || !cfg.apiKey || !cfg.projectId || !cfg.messagingSenderId || !cfg.appId) return null;
  if (initializedMessaging) return initializedMessaging;
  try {
    if (!firebase.apps || firebase.apps.length === 0) {
      firebase.initializeApp({
        apiKey: cfg.apiKey,
        authDomain: cfg.authDomain || (cfg.projectId + '.firebaseapp.com'),
        projectId: cfg.projectId,
        storageBucket: cfg.storageBucket || (cfg.projectId + '.appspot.com'),
        messagingSenderId: cfg.messagingSenderId,
        appId: cfg.appId
      });
    }
    const messaging = firebase.messaging();
    messaging.onBackgroundMessage((payload) => {
      const notif = (payload && payload.notification) || {};
      const data = (payload && payload.data) || {};
      const title = notif.title || data.title || 'طب الحجايزة — إشعار جديد';
      const body = notif.body || data.body || 'لديك تحديث جديد بشأن مواعيد العيادات أو ملفك الطبي.';
      self.registration.showNotification(title, {
        body: body,
        icon: './icon.svg',
        badge: './icon.svg',
        dir: 'rtl',
        lang: 'ar',
        tag: data.tag || 'teb-alhajayzah-alert',
        data: {
          namespace: 'teb_alhajayzah',
          url: data.url || './'
        }
      });
    });
    initializedMessaging = messaging;
    return messaging;
  } catch (e) {
    return null;
  }
}

self.addEventListener('install', () => {
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    await self.clients.claim();
    const cfg = await loadDynamicConfig();
    if (cfg) initFirebaseMessagingWithConfig(cfg);
  })());
});

self.addEventListener('message', (event) => {
  const data = event && event.data;
  if (!data) return;
  if (data.type === 'TEB_INIT_FIREBASE' && data.config) {
    event.waitUntil(
      saveDynamicConfig(data.config).then(() => initFirebaseMessagingWithConfig(data.config))
    );
  }
});

// Fallback handler if push arrives before Firebase SDK finishes initializing
self.addEventListener('push', (event) => {
  if (initializedMessaging) return;
  let payload = {};
  try {
    payload = event.data ? event.data.json() : {};
  } catch (e) {
    payload = { notification: { title: 'طب الحجايزة', body: event.data ? event.data.text() : '' } };
  }
  const notif = (payload && payload.notification) || {};
  const data = (payload && payload.data) || {};
  if (!notif.title && !notif.body && !data.title && !data.body) return;
  event.waitUntil(
    self.registration.showNotification(notif.title || data.title || 'طب الحجايزة', {
      body: notif.body || data.body || '',
      icon: './icon.svg',
      badge: './icon.svg',
      dir: 'rtl',
      lang: 'ar',
      data: { namespace: 'teb_alhajayzah', url: data.url || './' }
    })
  );
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const targetUrl = (event.notification.data && event.notification.data.url) || './';
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientsList) => {
      for (const client of clientsList) {
        if ('focus' in client) return client.focus();
      }
      if (self.clients.openWindow) return self.clients.openWindow(targetUrl);
      return Promise.resolve();
    })
  );
});
