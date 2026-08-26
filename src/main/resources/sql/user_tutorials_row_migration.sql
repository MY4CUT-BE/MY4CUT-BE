-- 기존 user_tutorials 테이블을 화면별 완료 행 구조로 전환하는 일회성 마이그레이션입니다.
-- 애플리케이션 배포 전에 실행하고, 검증이 끝날 때까지 legacy 테이블을 유지합니다.

CREATE TABLE user_tutorials_new (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    tutorial_type VARCHAR(50) NOT NULL,
    completed_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_tutorials_user_type UNIQUE (user_id, tutorial_type),
    CONSTRAINT fk_user_tutorials_new_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

INSERT INTO user_tutorials_new (
    user_id,
    tutorial_type,
    completed_at,
    created_at
)
SELECT user_id, 'HOME', NOW(6), created_at
FROM user_tutorials
WHERE home_completed = TRUE
UNION ALL
SELECT user_id, 'UPLOAD_DATE', NOW(6), created_at
FROM user_tutorials
WHERE photo_upload_completed = TRUE
UNION ALL
SELECT user_id, 'UPLOAD_CONTENT', NOW(6), created_at
FROM user_tutorials
WHERE photo_upload_completed = TRUE
UNION ALL
SELECT user_id, 'RETOUCH_MAIN', NOW(6), created_at
FROM user_tutorials
WHERE workspace_completed = TRUE
UNION ALL
SELECT user_id, 'RETOUCH_SPACE', NOW(6), created_at
FROM user_tutorials
WHERE workspace_completed = TRUE
UNION ALL
SELECT user_id, 'RETOUCH_PHOTO', NOW(6), created_at
FROM user_tutorials
WHERE workspace_completed = TRUE
UNION ALL
SELECT user_id, 'RETOUCH_DETAIL', NOW(6), created_at
FROM user_tutorials
WHERE workspace_completed = TRUE;

RENAME TABLE
    user_tutorials TO user_tutorials_legacy,
    user_tutorials_new TO user_tutorials;

-- 신규 버전 안정화 및 데이터 검증 후 별도 작업으로 제거합니다.
-- DROP TABLE user_tutorials_legacy;
