/**
 * Cloudflare Worker for "طب الحجايزة" (Teb Al-Hajayzah)
 * Project: gen-lang-client-0514993881
 *
 * Features:
 * 1. Google Service Account OAuth2 (RS256 JWT) with scopes:
 *    "https://www.googleapis.com/auth/firebase.messaging https://www.googleapis.com/auth/datastore"
 * 2. Multi-token FCM HTTP v1 Push Notifications (`tokens: string[]`) with HIGH priority
 *    and Android channel `teb_alhajayzah_high_priority`.
 * 3. `/check-reminders` endpoint & Cloudflare Cron Trigger for automatic 2-hour (`reminded2h`)
 *    and 30-minute (`reminded30m`) appointment reminders via Firestore REST API.
 *
 * Required Cloudflare Worker Secrets / Environment Variables:
 * - FIREBASE_SERVICE_ACCOUNT_JSON  (Full JSON of Firebase Service Account — recommended)
 *   OR individual variables:
 * - FIREBASE_PROJECT_ID            (defaults to "gen-lang-client-0514993881")
 * - FIREBASE_CLIENT_EMAIL
 * - FIREBASE_PRIVATE_KEY
 */

const DEFAULT_PROJECT_ID = 'gen-lang-client-0514993881';
const DEFAULT_DATABASE_ID = '(default)';
const GOOGLE_OAUTH_TOKEN_URL = 'https://oauth2.googleapis.com/token';
const OAUTH_SCOPES = 'https://www.googleapis.com/auth/firebase.messaging https://www.googleapis.com/auth/datastore';
const ANDROID_CHANNEL_ID = 'teb_alhajayzah_high_priority';
const APPOINTMENTS_COLLECTION = 'teb_alhajayzah_appointments';
const FCM_TOKENS_COLLECTION = 'teb_alhajayzah_fcm_tokens';

const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization'
};

let cachedOAuthToken = '';
let cachedOAuthExpiryMs = 0;

export default {
  async fetch(request, env, ctx) {
    return await handleRequest(request, env, ctx);
  },

  async scheduled(event, env, ctx) {
    ctx.waitUntil(checkAndDispatchAppointmentReminders(env));
  }
};

function jsonResponse(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      ...CORS_HEADERS
    }
  });
}

function getEnvValue(env, key) {
  if (env && env[key] !== undefined && env[key] !== null && String(env[key]).trim() !== '') {
    return String(env[key]).trim();
  }
  if (typeof globalThis !== 'undefined' && globalThis[key] !== undefined && globalThis[key] !== null) {
    const val = String(globalThis[key]).trim();
    if (val !== '') return val;
  }
  return '';
}

function readWorkerConfig(env, projectIdOverride = '') {
  let projectId = String(projectIdOverride || '').trim() || getEnvValue(env, 'FIREBASE_PROJECT_ID') || DEFAULT_PROJECT_ID;
  let databaseId = getEnvValue(env, 'FIREBASE_DATABASE_ID') || DEFAULT_DATABASE_ID;
  let clientEmail = getEnvValue(env, 'FIREBASE_CLIENT_EMAIL');
  let privateKey = getEnvValue(env, 'FIREBASE_PRIVATE_KEY');

  const rawSaJson = getEnvValue(env, 'FIREBASE_SERVICE_ACCOUNT_JSON');
  if (rawSaJson) {
    try {
      const sa = JSON.parse(rawSaJson);
      if (!projectIdOverride && sa.project_id) projectId = String(sa.project_id).trim();
      if (sa.client_email) clientEmail = String(sa.client_email).trim();
      if (sa.private_key) privateKey = String(sa.private_key).trim();
    } catch (e) {
      throw new Error('Invalid FIREBASE_SERVICE_ACCOUNT_JSON secret: ' + e.message);
    }
  }

  privateKey = privateKey.replace(/\\n/g, '\n').trim();

  if (!clientEmail || !privateKey) {
    throw new Error('Missing Firebase Service Account credentials (FIREBASE_SERVICE_ACCOUNT_JSON or FIREBASE_CLIENT_EMAIL + FIREBASE_PRIVATE_KEY).');
  }

  return { projectId, databaseId, clientEmail, privateKey };
}

