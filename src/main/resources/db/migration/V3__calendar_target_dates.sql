-- Add optional target dates only when the column does not already exist.
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'subgoals'
      AND COLUMN_NAME = 'target_date'
);

SET @sql = IF(
    @column_exists = 0,
    'ALTER TABLE subgoals ADD COLUMN target_date DATE NULL',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;