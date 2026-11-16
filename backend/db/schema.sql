-- NOVA VPN Global Infrastructure Database Schema (PostgreSQL 16+)
-- Zero browsing history or traffic payload tables by design.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS regions (
  code VARCHAR(32) PRIMARY KEY,
  name_en VARCHAR(100) NOT NULL,
  name_ar VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS countries (
  iso_code CHAR(2) PRIMARY KEY,
  name_en VARCHAR(120) NOT NULL,
  name_ar VARCHAR(120) NOT NULL,
  region_code VARCHAR(32) NOT NULL REFERENCES regions(code),
  is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cities (
  id VARCHAR(64) PRIMARY KEY,
  country_iso CHAR(2) NOT NULL REFERENCES countries(iso_code) ON DELETE CASCADE,
  name_en VARCHAR(120) NOT NULL,
  name_ar VARCHAR(120) NOT NULL,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS vpn_servers (
  id VARCHAR(64) PRIMARY KEY,
  country_iso CHAR(2) NOT NULL REFERENCES countries(iso_code) ON DELETE CASCADE,
  city_id VARCHAR(64) NOT NULL REFERENCES cities(id) ON DELETE CASCADE,
  hostname VARCHAR(255) NOT NULL UNIQUE,
  endpoint_host VARCHAR(255) NOT NULL,
  endpoint_port INTEGER NOT NULL CHECK (endpoint_port BETWEEN 1 AND 65535),
  public_key VARCHAR(64) NOT NULL,
  wg_agent_url VARCHAR(255),
  tunnel_subnet_cidr VARCHAR(32) NOT NULL DEFAULT '10.66.0.0/16',
  supported_protocols TEXT[] NOT NULL DEFAULT ARRAY['WIREGUARD'],
  status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE'
    CHECK (status IN ('ONLINE', 'BUSY', 'MAINTENANCE', 'OFFLINE')),
  max_sessions INTEGER NOT NULL DEFAULT 500,
  bandwidth_capacity_mbps INTEGER NOT NULL DEFAULT 10000,
  is_available BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS server_health_metrics (
  server_id VARCHAR(64) PRIMARY KEY REFERENCES vpn_servers(id) ON DELETE CASCADE,
  load_percent INTEGER NOT NULL DEFAULT 0 CHECK (load_percent BETWEEN 0 AND 100),
  available_capacity_percent INTEGER NOT NULL DEFAULT 100 CHECK (available_capacity_percent BETWEEN 0 AND 100),
  active_sessions INTEGER NOT NULL DEFAULT 0,
  latency_ms INTEGER,
  jitter_ms INTEGER,
  packet_loss_percent DOUBLE PRECISION NOT NULL DEFAULT 0.0,
  checked_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  tier VARCHAR(32) NOT NULL DEFAULT 'standard',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS vpn_sessions (
  session_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  server_id VARCHAR(64) NOT NULL REFERENCES vpn_servers(id) ON DELETE CASCADE,
  client_public_key VARCHAR(64) NOT NULL,
  assigned_ipv4 VARCHAR(32) NOT NULL,
  assigned_ipv6 VARCHAR(64),
  protocol VARCHAR(24) NOT NULL DEFAULT 'WIREGUARD',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  expires_at TIMESTAMPTZ NOT NULL,
  revoked_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_vpn_sessions_server_active
  ON vpn_sessions(server_id, status) WHERE status = 'ACTIVE';

CREATE TABLE IF NOT EXISTS connection_diagnostics (
  id BIGSERIAL PRIMARY KEY,
  user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  server_id VARCHAR(64) REFERENCES vpn_servers(id) ON DELETE SET NULL,
  event VARCHAR(64) NOT NULL,
  measured_latency_ms INTEGER,
  error_code VARCHAR(64),
  app_version VARCHAR(32) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Seed standard geographical regions
INSERT INTO regions (code, name_en, name_ar) VALUES
  ('MENA', 'Middle East & North Africa', 'الشرق الأوسط وشمال أفريقيا'),
  ('EUROPE', 'Europe', 'أوروبا'),
  ('NORTH_AMERICA', 'North America', 'أمريكا الشمالية'),
  ('SOUTH_AMERICA', 'South America', 'أمريكا الجنوبية'),
  ('ASIA_PACIFIC', 'Asia-Pacific', 'آسيا والمحيط الهادئ'),
  ('SUB_SAHARAN_AFRICA', 'Sub-Saharan Africa', 'أفريقيا جنوب الصحراء'),
  ('CENTRAL_ASIA', 'Central Asia & Caucasus', 'آسيا الوسطى والقوقاز'),
  ('CARIBBEAN_OCEANIA', 'Caribbean & Oceania', 'الكاريبي وأوقيانوسيا')
ON CONFLICT (code) DO NOTHING;
