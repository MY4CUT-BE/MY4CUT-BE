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
                               legacy_media_file_id BIGINT NULL,
                               PRIMARY KEY (id),
                               CONSTRAINT fk_media_objects_owner
                                   FOREIGN KEY (owner_id) REFERENCES users (id),
                               CONSTRAINT uk_media_object_owner_hash_size
                                   UNIQUE (owner_id, sha256, file_size),
                               CONSTRAINT uk_media_objects_legacy_media_file_id
                                   UNIQUE (legacy_media_file_id)
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
    deleted_at,
    legacy_media_file_id
)
SELECT
    mf.uploader_id,
    NULL,
    mf.file_url,
    NULL,
    NULL,
    'ACTIVE',
    mf.created_at,
    NULL,
    mf.id
FROM media_files mf;

UPDATE media_files mf
    JOIN media_objects mo
    ON mo.legacy_media_file_id = mf.id
SET mf.media_object_id = mo.id
WHERE mf.media_object_id IS NULL;

SELECT COUNT(*) AS unlinked_media_files
FROM media_files
WHERE media_object_id IS NULL;

SELECT COUNT(*) AS broken_links
FROM media_files mf
         LEFT JOIN media_objects mo ON mo.id = mf.media_object_id
WHERE mo.id IS NULL;

ALTER TABLE media_files
    MODIFY COLUMN media_object_id BIGINT NOT NULL;

CREATE INDEX idx_media_files_media_object_id
    ON media_files (media_object_id);

CREATE INDEX idx_media_objects_status
    ON media_objects (status);

ALTER TABLE media_objects
    DROP INDEX uk_media_objects_legacy_media_file_id;

ALTER TABLE media_objects
    DROP COLUMN legacy_media_file_id;

-- Backfill strategy:
-- 1. 기존 media_files row를 media_objects로 1:1 백필한다.
-- 2. migration 중에는 legacy_media_file_id로 안정적으로 매핑한다.
-- 3. 기존 데이터는 sha256/file_size를 NULL로 두어 즉시 dedup하지 않는다.
-- 4. 신규 업로드부터 실제 sha256/file_size 기반 dedup을 적용한다.