function base64UrlEncodeBytes(bytes) {
  let binary = '';
  const arr = bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes);
  for (let i = 0; i < arr.length; i++) {
    binary += String.fromCharCode(arr[i]);
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
}

function base64UrlEncodeString(str) {
  const encoded = new TextEncoder().encode(str);
  return base64UrlEncodeBytes(encoded);
}

function pemToArrayBuffer(pem) {
  const cleaned = String(pem || '')
    .replace(/-----BEGIN PRIVATE KEY-----/g, '')
    .replace(/-----END PRIVATE KEY-----/g, '')
    .replace(/\s+/g, '');
  const binary = atob(cleaned);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
}

/**
 * Generates and returns a Google OAuth2 `access_token` using RS256 Service Account JWT.
 * Scope: "https://www.googleapis.com/auth/firebase.messaging https://www.googleapis.com/auth/datastore"
 */
async function getGoogleAccessToken(config) {
  const nowMs = Date.now();
  if (cachedOAuthToken && nowMs < cachedOAuthExpiryMs - 300000) {
    return cachedOAuthToken;
  }

  const nowSec = Math.floor(nowMs / 1000);
  const header = { alg: 'RS256', typ: 'JWT' };
  const claimSet = {
    iss: config.clientEmail,
    sub: config.clientEmail,
    scope: OAUTH_SCOPES,
    aud: GOOGLE_OAUTH_TOKEN_URL,
    iat: nowSec,
    exp: nowSec + 3600
  };

  const signingInput = base64UrlEncodeString(JSON.stringify(header)) + '.' + base64UrlEncodeString(JSON.stringify(claimSet));
  const keyBuffer = pemToArrayBuffer(config.privateKey);
  const cryptoKey = await crypto.subtle.importKey(
    'pkcs8',
    keyBuffer,
    { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
    false,
    ['sign']
  );

  const signature = await crypto.subtle.sign(
    'RSASSA-PKCS1-v1_5',
    cryptoKey,
    new TextEncoder().encode(signingInput)
  );

  const jwt = signingInput + '.' + base64UrlEncodeBytes(signature);

  const tokenResp = await fetch(GOOGLE_OAUTH_TOKEN_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
      assertion: jwt
    }).toString()
  });

  const tokenJson = await tokenResp.json();
  if (!tokenResp.ok || !tokenJson || !tokenJson.access_token) {
    const errMsg = (tokenJson && (tokenJson.error_description || tokenJson.error)) || ('HTTP ' + tokenResp.status);
    throw new Error('OAuth token exchange failed: ' + errMsg);
  }

  cachedOAuthToken = String(tokenJson.access_token);
  const expiresInSec = Number(tokenJson.expires_in) || 3600;
  cachedOAuthExpiryMs = nowMs + expiresInSec * 1000;
  return cachedOAuthToken;
}

async function handleRequest(request, env, ctx) {
  if (request.method === 'OPTIONS') {
    return new Response(null, { status: 204, headers: CORS_HEADERS });
  }

  const url = new URL(request.url);
  const pathname = url.pathname.replace(/\/+$/, '') || '/';

  if (pathname === '/check-reminders') {
    try {
      const summary = await checkAndDispatchAppointmentReminders(env);
      return jsonResponse(summary, 200);
    } catch (err) {
      return jsonResponse({ success: false, error: err && err.message ? err.message : String(err) }, 500);
    }
  }

  if (request.method === 'GET' && (pathname === '/' || pathname === '/health')) {
    return jsonResponse({
      ok: true,
      service: 'Teb Al-Hajayzah FCM & Reminders Worker',
      projectId: getEnvValue(env, 'FIREBASE_PROJECT_ID') || DEFAULT_PROJECT_ID,
      oauthScope: OAUTH_SCOPES
    }, 200);
  }

  if (request.method === 'POST') {
    try {
      const body = await request.json();
      const result = await sendFcmPushNotification(body, env);
      const status = result.tokensCount === 0 ? 400 : (result.sentCount > 0 ? 200 : 502);
      return jsonResponse(result, status);
    } catch (err) {
      return jsonResponse({
        success: false,
        error: err && err.message ? err.message : String(err)
      }, 500);
    }
  }

  return jsonResponse({ ok: false, error: 'Method or route not supported' }, 404);
}

