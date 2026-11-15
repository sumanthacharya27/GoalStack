-- Version 3, phase 1: effort-based weights.
--
-- Safety check: this migration needs the Version 2 tables. If one of the next two lines fails with
-- "Table ... doesn't exist" or "Unknown column", your database is still at Version 1. Run
-- database/upgrade_v1_to_v2.sql once, then start the app again. Nothing has been changed yet at this point.
SELECT COUNT(*) FROM users;
SELECT user_id FROM goals LIMIT 0;

-- 1. Every goal that exists right now was made before Version 3: equal weights forever.
ALTER TABLE goals ADD COLUMN weighting_mode VARCHAR(10) NOT NULL DEFAULT 'EQUAL';
ALTER TABLE goals ALTER COLUMN weighting_mode DROP DEFAULT;

-- 2. New subgoal columns. estimated_hours stays empty for old subgoals (no invented numbers).
ALTER TABLE subgoals
    ADD COLUMN estimated_hours DECIMAL(7,2) NULL,
    ADD COLUMN weight          DECIMAL(5,2) NULL,
    ADD COLUMN sort_order      INT          NULL;

-- 3. Backfill: order by id, and give every goal's subgoals equal weights that total exactly 100.00.
--    Same rule as WeightedProgressService.equalWeights: base = floor(10000 / n) hundredths of a percent,
--    and the first (10000 mod n) subgoals get one extra hundredth. Example: 3 subgoals -> 33.34, 33.33, 33.33.
UPDATE subgoals s
    JOIN (
    SELECT id,
    ROW_NUMBER() OVER (PARTITION BY goal_id ORDER BY id) AS rn,
    COUNT(*)     OVER (PARTITION BY goal_id)             AS n
    FROM subgoals
    ) x ON x.id = s.id
    SET s.sort_order = x.rn,
        s.weight = (FLOOR(10000 / x.n)
        + CASE WHEN x.rn <= (10000 - x.n * FLOOR(10000 / x.n)) THEN 1 ELSE 0 END) / 100;

-- 4. Now every row has a weight and an order, so they become required.
ALTER TABLE subgoals
    MODIFY weight     DECIMAL(5,2) NOT NULL,
    MODIFY sort_order INT          NOT NULL;

ALTER TABLE subgoals
    ADD CONSTRAINT chk_subgoals_hours  CHECK (estimated_hours IS NULL OR (estimated_hours >= 0.25 AND estimated_hours <= 1000)),
    ADD CONSTRAINT chk_subgoals_weight CHECK (weight >= 0 AND weight <= 100);

CREATE INDEX idx_subgoals_goal_order ON subgoals (goal_id, sort_order);
