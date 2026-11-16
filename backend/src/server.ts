import 'dotenv/config';
import crypto from 'node:crypto';
import cors from 'cors';
import express, { NextFunction, Request, Response } from 'express';
import rateLimit from 'express-rate-limit';
import helmet from 'helmet';
import jwt from 'jsonwebtoken';
import { Pool } from 'pg';
import { z } from 'zod';

const PORT = Number(process.env.PORT ?? 8443);
const DATABASE_URL = process.env.DATABASE_URL;
const JWT_SECRET = process.env.JWT_SECRET;
const ADMIN_API_KEY = process.env.ADMIN_API_KEY;
const WG_AGENT_SECRET = process.env.WG_AGENT_SECRET ?? '';

if (!DATABASE_URL || !JWT_SECRET || !ADMIN_API_KEY) {
  console.error('FATAL: DATABASE_URL, JWT_SECRET, and ADMIN_API_KEY must be configured.');
  process.exit(1);
}

const pool = new Pool({ connectionString: DATABASE_URL });
const app = express();

app.use(helmet());
app.use(
  cors({
    origin: (process.env.CORS_ORIGINS ?? '').split(',').filter(Boolean),
    methods: ['GET', 'POST', 'PATCH', 'DELETE'],
  }),
);
app.use(express.json({ limit: '256kb' }));

const apiLimiter = rateLimit({
  windowMs: Number(process.env.RATE_LIMIT_WINDOW_MS ?? 60_000),
  max: Number(process.env.RATE_LIMIT_MAX_REQUESTS ?? 120),
  standardHeaders: true,
  legacyHeaders: false,
});
app.use('/api/v1', apiLimiter);

interface AuthenticatedRequest extends Request {
  user?: { id: string; email: string; tier: string };
}

function hashPassword(password: string): string {
  const salt = crypto.randomBytes(16).toString('hex');
  const derived = crypto.scryptSync(password, salt, 64).toString('hex');
  return `${salt}:${derived}`;
}

function verifyPassword(password: string, stored: string): boolean {
  const [salt, expected] = stored.split(':');
  if (!salt || !expected) return false;
  const derived = crypto.scryptSync(password, salt, 64);
  const expectedBuf = Buffer.from(expected, 'hex');
  if (derived.length !== expectedBuf.length) return false;
  return crypto.timingSafeEqual(derived, expectedBuf);
}

function requireAuth(req: AuthenticatedRequest, res: Response, next: NextFunction) {
  const authHeader = req.headers.authorization;
  if (!authHeader?.startsWith('Bearer ')) {
    res.status(401).json({ error: 'Missing bearer token' });
    return;
  }
  const token = authHeader.slice(7);
  try {
    const payload = jwt.verify(token, JWT_SECRET!) as { sub: string; email: string; tier: string };
    req.user = { id: payload.sub, email: payload.email, tier: payload.tier };
    next();
  } catch {
    res.status(401).json({ error: 'Invalid or expired token' });
  }
}

function requireAdmin(req: Request, res: Response, next: NextFunction) {
  const key = req.headers['x-admin-api-key'];
  if (typeof key !== 'string' || !crypto.timingSafeEqual(Buffer.from(key), Buffer.from(ADMIN_API_KEY!))) {
    res.status(403).json({ error: 'Forbidden' });
    return;
  }
  next();
}

const AuthSchema = z.object({
  email: z.string().email().max(255),
  password: z.string().min(8).max(128),
});

app.post('/api/v1/auth/register', async (req: Request, res: Response) => {
  const parsed = AuthSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: 'Invalid credentials payload' });
    return;
  }
  const { email, password } = parsed.data;
  const passwordHash = hashPassword(password);
  try {
    const result = await pool.query(
      'INSERT INTO users (email, password_hash) VALUES ($1, $2) RETURNING id, email, tier',
      [email.toLowerCase(), passwordHash],
    );
    const user = result.rows[0];
    const expiresAt = new Date(Date.now() + 7 * 24 * 3600 * 1000).toISOString();
    const token = jwt.sign({ sub: user.id, email: user.email, tier: user.tier }, JWT_SECRET!, {
      expiresIn: '7d',
    });
    res.status(201).json({ token, expires_at: expiresAt, user });
  } catch {
    res.status(409).json({ error: 'Account already exists' });
  }
});

