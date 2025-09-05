#!/usr/bin/env bash
set -euo pipefail

# --- helpers ---
require_cmd() { command -v "$1" >/dev/null 2>&1 || { echo "Error: $1 not found in PATH"; exit 127; }; }
load_env() {
  if [[ -f ".env.local" ]]; then
    # shellcheck disable=SC1091
    set -a; source .env.local; set +a
  else
    echo "Note: .env.local not found. Using existing environment variables."
  fi
}

# --- checks ---
require_cmd java
if [[ -x "./mvnw" ]]; then MVN="./mvnw"; else require_cmd mvn; MVN="mvn"; fi

# --- env ---
load_env
: "${AUTH0_ISSUER_URI:?Missing AUTH0_ISSUER_URI (set in .env.local)}"
: "${AUTH0_AUDIENCE:?Missing AUTH0_AUDIENCE (set in .env.local)}"

# --- modes ---
MODE="${1:-dev}"   # dev = spring-boot:run, jar = build & run jar

if [[ "$MODE" == "jar" ]]; then
  "$MVN" -q -DskipTests package
  JAR="$(ls -1 target/*-SNAPSHOT.jar target/*.jar 2>/dev/null | head -n1)"
  [[ -n "${JAR:-}" ]] || { echo "Jar not found in target/"; exit 1; }
  exec java -jar "$JAR"
else
  exec "$MVN" spring-boot:run
fi
