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

## Upload limits in production

Defaults: 20 MiB per file, 205 MiB per multipart request, with at most 10 files
in `/media/upload/bulk`. Spring `MB` and Nginx `m` use 1024-based units.
These multipart limits apply to all upload endpoints; endpoint-specific validation
still applies (pose images retain their own 5 MiB limit). Larger uploads consume
more temporary disk, transfer time, and storage. Check available disk and memory
and monitor upload failures after changing limits.

### Apply

The current production host runs Nginx as a systemd service, outside Compose.
The production CD workflow pulls images but does not synchronize the host Compose
file or Nginx configuration. Update those host files separately.

1. Back up the host Compose file, `.env`, and Nginx virtual host outside Git.
   Record the current image tag and existing multipart environment overrides.
2. In `/etc/nginx/sites-available/api.my4cut.shop`, replace the existing
   `client_max_body_size 55m;` in the HTTPS server block with
   `client_max_body_size 210m;`. Check for smaller location-level overrides.
   Run `sudo nginx -t` and only if it succeeds, `sudo systemctl reload nginx`.
3. Update the host Compose file with the environment defaults in this directory.
   Alternatively, the existing host Compose uses `env_file: .env`, so add these
   values to that `.env` unless Compose `environment` already overrides them:

   ```dotenv
   SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=20MB
   SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE=205MB
   ```

   Environment overrides allow applying limits to the existing image without a
   rebuild. Do not print or commit the production `.env`.
4. In the host deployment directory, recreate only the API, preserving its image:

   ```sh
   running_image=$(docker inspect --format '{{.Config.Image}}' my4cut-api)
   IMAGE_TAG="${running_image##*:}" docker compose up -d --no-deps --force-recreate api
   curl --fail --silent --show-error http://127.0.0.1:8080/v3/api-docs > /dev/null
   ```

   API recreation briefly interrupts this single-instance service. A plain
   `docker compose restart` does not apply new environment values. Retry the
   health check while startup completes and inspect `docker compose logs --since 5m api`.
5. With a test account in a test environment, send real multipart HTTP requests:
   6 MiB and exactly 20 MiB files should pass parsing, 20 MiB + 1 byte should
   fail, ten 20 MiB files should pass the aggregate limit, and a request over
   205 MiB should fail. Verify both direct Tomcat and public Nginx paths.
   File-content validation can reject synthetic files after parsing.

### Rollback

Restore the previous Nginx directive, Compose configuration, and multipart
`.env` values. Validate/reload Nginx and recreate only the API using the same
image tag. Monitor disk usage and 400/408/499 responses. These changes address
size limits, not database deadlocks or independently caused upload timeouts.
