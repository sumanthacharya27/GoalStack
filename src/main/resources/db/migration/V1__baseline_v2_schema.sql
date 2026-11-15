-- Version 2 schema (users, goals, subgoals), used to build a brand new database.
-- A database that already has these tables is "baselined" at version 1 and this file is skipped.

CREATE TABLE IF NOT EXISTS users (
                                     id            BIGINT       NOT NULL AUTO_INCREMENT,
                                     name          VARCHAR(60)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS goals (
                                     id          BIGINT       NOT NULL AUTO_INCREMENT,
                                     user_id     BIGINT       NULL,
                                     title       VARCHAR(120) NOT NULL,
    description TEXT         NULL,
    deadline    DATE         NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_goals_user_id (user_id),
    CONSTRAINT fk_goals_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS subgoals (
                                        id          BIGINT       NOT NULL AUTO_INCREMENT,
                                        goal_id     BIGINT       NOT NULL,
                                        title       VARCHAR(120) NOT NULL,
    description TEXT         NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_subgoals_goal_id (goal_id),
    CONSTRAINT fk_subgoals_goal FOREIGN KEY (goal_id) REFERENCES goals (id) ON DELETE CASCADE,
    CONSTRAINT chk_subgoals_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED'))
    ) ENGINE=InnoDB;