function collectTargetTokens(payload) {
  const tokenSet = new Set();
  if (payload && Array.isArray(payload.tokens)) {
    for (const item of payload.tokens) {
      const clean = String(item || '').trim();
      if (clean) tokenSet.add(clean);
    }
  } else if (payload) {
    const single = String(payload.token || payload.fcmToken || '').trim();
    if (single) tokenSet.add(single);
  }
  return Array.from(tokenSet);
}

function stringifyDataMap(rawData, extraFields) {
  const out = {};
  const merged = Object.assign({}, rawData && typeof rawData === 'object' ? rawData : {}, extraFields || {});
  for (const [k, v] of Object.entries(merged)) {
    if (v !== undefined && v !== null) {
      out[String(k)] = typeof v === 'string' ? v : JSON.stringify(v);
    }
  }
  return out;
}

async function sendSingleFcmMessage(projectId, accessToken, deviceToken, title, body, dataMap) {
  const fcmUrl = `https://fcm.googleapis.com/v1/projects/${encodeURIComponent(projectId)}/messages:send`;
  const fcmPayload = {
    message: {
      token: deviceToken,
      notification: {
        title,
        body
      },
      data: dataMap,
      android: {
        priority: 'HIGH',
        ttl: '86400s',
        notification: {
          channel_id: ANDROID_CHANNEL_ID,
          sound: 'default',
          default_vibrate_timings: true,
          notification_priority: 'PRIORITY_MAX',
          visibility: 'PUBLIC'
        }
      },
      webpush: {
        headers: {
          Urgency: 'high'
        },
        notification: {
          title,
          body,
          icon: '/icon.svg',
          badge: '/icon.svg'
        }
      },
      apns: {
        headers: {
          'apns-priority': '10'
        },
        payload: {
          aps: {
            sound: 'default',
            'content-available': 1
          }
        }
      }
    }
  };

  const resp = await fetch(fcmUrl, {
    method: 'POST',
    headers: {
      Authorization: 'Bearer ' + accessToken,
      'Content-Type': 'application/json; charset=UTF-8'
    },
    body: JSON.stringify(fcmPayload)
  });

  const text = await resp.text();
  let parsed = null;
  try { parsed = JSON.parse(text); } catch (e) {}

  return {
    ok: resp.ok,
    status: resp.status,
    tokenPreview: deviceToken.slice(0, 15) + '...',
    messageName: parsed && parsed.name ? parsed.name : '',
    error: !resp.ok ? ((parsed && parsed.error && parsed.error.message) || text.slice(0, 180)) : ''
  };
}

async function sendFcmPushNotification(payload, env) {
  const tokens = collectTargetTokens(payload);
  if (tokens.length === 0) {
    return {
      success: false,
      tokensCount: 0,
      sentCount: 0,
      failedCount: 0,
      error: 'No target FCM tokens in payload.tokens'
    };
  }

  const config = readWorkerConfig(env, payload && payload.projectId);
  const accessToken = await getGoogleAccessToken(config);

  const cleanTitle = String((payload && payload.title) || 'طب الحجايزة');
  const cleanBody = String((payload && (payload.message || payload.body)) || '');
  const dataMap = stringifyDataMap(payload && payload.data, {
    title: cleanTitle,
    body: cleanBody,
    eventType: String((payload && payload.eventType) || 'PLATFORM_ALERT'),
    targetAudience: String((payload && payload.targetAudience) || 'ALL'),
    targetId: String((payload && payload.targetId) || ''),
    collectionPath: String((payload && payload.collectionPath) || ''),
    namespace: String((payload && payload.namespace) || 'teb_alhajayzah')
  });

  const settled = await Promise.allSettled(
    tokens.map((tok) => sendSingleFcmMessage(config.projectId, accessToken, tok, cleanTitle, cleanBody, dataMap))
  );

  const results = settled.map((item, idx) => {
    if (item.status === 'fulfilled') return item.value;
    return {
      ok: false,
      status: 0,
      tokenPreview: tokens[idx].slice(0, 15) + '...',
      error: item.reason && item.reason.message ? item.reason.message : 'Fetch error'
    };
  });

  const sentCount = results.filter((r) => r.ok).length;
  const failedCount = results.length - sentCount;

  return {
    success: sentCount > 0,
    tokensCount: tokens.length,
    sentCount,
    failedCount,
    results
  };
}