app.post('/api/v1/auth/login', async (req: Request, res: Response) => {
  const parsed = AuthSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: 'Invalid credentials payload' });
    return;
  }
  const { email, password } = parsed.data;
  const result = await pool.query('SELECT id, email, tier, password_hash FROM users WHERE email = $1', [
    email.toLowerCase(),
  ]);
  const row = result.rows[0];
  if (!row || !verifyPassword(password, row.password_hash)) {
    res.status(401).json({ error: 'Invalid email or password' });
    return;
  }
  const expiresAt = new Date(Date.now() + 7 * 24 * 3600 * 1000).toISOString();
  const token = jwt.sign({ sub: row.id, email: row.email, tier: row.tier }, JWT_SECRET!, {
    expiresIn: '7d',
  });
  res.json({
    token,
    expires_at: expiresAt,
    user: { id: row.id, email: row.email, tier: row.tier },
  });
});

app.get('/api/v1/locations', async (_req: Request, res: Response) => {
  const regionsRes = await pool.query('SELECT code, name_en, name_ar FROM regions ORDER BY code');
  const countriesRes = await pool.query(
    'SELECT iso_code, name_en, name_ar, region_code FROM countries WHERE is_enabled = TRUE ORDER BY name_en',
  );
  const citiesRes = await pool.query('SELECT id, country_iso, name_en, name_ar FROM cities ORDER BY name_en');
  const serversRes = await pool.query(`
    SELECT s.*, h.load_percent, h.available_capacity_percent, h.latency_ms, h.jitter_ms, h.checked_at,
           c.name_en AS city_name_en, c.name_ar AS city_name_ar
    FROM vpn_servers s
    JOIN cities c ON c.id = s.city_id
    LEFT JOIN server_health_metrics h ON h.server_id = s.id
    ORDER BY s.country_iso, s.city_id, s.hostname
  `);

  const serversByCity = new Map<string, unknown[]>();
  for (const s of serversRes.rows) {
    const arr = serversByCity.get(s.city_id) ?? [];
    arr.push({
      id: s.id,
      country_iso: s.country_iso,
      city_id: s.city_id,
      city_name_en: s.city_name_en,
      city_name_ar: s.city_name_ar,
      hostname: s.hostname,
      endpoint_host: s.endpoint_host,
      endpoint_port: s.endpoint_port,
      public_key: s.public_key,
      supported_protocols: s.supported_protocols,
      status: s.status,
      load_percent: s.load_percent,
      available_capacity_percent: s.available_capacity_percent,
      latency_ms: s.latency_ms,
      jitter_ms: s.jitter_ms,
      bandwidth_capacity_mbps: s.bandwidth_capacity_mbps,
      is_available: s.is_available,
      last_health_check: s.checked_at,
    });
    serversByCity.set(s.city_id, arr);
  }

  const citiesByCountry = new Map<string, unknown[]>();
  for (const city of citiesRes.rows) {
    const arr = citiesByCountry.get(city.country_iso) ?? [];
    arr.push({
      id: city.id,
      country_iso: city.country_iso,
      name_en: city.name_en,
      name_ar: city.name_ar,
      servers: serversByCity.get(city.id) ?? [],
    });
    citiesByCountry.set(city.country_iso, arr);
  }

  res.json({
    regions: regionsRes.rows,
    countries: countriesRes.rows.map((c) => ({
      iso_code: c.iso_code,
      name_en: c.name_en,
      name_ar: c.name_ar,
      region: c.region_code,
      cities: citiesByCountry.get(c.iso_code) ?? [],
    })),
    updated_at: new Date().toISOString(),
  });
});

