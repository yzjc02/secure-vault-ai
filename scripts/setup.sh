#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env"

random_secret() {
  bytes="$1"
  length="$2"

  if command -v openssl >/dev/null 2>&1; then
    openssl rand -base64 "$bytes" | tr '+/' '-_' | tr -d '=' | cut -c "1-$length"
  else
    od -An -N "$bytes" -tx1 /dev/urandom | tr -d ' \n' | cut -c "1-$length"
  fi
}

if [ -f "$ENV_FILE" ]; then
  printf '%s\n' ".env already exists. Keeping existing local configuration."
  printf '%s\n' "Next step:"
  printf '%s\n' "  docker compose up -d"
  exit 0
fi

JWT_SECRET="$(random_secret 72 96)"
POSTGRES_PASSWORD="$(random_secret 36 48)"

cat > "$ENV_FILE" <<EOF
APP_PORT=8080
SPRING_PROFILES_ACTIVE=prod
POSTGRES_DB=securevault
POSTGRES_USER=securevault_user
POSTGRES_PASSWORD=$POSTGRES_PASSWORD
JWT_SECRET=$JWT_SECRET
JWT_EXPIRATION=86400000
FILE_STORAGE_DIR=/app/data/uploads
EOF

printf '%s\n' ".env created with local secrets."
printf '%s\n' "Next step:"
printf '%s\n' "  docker compose up -d"
