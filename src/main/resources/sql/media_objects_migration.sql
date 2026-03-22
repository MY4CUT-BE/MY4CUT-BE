CREATE TABLE media_objects (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    sha256 VARCHAR(64) NULL,
    file_key VARCHAR(255) NOT NULL,
    file_size BIGINT NULL,
    content_type VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_media_objects_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT uk_media_object_owner_hash_size UNIQUE (owner_id, sha256, file_size)
);

ALTER TABLE media_files
    ADD COLUMN media_object_id BIGINT NULL;

ALTER TABLE media_files
    ADD CONSTRAINT fk_media_files_media_object
        FOREIGN KEY (media_object_id) REFERENCES media_objects (id);

INSERT INTO media_objects (
    owner_id,
    sha256,
    file_key,
    file_size,
    content_type,
    status,
    created_at,
    deleted_at
)
SELECT
    mf.uploader_id,
    NULL,
    mf.file_url,
    NULL,
    NULL,
    'ACTIVE',
    mf.created_at,
    NULL
FROM media_files mf;

UPDATE media_files mf
JOIN media_objects mo
    ON mo.owner_id = mf.uploader_id
   AND mo.file_key = mf.file_url
   AND mo.created_at = mf.created_at
SET mf.media_object_id = mo.id
WHERE mf.media_object_id IS NULL;

ALTER TABLE media_files
    MODIFY COLUMN media_object_id BIGINT NOT NULL;

CREATE INDEX idx_media_files_media_object_id
    ON media_files (media_object_id);

CREATE INDEX idx_media_objects_status
    ON media_objects (status);

-- Backfill strategy:
-- 1. Run the DDL and data copy above in a maintenance window.
-- 2. Existing rows are migrated 1:1 into media_objects with sha256/file_size left NULL.
--    MySQL unique indexes allow multiple NULL combinations, so legacy rows do not collide.
-- 3. New uploads start writing real sha256/file_size values and deduplicate only for new data.
-- 4. If full legacy dedup is needed later, run an offline job that downloads each legacy S3 object,
--    computes sha256/file_size, updates media_objects, and merges duplicate business references carefully.