app.get('/api/v1/servers', async (req: Request, res: Response) => {
  const { country_iso, status } = req.query;
  const conditions: string[] = [];
  const params: unknown[] = [];

  if (typeof country_iso === 'string' && country_iso.length === 2) {
    params.push(country_iso.toUpperCase());
    conditions.push(`s.country_iso = $${params.length}`);
  }
  if (typeof status === 'string') {
    params.push(status.toUpperCase());
    conditions.push(`s.status = $${params.length}`);
  }

  const where = conditions.length > 0 ? `WHERE ${conditions.join(' AND ')}` : '';
  const query = `
    SELECT s.*, h.load_percent, h.available_capacity_percent, h.latency_ms, h.jitter_ms, h.checked_at,
           c.name_en AS city_name_en, c.name_ar AS city_name_ar
    FROM vpn_servers s
    JOIN cities c ON c.id = s.city_id
    LEFT JOIN server_health_metrics h ON h.server_id = s.id
    ${where}
    ORDER BY h.load_percent ASC NULLS LAST
  `;
  const result = await pool.query(query, params);
  res.json({
    servers: result.rows.map((s) => ({
      id: s.id,
      country_iso: s.country_iso,
      city_id: s.city_id,
      city_name_en: s.city_name_en,
      city_name_ar: s.city_name_ar,
      hostname: s.hostname,
      endpoint_host: s.endpoint_host,
      endpoint_port: s.endpoint_port,
      public_key: s.public_key,
      supported_protocols: s.supported_protocols,
      status: s.status,
      load_percent: s.load_percent,
      available_capacity_percent: s.available_capacity_percent,
      latency_ms: s.latency_ms,
      jitter_ms: s.jitter_ms,
      bandwidth_capacity_mbps: s.bandwidth_capacity_mbps,
      is_available: s.is_available,
      last_health_check: s.checked_at,
    })),
  });
});

app.get('/api/v1/servers/:server_id/health', async (req: Request, res: Response) => {
  const result = await pool.query(
    `SELECT s.id, s.status, h.load_percent, h.available_capacity_percent, h.active_sessions, h.packet_loss_percent, h.checked_at
     FROM vpn_servers s
     LEFT JOIN server_health_metrics h ON h.server_id = s.id
     WHERE s.id = $1`,
    [req.params.server_id],
  );
  const row = result.rows[0];
  if (!row) {
    res.status(404).json({ error: 'Server not found' });
    return;
  }
  res.json({
    server_id: row.id,
    status: row.status,
    load_percent: row.load_percent ?? 0,
    available_capacity_percent: row.available_capacity_percent ?? 100,
    active_sessions: row.active_sessions ?? 0,
    packet_loss_percent: row.packet_loss_percent ?? 0,
    checked_at: row.checked_at ?? new Date().toISOString(),
  });
});

const CreateSessionSchema = z.object({
  server_id: z.string().min(1).max(64),
  client_public_key: z.string().regex(/^[A-Za-z0-9+/]{42,44}=?$/),
  protocol: z.enum(['WIREGUARD', 'OPENVPN']).default('WIREGUARD'),
  preferred_dns: z.string().ip().optional(),
});

