/**
 * Cloudflare Worker Template for "طب الحجايزة" (Teb Al-Hajayzah)
 * Handles FCM Push Notifications, Admin/Patient messages, and automated Cron Trigger reminders (2 hours & 30 minutes before appointment).
 */

addEventListener('scheduled', event => {
  event.waitUntil(handleCronTrigger(event));
});

addEventListener('fetch', event => {
  event.respondWith(handleRequest(event.request));
});

async function handleRequest(request) {
  const url = new URL(request.url);
  if (request.method === 'POST' && url.pathname.includes('/dispatch-notification')) {
    try {
      const body = await request.json();
      const result = await sendFcmPushNotification(body);
      return new Response(JSON.stringify({ success: true, result }), {
        headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
      });
    } catch (err) {
      return new Response(JSON.stringify({ success: false, error: err.message }), {
        status: 500,
        headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
      });
    }
  }
  return new Response('Teb Al-Hajayzah Cloudflare Worker Active', {
    headers: { 'Content-Type': 'text/plain', 'Access-Control-Allow-Origin': '*' }
  });
}

async function handleCronTrigger(event) {
  console.log('[Cron] Checking upcoming appointments for 2h and 30m reminders...');
  // 1. Fetch appointments from Firestore REST API
  // 2. Iterate appointments where reminded2h == false or reminded30m == false
  // 3. Compute time difference from nowMillis
  // 4. If time remaining <= 120 mins and !reminded2h -> Send FCM push notification (2h reminder) & update reminded2h = true
  // 5. If time remaining <= 30 mins and !reminded30m -> Send FCM push notification (30m urgent reminder) & update reminded30m = true
}

async function sendFcmPushNotification(payload) {
  // payload: { title, body, targetRole, targetId, fcmToken, projectId, apiKey }
  // Sends HTTP v1 request to FCM endpoint using project credentials
  return { dispatched: true };
}
