## Auth0 + Angular + Spring “Orders” Demo

A tiny but real end-to-end app that shows OIDC login and API RBAC with Auth0.

Tech

- Frontend: Angular 20 SPA (public client) using Authorization Code + PKCE
- Backend API: Spring Boot 3.5 (resource server, JWT) with RBAC and permissions
- Workers/Tools:
  - orders-worker (Java) using Client Credentials (machine-to-machine)
  - orders-cli (Java) using Device Authorization (device code) for user login without a browser session

Permissions used (RBAC):
`read:orders`, `write:orders`

Flows implemented (3):

1.  Authorization Code + PKCE (Angular SPA → Auth0 → Orders API)
2.  Client Credentials (orders-worker → Auth0 → Orders API)
3.  Device Authorization (orders-cli → Auth0 → Orders API)

Remaining (optional/coming next): Refresh Token grant details for the CLI, and ROPC (not recommended for browsers; only for special backend-only cases).

## Repo layout

```bash
Auth0_Java_Angular/
├─ docs/ # documentation
├─ orders-api/ # Spring Boot resource server (JWT)
├─ orders-spa/ # Angular 20 SPA (Auth Code + PKCE)
├─ orders-worker/ # Java worker (Client Credentials)
├─ orders-cli/ # Java CLI (Device Authorization / device code)
└─ .gitignore
```

## What you’ll build/test

- GET /api/orders — requires `read:orders`
- POST /api/orders — requires `write:orders`
- GET /api/whoami — shows what the API sees in your token (iss/aud/permissions/authorities)

The Angular SPA logs a user in and calls these endpoints.
The Java worker calls them as a service (no user).
The Java CLI logs a user in via device code, then calls the API.

## Prerequisites

- Node.js 22 LTS (Angular 20 compatible)
- Angular CLI latest
- Java 21 (Temurin or similar)
- Maven 3.9+
- An Auth0 tenant (or free account)

## Auth0 configuration (one time)

### 1. API (represents the Spring backend)

- Create Orders API
  - Identifier (audience): `https://orders-api`
  - Signing algorithm: RS256
- Enable RBAC and Add Permissions in the Access Token
- Add Permissions: read:orders, write:orders
- (Optional) Allow Offline Access if you want refresh tokens for long-lived clients (used later)

### 2. Angular SPA application

- Create Single Page Application (e.g., AngularSPA)
- Allowed URLs (dev):
  - Callback: `http://localhost:4200`
  - Logout: `http://localhost:4200`
  - Web Origins: `http://localhost:4200`
- Enable Refresh Token Rotation (pairs with offline_access scope requested by the SPA)
- Keep note of Domain and Client ID

### 3. Machine-to-Machine application (orders-worker)

- Create M2M app; authorize it to Orders API
- Grant scopes as needed:
  - Reader worker: `read:orders`
  - Writer worker: `read:orders write:orders`
- Keep note of Client ID / Client Secret

### 4. Native application (orders-cli)

- Create Native app; enable Device Authorization
- Authorize it to Orders API
- Scopes: at least `openid profile email read:orders` (+ `write:orders` if needed). Add `offline_access` if you want refresh.

## Troubleshooting

- 401 Unauthorized

  - Angular: ensure the interceptor attaches tokens to `/api/\*` and you requested the correct audience (`https://orders-api`)
  - Spring: `issuer-uri` must include a trailing `/`
  - Decode the token and confirm `aud` and `iss`

- 403 Forbidden

  - User/app lacks `read:orders` or `write:orders`
  - In Auth0, grant the scope (API → Machine-to-Machine Applications / App → APIs tab), then mint a new token

- CORS in dev
  - Use the Angular proxy (`proxy.conf.json`) so SPA calls `/api/...` and the dev server forwards to `8080`
  - Or configure Spring CORS to allow `http://localhost:4200` during dev

Usage quickies

From each app directory:

```bash
# make executable once
chmod +x run.sh

# orders-api
./run.sh              # dev (spring-boot:run)
./run.sh jar          # build & run jar

# orders-worker
./run.sh              # read flow (GET)
./run.sh write        # write flow (POST)

# orders-cli
./run.sh              # read flow
./run.sh write        # write flow

# orders-spa
npm start
```