app.post('/api/v1/vpn/sessions', requireAuth, async (req: AuthenticatedRequest, res: Response) => {
  const parsed = CreateSessionSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: 'Invalid session request' });
    return;
  }
  const { server_id, client_public_key, protocol, preferred_dns } = parsed.data;
  const serverRes = await pool.query('SELECT * FROM vpn_servers WHERE id = $1', [server_id]);
  const server = serverRes.rows[0];
  if (!server || !server.is_available || server.status === 'OFFLINE' || server.status === 'MAINTENANCE') {
    res.status(503).json({ error: 'Server not currently available' });
    return;
  }

  // Allocate a deterministic private tunnel IPv4 from 10.66.x.y/32
  const activeCountRes = await pool.query(
    "SELECT COUNT(*)::int AS cnt FROM vpn_sessions WHERE server_id = $1 AND status = 'ACTIVE'",
    [server_id],
  );
  const hostIndex = (activeCountRes.rows[0]?.cnt ?? 0) + 2;
  const octet3 = Math.floor(hostIndex / 250) % 250;
  const octet4 = (hostIndex % 250) + 2;
  const assignedIpv4 = `10.66.${octet3}.${octet4}/32`;
  const expiresAt = new Date(Date.now() + 24 * 3600 * 1000).toISOString();

  // Notify WireGuard node agent to register peer public key (only public key ever transmitted)
  if (server.wg_agent_url && WG_AGENT_SECRET) {
    await fetch(`${server.wg_agent_url}/peers`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-WG-Agent-Token': WG_AGENT_SECRET,
      },
      body: JSON.stringify({
        client_public_key,
        allowed_ip: assignedIpv4,
      }),
    }).catch(() => null);
  }

  const insertRes = await pool.query(
    `INSERT INTO vpn_sessions (user_id, server_id, client_public_key, assigned_ipv4, protocol, expires_at)
     VALUES ($1, $2, $3, $4, $5, $6)
     RETURNING session_id`,
    [req.user!.id, server.id, client_public_key, assignedIpv4, protocol, expiresAt],
  );

  res.status(201).json({
    session_id: insertRes.rows[0].session_id,
    server_id: server.id,
    assigned_ipv4: assignedIpv4,
    assigned_ipv6: null,
    dns_servers: preferred_dns ? [preferred_dns] : ['1.1.1.1', '9.9.9.9'],
    server_public_key: server.public_key,
    preshared_key: null,
    endpoint: `${server.endpoint_host}:${server.endpoint_port}`,
    allowed_ips: ['0.0.0.0/0', '::/0'],
    persistent_keepalive_sec: 25,
    mtu: 1280,
    expires_at: expiresAt,
  });
});

app.delete('/api/v1/vpn/sessions/:session_id', requireAuth, async (req: AuthenticatedRequest, res: Response) => {
  await pool.query(
    `UPDATE vpn_sessions
     SET status = 'REVOKED', revoked_at = NOW()
     WHERE session_id = $1 AND user_id = $2`,
    [req.params.session_id, req.user!.id],
  );
  res.status(204).send();
});

app.post('/api/v1/diagnostics', async (req: Request, res: Response) => {
  const { server_id, event, measured_latency_ms, error_code, app_version } = req.body ?? {};
  if (typeof event !== 'string' || typeof app_version !== 'string') {
    res.status(400).json({ error: 'Invalid diagnostics payload' });
    return;
  }
  await pool.query(
    `INSERT INTO connection_diagnostics (server_id, event, measured_latency_ms, error_code, app_version)
     VALUES ($1, $2, $3, $4, $5)`,
    [server_id ?? null, event.slice(0, 64), measured_latency_ms ?? null, error_code ?? null, app_version.slice(0, 32)],
  );
  res.status(202).json({ accepted: true });
});

// Speedtest Endpoints (zero fake data — streams real bytes for actual client throughput measurement)
app.get('/api/v1/speedtest/ping', (_req: Request, res: Response) => {
  res.setHeader('Cache-Control', 'no-store');
  res.status(200).json({ ok: true, ts: Date.now() });
});

app.get('/api/v1/speedtest/download', (req: Request, res: Response) => {
  const requested = Math.min(Math.max(Number(req.query.bytes ?? 8_388_608), 65_536), 33_554_432);
  res.setHeader('Content-Type', 'application/octet-stream');
  res.setHeader('Content-Length', String(requested));
  res.setHeader('Cache-Control', 'no-store');
  const chunk = Buffer.alloc(65_536, 0x56);
  let sent = 0;
  function writeMore() {
    while (sent < requested) {
      const remaining = requested - sent;
      const slice = remaining >= chunk.length ? chunk : chunk.subarray(0, remaining);
      sent += slice.length;
      if (!res.write(slice)) {
        res.once('drain', writeMore);
        return;
      }
    }
    res.end();
  }
  writeMore();
});

app.post('/api/v1/speedtest/upload', (req: Request, res: Response) => {
  let received = 0;
  req.on('data', (chunk: Buffer) => {
    received += chunk.length;
  });
  req.on('end', () => {
    res.status(200).json({ received_bytes: received });
  });
});