function decodeFirestoreValue(wrapper) {
  if (!wrapper || typeof wrapper !== 'object') return undefined;
  if (Object.prototype.hasOwnProperty.call(wrapper, 'stringValue')) return String(wrapper.stringValue || '');
  if (Object.prototype.hasOwnProperty.call(wrapper, 'booleanValue')) return Boolean(wrapper.booleanValue);
  if (Object.prototype.hasOwnProperty.call(wrapper, 'integerValue')) return Number(wrapper.integerValue) || 0;
  if (Object.prototype.hasOwnProperty.call(wrapper, 'doubleValue')) return Number(wrapper.doubleValue) || 0;
  if (Object.prototype.hasOwnProperty.call(wrapper, 'nullValue')) return null;
  if (wrapper.arrayValue && Array.isArray(wrapper.arrayValue.values)) {
    return wrapper.arrayValue.values.map((v) => decodeFirestoreValue(v)).filter((v) => v !== undefined);
  }
  if (wrapper.arrayValue) return [];
  if (wrapper.mapValue && wrapper.mapValue.fields) {
    return decodeFirestoreDocFields(wrapper.mapValue.fields);
  }
  return undefined;
}

function decodeFirestoreDocFields(fields) {
  const out = {};
  if (!fields || typeof fields !== 'object') return out;
  for (const [k, v] of Object.entries(fields)) {
    const decoded = decodeFirestoreValue(v);
    if (decoded !== undefined) out[k] = decoded;
  }
  return out;
}

async function firestoreListCollection(config, accessToken, collectionName) {
  const docs = [];
  let pageToken = '';
  let guard = 0;

  do {
    guard++;
    let url = `https://firestore.googleapis.com/v1/projects/${encodeURIComponent(config.projectId)}/databases/${encodeURIComponent(config.databaseId)}/documents/${encodeURIComponent(collectionName)}?pageSize=500`;
    if (pageToken) url += '&pageToken=' + encodeURIComponent(pageToken);

    const resp = await fetch(url, {
      method: 'GET',
      headers: { Authorization: 'Bearer ' + accessToken }
    });
    if (resp.status === 404) return [];
    if (!resp.ok) {
      const txt = await resp.text();
      throw new Error(`Firestore list(${collectionName}) HTTP ${resp.status}: ${txt.slice(0, 160)}`);
    }

    const json = await resp.json();
    const rawDocs = Array.isArray(json.documents) ? json.documents : [];
    for (const d of rawDocs) {
      const obj = decodeFirestoreDocFields(d.fields);
      const docName = String(d.name || '');
      const docId = docName ? docName.split('/').pop() : '';
      if (docId) obj._docId = docId;
      docs.push(obj);
    }
    pageToken = json.nextPageToken ? String(json.nextPageToken) : '';
  } while (pageToken && guard < 10);

  return docs;
}

async function firestorePatchBooleanFlag(config, accessToken, collectionName, docId, fieldName, fieldVal) {
  const baseUrl = `https://firestore.googleapis.com/v1/projects/${encodeURIComponent(config.projectId)}/databases/${encodeURIComponent(config.databaseId)}/documents/${encodeURIComponent(collectionName)}/${encodeURIComponent(docId)}`;
  const url = `${baseUrl}?updateMask.fieldPaths=${encodeURIComponent(fieldName)}`;
  const resp = await fetch(url, {
    method: 'PATCH',
    headers: {
      Authorization: 'Bearer ' + accessToken,
      'Content-Type': 'application/json; charset=UTF-8'
    },
    body: JSON.stringify({
      fields: {
        [fieldName]: { booleanValue: Boolean(fieldVal) }
      }
    })
  });
  return resp.ok;
}

