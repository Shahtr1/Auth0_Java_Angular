#!/usr/bin/env bash
set -euo pipefail

require_cmd() { command -v "$1" >/dev/null 2>&1 || { echo "Error: $1 not found in PATH"; exit 127; }; }
load_env() {
  if [[ -f ".env.local" ]]; then
    # shellcheck disable=SC1091
    set -a; source .env.local; set +a
  else
    echo "Note: .env.local not found. Using existing environment variables."
  fi
}

require_cmd java
if [[ -x "./mvnw" ]]; then MVN="./mvnw"; else require_cmd mvn; MVN="mvn"; fi

load_env
# required for worker
: "${AUTH0_DOMAIN:?Missing AUTH0_DOMAIN}"
: "${AUTH0_CLIENT_ID:?Missing AUTH0_CLIENT_ID}"
: "${AUTH0_CLIENT_SECRET:?Missing AUTH0_CLIENT_SECRET}"
: "${API_AUDIENCE:?Missing API_AUDIENCE}"
: "${API_BASE:?Missing API_BASE}"

# build shaded jar
"$MVN" -q -DskipTests package
JAR="$(ls -1 target/*.jar 2>/dev/null | head -n1)"
[[ -n "${JAR:-}" ]] || { echo "Shaded jar not found in target/ (did maven-shade-plugin run?)"; exit 1; }

# pass-through args: "" (read) or "write"
exec java -jar "$JAR" "${@:-}"
