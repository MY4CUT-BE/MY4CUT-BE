-- 기존 워크스페이스의 owner_id는 역할 구분에 더 이상 사용하지 않는다.
-- MySQL에서 owner_id에 연결된 외래 키를 이름과 무관하게 제거한다.
SET @owner_fk_name = (
    SELECT kcu.CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'workspaces'
      AND kcu.COLUMN_NAME = 'owner_id'
      AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
    LIMIT 1
);

SET @drop_owner_fk_sql = IF(
    @owner_fk_name IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE workspaces DROP FOREIGN KEY `', @owner_fk_name, '`')
);
PREPARE drop_owner_fk_statement FROM @drop_owner_fk_sql;
EXECUTE drop_owner_fk_statement;
DEALLOCATE PREPARE drop_owner_fk_statement;

ALTER TABLE workspaces DROP COLUMN owner_id;
