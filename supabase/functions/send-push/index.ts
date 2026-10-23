import { type SendOutcome, sendData, type ServiceAccount } from "./fcm.ts";

// docs/decisions.md, 2026-10-16: only these keys ever reach a phone, whatever
// the body carries (RF-09.10).
const SHARED_KEYS = ["event", "requestId", "recipientRole", "serviceTypeName", "scheduledAt"];
const TARGET_KEYS = ["recipientId", "distanceM"];

const INSTALLATION_ID = /^[A-Za-z0-9_:-]{8,256}$/;
const BATCH_SIZE = 10;
const MAX_TTL_SECONDS = 2_419_200;

interface Target {
  fid: string;
  [key: string]: unknown;
}

interface PushBody {
  ttlSeconds?: number;
  targets?: Target[];
  [key: string]: unknown;
}

async function sha256(text: string): Promise<Uint8Array> {
  return new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text)));
}

// Hashing both sides first makes the comparison constant-time even when the
// lengths differ.
async function hasSharedSecret(request: Request, expected: string): Promise<boolean> {
  const given = request.headers.get("x-webhook-secret");
  if (!given) return false;
  const [a, b] = await Promise.all([sha256(expected), sha256(given)]);
  let difference = 0;
  for (let i = 0; i < a.length; i++) difference |= a[i] ^ b[i];
  return difference === 0;
}

function pick(source: Record<string, unknown>, keys: string[]): Record<string, string> {
  const picked: Record<string, string> = {};
  for (const key of keys) {
    const value = source[key];
    if (value !== undefined && value !== null) picked[key] = String(value);
  }
  return picked;
}

async function prune(fids: string[], startedAt: string): Promise<number> {
  if (fids.length === 0) return 0;
  const list = fids.map((fid) => `"${fid}"`).join(",");
  // A row registered again after this delivery started belongs to a fresh
  // registration and stays.
  const query = `token=in.(${encodeURIComponent(list)})&last_used_at=lt.${encodeURIComponent(startedAt)}` +
    "&select=last_used_at";
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  const response = await fetch(`${Deno.env.get("SUPABASE_URL")}/rest/v1/device_tokens?${query}`, {
    method: "DELETE",
    headers: {
      apikey: serviceKey,
      Authorization: `Bearer ${serviceKey}`,
      Prefer: "return=representation",
    },
  });
  if (!response.ok) {
    await response.body?.cancel();
    return 0;
  }
  const deleted: unknown[] = await response.json();
  return deleted.length;
}

Deno.serve(async (request) => {
  const secret = Deno.env.get("PUSH_WEBHOOK_SECRET");
  const serviceAccountJson = Deno.env.get("FCM_SERVICE_ACCOUNT");
  if (!secret || !serviceAccountJson) return new Response(null, { status: 500 });
  if (request.method !== "POST") return new Response(null, { status: 405 });
  if (!(await hasSharedSecret(request, secret))) return new Response(null, { status: 401 });

  let body: PushBody;
  try {
    body = await request.json();
  } catch {
    return new Response(null, { status: 400 });
  }
  if (!Array.isArray(body.targets)) return new Response(null, { status: 400 });

  const account: ServiceAccount = JSON.parse(serviceAccountJson);
  const startedAt = new Date().toISOString();
  const shared = pick(body, SHARED_KEYS);
  const ttlSeconds = Math.min(Math.max(Math.trunc(Number(body.ttlSeconds) || 0), 0), MAX_TTL_SECONDS);
  const targets = body.targets.filter((target) =>
    typeof target?.fid === "string" && INSTALLATION_ID.test(target.fid)
  );

  const outcomes: { fid: string; outcome: SendOutcome }[] = [];
  for (let i = 0; i < targets.length; i += BATCH_SIZE) {
    const batch = targets.slice(i, i + BATCH_SIZE);
    const settled = await Promise.allSettled(
      batch.map((target) =>
        sendData(account, target.fid, { ...shared, ...pick(target, TARGET_KEYS) }, ttlSeconds)
      ),
    );
    settled.forEach((result, index) => {
      if (result.status === "rejected") console.warn(`send failed: ${result.reason}`);
      outcomes.push({
        fid: batch[index].fid,
        outcome: result.status === "fulfilled" ? result.value : "failed",
      });
    });
  }

  const unregistered = outcomes.filter((o) => o.outcome === "unregistered").map((o) => o.fid);
  const summary = {
    sent: outcomes.filter((o) => o.outcome === "sent").length,
    pruned: await prune(unregistered, startedAt),
    failed: outcomes.filter((o) => o.outcome === "failed").length +
      body.targets.length - targets.length,
  };
  console.log(`send-push ${shared.event ?? "?"}: ${JSON.stringify(summary)}`);
  return Response.json(summary);
});
