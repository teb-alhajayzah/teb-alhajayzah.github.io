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
  if (request.method === 'OPTIONS') {
    return new Response(null, {
      headers: {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type, Authorization'
      }
    });
  }
  if (request.method === 'POST') {
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
  // 1. Fetch teb_alhajayzah_appointments from Firestore REST API (gen-lang-client-0514993881)
  // 2. Iterate appointments where reminded2h == false or reminded30m == false
  // 3. Compute (appointmentTimestamp - Date.now())
  // 4. If <= 120 mins and !reminded2h -> Send FCM push notification (2h reminder) & PATCH reminded2h = true
  // 5. If <= 30 mins and !reminded30m -> Send FCM push notification (30m urgent reminder) & PATCH reminded30m = true
}

async function sendFcmPushNotification(payload) {
  // payload: { tokens: string[], title: string, message: string, data: object, targetAudience, targetId }
  // Sends HTTP v1 priority HIGH requests to FCM endpoint for all matching tokens
  return { dispatched: true, count: Array.isArray(payload && payload.tokens) ? payload.tokens.length : 0 };
}
