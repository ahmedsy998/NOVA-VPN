# NOVA VPN — Backend Control Plane, API Docs & Android Build Guide

## 1. Architecture Overview

- **Android App (`app/`)**: Native Kotlin + Jetpack Compose + Material 3 + Arabic RTL / English LTR + WireGuard `GoBackend` (`com.wireguard.android:tunnel`) + Android Keystore AES-256-GCM encrypted vault + Room + DataStore.
- **Control Plane (`backend/`)**: TypeScript + Express + PostgreSQL + Zod + JWT + Rate Limiting + WireGuard Node Agent provisioning hooks.
- **Zero-Logs Design**: WireGuard Curve25519 private keys are generated on-device and never leave the Android Keystore vault. The database stores no browsing history, DNS queries, or traffic payloads.

---

## 2. REST API Documentation

### Authentication
- `POST /api/v1/auth/register` — `{ "email": "...", "password": "..." }` → `201 { token, expires_at, user }`
- `POST /api/v1/auth/login` — `{ "email": "...", "password": "..." }` → `200 { token, expires_at, user }`

### Locations & Server Discovery
- `GET /api/v1/locations` → `{ regions, countries: [{ iso_code, name_en, name_ar, region, cities: [{ id, name_en, name_ar, servers: [...] }] }], updated_at }`
- `GET /api/v1/servers?country_iso=DE&status=ONLINE` → `{ servers: [...] }`
- `GET /api/v1/servers/{server_id}/health` → `{ server_id, status, load_percent, available_capacity_percent, active_sessions, packet_loss_percent, checked_at }`

### VPN Session Provisioning (Authenticated Bearer JWT)
- `POST /api/v1/vpn/sessions`
  - Request: `{ "server_id": "de-fra-01", "client_public_key": "<base64>", "protocol": "WIREGUARD", "preferred_dns": "1.1.1.1" }`
  - Response `201`: `{ session_id, server_id, assigned_ipv4, assigned_ipv6, dns_servers, server_public_key, endpoint, allowed_ips, persistent_keepalive_sec, mtu, expires_at }`
- `DELETE /api/v1/vpn/sessions/{session_id}` → `204 No Content`

### Minimal Diagnostics & Real Speedtest
- `POST /api/v1/diagnostics` → `{ server_id, event, measured_latency_ms, error_code, app_version }`
- `GET /api/v1/speedtest/ping`
- `GET /api/v1/speedtest/download?bytes=8388608`
- `POST /api/v1/speedtest/upload`

### Admin Infrastructure Management (`X-Admin-API-Key`)
- `POST /api/v1/admin/servers` — Dynamically add/update countries, cities, and VPN nodes without app updates.
- `PATCH /api/v1/admin/servers/{id}` — Update server status (`ONLINE`, `BUSY`, `MAINTENANCE`, `OFFLINE`), load, and latency.

---

## 3. Server Deployment Instructions

1. Configure secrets in `backend/.env` (copy from `backend/.env.example`):
   ```bash
   cp .env.example .env
   ```
2. Launch PostgreSQL and the Control Plane behind an HTTPS reverse proxy (Caddy/Nginx/Cloudflare):
   ```bash
   cd backend
   docker compose up -d --build
   ```
3. Open `backend/admin-dashboard/index.html` to register real WireGuard VPN nodes.
4. Enter your HTTPS backend URL inside the NOVA VPN Android app under **Settings → Backend URL** (or set `NOVA_API_BASE_URL` in `.env` before building).

---

## 4. Android Build & Signed Release APK Instructions

### Debug Build & Unit/Robolectric Tests
```bash
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```

### Signed Release APK / AAB
1. Generate an upload keystore (keep offline and secure):
   ```bash
   keytool -genkeypair -v -keystore my-upload-key.jks -keyalg RSA -keysize 4096 -validity 10000 -alias upload
   ```
2. Export signing credentials via environment variables (never hardcode in git):
   ```bash
   export KEYSTORE_PATH=/secure/path/my-upload-key.jks
   export STORE_PASSWORD=your_store_password
   export KEY_PASSWORD=your_key_password
   ```
3. Build signed release APK and Play App Bundle:
   ```bash
   gradle :app:assembleRelease :app:bundleRelease
   ```
   Outputs are generated at `app/build/outputs/apk/release/` and `app/build/outputs/bundle/release/`.
