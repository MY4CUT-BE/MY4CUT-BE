CREATE TABLE IF NOT EXISTS user_tutorials (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    home_completed BOOLEAN NOT NULL DEFAULT FALSE,
    workspace_completed BOOLEAN NOT NULL DEFAULT FALSE,
    photo_upload_completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_tutorials_user_id UNIQUE (user_id),
    CONSTRAINT fk_user_tutorials_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

INSERT INTO user_tutorials (
    user_id,
    home_completed,
    workspace_completed,
    photo_upload_completed,
    created_at
)
SELECT
    users.id,
    FALSE,
    FALSE,
    FALSE,
    NOW(6)
FROM users
LEFT JOIN user_tutorials
    ON user_tutorials.user_id = users.id
WHERE user_tutorials.user_id IS NULL;