function normalizePhoneDigits(phone) {
  const ascii = String(phone || '')
    .replace(/[\u0660-\u0669]/g, (c) => String(c.charCodeAt(0) - 0x0660))
    .replace(/[\u06F0-\u06F9]/g, (c) => String(c.charCodeAt(0) - 0x06F0));
  const digits = ascii.replace(/\D/g, '');
  if (digits.startsWith('0020') && digits.length >= 14) return '0' + digits.slice(4);
  if (digits.startsWith('20') && digits.length >= 12) return '0' + digits.slice(2);
  return digits;
}

function getCairoOffsetMinutesForDate(dateStr) {
  try {
    const [y, m, d] = dateStr.split('-').map(Number);
    const utcProbe = new Date(Date.UTC(y, m - 1, d, 12, 0, 0));
    const parts = new Intl.DateTimeFormat('en-US', {
      timeZone: 'Africa/Cairo',
      timeZoneName: 'shortOffset'
    }).formatToParts(utcProbe);
    const tzPart = parts.find((p) => p.type === 'timeZoneName');
    const match = tzPart && tzPart.value ? tzPart.value.match(/GMT([+-])(\d{1,2})(?::(\d{2}))?/i) : null;
    if (match) {
      const sign = match[1] === '-' ? -1 : 1;
      const hrs = Number(match[2]) || 0;
      const mins = Number(match[3]) || 0;
      return sign * (hrs * 60 + mins);
    }
  } catch (e) {}
  return 120; // Cairo UTC+2 fallback
}

function resolveAppointmentTimestampMs(slot) {
  const direct = Number(
    slot.appointmentTimestampMillis ||
    slot.scheduledAtMillis ||
    slot.visitAtMillis ||
    slot.startTimestampMillis ||
    0
  );
  if (direct > 0) return direct;

  const dateStr = String(slot.date || slot.dayKey || slot.visitDate || '').trim();
  const mins = Number(slot.effectiveMinutes ?? slot.startMinutes ?? -1);
  if (/^\d{4}-\d{2}-\d{2}$/.test(dateStr) && mins >= 0) {
    const [y, m, d] = dateStr.split('-').map(Number);
    const offsetMin = getCairoOffsetMinutesForDate(dateStr);
    const utcMidnightMs = Date.UTC(y, m - 1, d, 0, 0, 0);
    return utcMidnightMs + (mins - offsetMin) * 60000;
  }

  const dayOffset = Number(slot.dayOffset);
  if (!Number.isNaN(dayOffset) && mins >= 0 && slot.dateMillis) {
    return Number(slot.dateMillis) + mins * 60000;
  }
  return 0;
}

function resolvePatientTokensForSlot(slot, tokenDocs) {
  const targetAccId = String(slot.bookedByPatientAccountId || slot.patientAccountId || '').trim();
  const targetPhone = normalizePhoneDigits(slot.bookedByPhone || slot.patientPhone || '');
  const resolved = new Set();

  for (const row of tokenDocs) {
    if (!row) continue;
    const role = String(row.role || '').toUpperCase();
    if (role === 'ADMIN' || role === 'DOCTOR' || role === 'CLINIC') continue;

    const uid = String(row.userId || row._docId || row.id || '').trim();
    const accId = String(row.accountId || '').trim();
    const phoneDigits = normalizePhoneDigits(row.phone || uid);

    const matched =
      (targetAccId && (accId === targetAccId || uid === targetAccId)) ||
      (targetPhone && (phoneDigits === targetPhone || uid === targetPhone));

    if (!matched) continue;

    const singleTok = String(row.fcmToken || row.token || '').trim();
    if (singleTok) resolved.add(singleTok);
    if (Array.isArray(row.tokens)) {
      for (const t of row.tokens) {
        const clean = String(t || '').trim();
        if (clean) resolved.add(clean);
      }
    }
  }

  return Array.from(resolved);
}

