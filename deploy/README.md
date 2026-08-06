# Production deployment

The production Compose configuration enables Firebase Cloud Messaging by default.
Before starting the API container, place the Firebase Admin service-account JSON on the host and point `FIREBASE_CONFIG_FILE` at it.

```shell
mkdir -p secrets
chmod 700 secrets
# Copy the service-account JSON to secrets/firebase-service-account.json without committing it.
chmod 600 secrets/firebase-service-account.json
cp .env.example .env
docker compose up -d
```

The host file is mounted read-only at `/run/secrets/firebase-service-account.json` in the API container. The application fails during startup when Firebase is enabled but the credential path is missing or unreadable. Set `FIREBASE_ENABLED=false` only for an environment where push delivery is intentionally disabled; the credential mount target must still exist for Compose to start the container.
