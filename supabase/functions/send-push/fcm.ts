export interface ServiceAccount {
  client_email: string;
  private_key: string;
  project_id: string;
  token_uri: string;
}

export type SendOutcome = "sent" | "unregistered" | "failed";

const SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
const RETRY_DELAY_MS = 500;

let cachedToken: { value: string; expiresAt: number } | null = null;

function base64Url(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function encodeJson(value: unknown): string {
  return base64Url(new TextEncoder().encode(JSON.stringify(value)));
}

async function importPrivateKey(pem: string): Promise<CryptoKey> {
  const body = pem.replace(/-----(BEGIN|END) PRIVATE KEY-----/g, "").replace(/\s+/g, "");
  const der = Uint8Array.from(atob(body), (c) => c.charCodeAt(0));
  return await crypto.subtle.importKey(
    "pkcs8",
    der,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
}

// Google's OAuth 2.0 flow for service accounts: a self-signed RS256 JWT is
// exchanged at token_uri for an access token that lives one hour.
export async function signAssertion(account: ServiceAccount, nowSeconds: number): Promise<string> {
  const unsigned = `${encodeJson({ alg: "RS256", typ: "JWT" })}.${
    encodeJson({
      iss: account.client_email,
      scope: SCOPE,
      aud: account.token_uri,
      iat: nowSeconds,
      exp: nowSeconds + 3600,
    })
  }`;
  const key = await importPrivateKey(account.private_key);
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(unsigned),
  );
  return `${unsigned}.${base64Url(new Uint8Array(signature))}`;
}

async function accessToken(account: ServiceAccount): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  if (cachedToken && cachedToken.expiresAt - 60 > now) return cachedToken.value;

  const response = await fetch(account.token_uri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: await signAssertion(account, now),
    }),
  });
  if (!response.ok) throw new Error(`token endpoint answered ${response.status}`);

  const json = await response.json();
  cachedToken = { value: json.access_token, expiresAt: now + json.expires_in };
  return cachedToken.value;
}

// FCM HTTP v1 addresses an app install by its installation id in the fid
// field; the token field is deprecated for clients registered by FID.
export async function sendData(
  account: ServiceAccount,
  fid: string,
  data: Record<string, string>,
  ttlSeconds: number,
): Promise<SendOutcome> {
  for (let attempt = 0; attempt < 2; attempt++) {
    const response = await fetch(
      `https://fcm.googleapis.com/v1/projects/${account.project_id}/messages:send`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${await accessToken(account)}`,
        },
        body: JSON.stringify({
          message: {
            fid,
            data,
            android: { priority: "HIGH", ttl: `${ttlSeconds}s` },
          },
        }),
      },
    );
    if (response.ok) {
      await response.body?.cancel();
      return "sent";
    }
    // docs/decisions.md, 2026-10-16, "el teléfono se identifica por su instalación", 9.
    const errorCodes = await response.json().then(
      (body) => (body?.error?.details ?? []).map((detail: { errorCode?: string }) => detail.errorCode),
      () => [],
    );
    if (errorCodes.includes("UNREGISTERED")) return "unregistered";
    if (response.status === 401) {
      cachedToken = null;
      continue;
    }
    if (response.status === 429 || response.status >= 500) {
      await new Promise((resolve) => setTimeout(resolve, RETRY_DELAY_MS));
      continue;
    }
    console.warn(`FCM answered ${response.status} ${errorCodes.join(",")}`);
    return "failed";
  }
  return "failed";
}