/**
 * Checks upcoming booked appointments in `teb_alhajayzah_appointments`:
 * - Sends 2-hour reminder (`reminded2h`) when 30 < minsLeft <= 120
 * - Sends 30-minute urgent reminder (`reminded30m`) when 0 < minsLeft <= 30
 */
async function checkAndDispatchAppointmentReminders(env) {
  const config = readWorkerConfig(env);
  const accessToken = await getGoogleAccessToken(config);

  const [appointments, tokenDocs] = await Promise.all([
    firestoreListCollection(config, accessToken, APPOINTMENTS_COLLECTION),
    firestoreListCollection(config, accessToken, FCM_TOKENS_COLLECTION)
  ]);

  const nowMs = Date.now();
  let checkedAppointments = 0;
  let sent2h = 0;
  let sent30m = 0;
  const details = [];

  for (const slot of appointments) {
    if (!slot || !slot._docId) continue;
    const status = String(slot.status || '').toUpperCase();
    const isActiveBooking = Boolean(slot.isBooked) || status === 'BOOKED' || status === 'CONFIRMED';
    if (!isActiveBooking || status === 'CANCELLED' || status === 'COMPLETED') continue;

    checkedAppointments++;
    const apptMs = resolveAppointmentTimestampMs(slot);
    if (!apptMs || apptMs <= nowMs) continue;

    const diffMins = (apptMs - nowMs) / 60000;
    const doctorLabel = String(slot.doctorName || slot.clinicName || 'العيادة');
    const timeLabel = String(slot.displayTime || slot.timeLabel || '');

    if (diffMins <= 30 && !slot.reminded30m) {
      const tokens = resolvePatientTokensForSlot(slot, tokenDocs);
      if (tokens.length > 0) {
        const title = '🚨 تذكير عاجل: موعدك خلال نصف ساعة';
        const body = `اقترب موعد الكشف لدى ${doctorLabel}${timeLabel ? ' الساعة ' + timeLabel : ''}. يرجى التوجه للعيادة الآن.`;
        const dataMap = stringifyDataMap(null, {
          eventType: 'APPOINTMENT_REMINDER_30M',
          slotId: String(slot.id || slot._docId),
          title,
          body
        });
        const settled = await Promise.allSettled(
          tokens.map((t) => sendSingleFcmMessage(config.projectId, accessToken, t, title, body, dataMap))
        );
        const anySent = settled.some((r) => r.status === 'fulfilled' && r.value && r.value.ok);
        if (anySent) {
          await firestorePatchBooleanFlag(config, accessToken, APPOINTMENTS_COLLECTION, slot._docId, 'reminded30m', true);
          sent30m++;
          details.push({ slotId: slot._docId, type: '30m', tokensCount: tokens.length });
        }
      }
    } else if (diffMins <= 120 && diffMins > 30 && !slot.reminded2h) {
      const tokens = resolvePatientTokensForSlot(slot, tokenDocs);
      if (tokens.length > 0) {
        const title = '⏰ تذكير بموعد الكشف (بعد ساعتين)';
        const body = `لديك موعد كشف قادم لدى ${doctorLabel}${timeLabel ? ' الساعة ' + timeLabel : ''} في منصة طب الحجايزة.`;
        const dataMap = stringifyDataMap(null, {
          eventType: 'APPOINTMENT_REMINDER_2H',
          slotId: String(slot.id || slot._docId),
          title,
          body
        });
        const settled = await Promise.allSettled(
          tokens.map((t) => sendSingleFcmMessage(config.projectId, accessToken, t, title, body, dataMap))
        );
        const anySent = settled.some((r) => r.status === 'fulfilled' && r.value && r.value.ok);
        if (anySent) {
          await firestorePatchBooleanFlag(config, accessToken, APPOINTMENTS_COLLECTION, slot._docId, 'reminded2h', true);
          sent2h++;
          details.push({ slotId: slot._docId, type: '2h', tokensCount: tokens.length });
        }
      }
    }
  }

  return {
    success: true,
    checkedAppointments,
    sent2h,
    sent30m,
    details,
    checkedAtIso: new Date(nowMs).toISOString()
  };
}