// Admin Server & Location Management Endpoints
const AdminServerSchema = z.object({
  id: z.string().min(2).max(64),
  country_iso: z.string().length(2),
  country_name_en: z.string().min(2).max(120),
  country_name_ar: z.string().min(2).max(120),
  region_code: z.string().min(2).max(32),
  city_id: z.string().min(2).max(64),
  city_name_en: z.string().min(2).max(120),
  city_name_ar: z.string().min(2).max(120),
  hostname: z.string().min(3).max(255),
  endpoint_host: z.string().min(3).max(255),
  endpoint_port: z.number().int().min(1).max(65535),
  public_key: z.string().min(32).max(64),
  status: z.enum(['ONLINE', 'BUSY', 'MAINTENANCE', 'OFFLINE']).default('ONLINE'),
  is_available: z.boolean().default(true),
  max_sessions: z.number().int().min(1).max(50000).default(500),
  bandwidth_capacity_mbps: z.number().int().min(10).max(100000).default(10000),
});

app.post('/api/v1/admin/servers', requireAdmin, async (req: Request, res: Response) => {
  const parsed = AdminServerSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }
  const d = parsed.data;
  await pool.query(
    `INSERT INTO countries (iso_code, name_en, name_ar, region_code)
     VALUES ($1, $2, $3, $4)
     ON CONFLICT (iso_code) DO UPDATE SET name_en = EXCLUDED.name_en, name_ar = EXCLUDED.name_ar`,
    [d.country_iso.toUpperCase(), d.country_name_en, d.country_name_ar, d.region_code],
  );
  await pool.query(
    `INSERT INTO cities (id, country_iso, name_en, name_ar)
     VALUES ($1, $2, $3, $4)
     ON CONFLICT (id) DO UPDATE SET name_en = EXCLUDED.name_en, name_ar = EXCLUDED.name_ar`,
    [d.city_id, d.country_iso.toUpperCase(), d.city_name_en, d.city_name_ar],
  );
  await pool.query(
    `INSERT INTO vpn_servers (id, country_iso, city_id, hostname, endpoint_host, endpoint_port, public_key, status, is_available, max_sessions, bandwidth_capacity_mbps)
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
     ON CONFLICT (id) DO UPDATE SET
       endpoint_host = EXCLUDED.endpoint_host,
       endpoint_port = EXCLUDED.endpoint_port,
       public_key = EXCLUDED.public_key,
       status = EXCLUDED.status,
       is_available = EXCLUDED.is_available,
       updated_at = NOW()`,
    [
      d.id,
      d.country_iso.toUpperCase(),
      d.city_id,
      d.hostname,
      d.endpoint_host,
      d.endpoint_port,
      d.public_key,
      d.status,
      d.is_available,
      d.max_sessions,
      d.bandwidth_capacity_mbps,
    ],
  );
  res.status(201).json({ id: d.id, status: d.status });
});

app.patch('/api/v1/admin/servers/:id', requireAdmin, async (req: Request, res: Response) => {
  const { status, is_available, load_percent, available_capacity_percent, latency_ms } = req.body ?? {};
  await pool.query(
    `UPDATE vpn_servers
     SET status = COALESCE($2, status),
         is_available = COALESCE($3, is_available),
         updated_at = NOW()
     WHERE id = $1`,
    [req.params.id, status ?? null, is_available ?? null],
  );
  if (typeof load_percent === 'number' || typeof latency_ms === 'number') {
    await pool.query(
      `INSERT INTO server_health_metrics (server_id, load_percent, available_capacity_percent, latency_ms, checked_at)
       VALUES ($1, $2, $3, $4, NOW())
       ON CONFLICT (server_id) DO UPDATE SET
         load_percent = EXCLUDED.load_percent,
         available_capacity_percent = EXCLUDED.available_capacity_percent,
         latency_ms = COALESCE(EXCLUDED.latency_ms, server_health_metrics.latency_ms),
         checked_at = NOW()`,
      [req.params.id, load_percent ?? 0, available_capacity_percent ?? 100, latency_ms ?? null],
    );
  }
  res.json({ updated: true });
});

app.listen(PORT, () => {
  console.log(`NOVA VPN Control Plane listening on port ${PORT}`);
});
