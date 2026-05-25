-- ══════════════════════════════════════════════════════════════
--  CivicCMS — Fix: Keep ONLY 6 departments in admin dashboard
--  Database: civic_cms
--  Run this in MySQL Workbench → File → Open SQL Script → Execute
-- ══════════════════════════════════════════════════════════════

USE civic_cms;

-- Step 1: Re-route any existing complaints away from depts being deleted
UPDATE complaints c
JOIN departments src  ON src.id = c.assigned_dept_id AND src.code = 'DRAINAGE'
JOIN departments dest ON dest.code = 'GARBAGE'
SET c.assigned_dept_id = dest.id;

UPDATE complaints c
JOIN departments src  ON src.id = c.assigned_dept_id AND src.code = 'STREETLIGHT'
JOIN departments dest ON dest.code = 'ELECTRICITY'
SET c.assigned_dept_id = dest.id;

UPDATE complaints c
JOIN departments src  ON src.id = c.assigned_dept_id AND src.code = 'PARK'
JOIN departments dest ON dest.code = 'OTHER'
SET c.assigned_dept_id = dest.id;

UPDATE complaints c
JOIN departments src  ON src.id = c.assigned_dept_id AND src.code = 'NOISE'
JOIN departments dest ON dest.code = 'ANIMAL'
SET c.assigned_dept_id = dest.id;

UPDATE complaints c
JOIN departments src  ON src.id = c.assigned_dept_id AND src.code = 'ENCROACHMENT'
JOIN departments dest ON dest.code = 'ANIMAL'
SET c.assigned_dept_id = dest.id;

-- Step 2: Delete the 5 unwanted departments
DELETE FROM departments WHERE code IN ('DRAINAGE','STREETLIGHT','PARK','NOISE','ENCROACHMENT');

-- Step 3: Rename the remaining rows to clean names
UPDATE departments SET name = 'Roads'         WHERE code = 'ROAD';
UPDATE departments SET name = 'Sanitation'    WHERE code = 'GARBAGE';
UPDATE departments SET name = 'Electricity'   WHERE code = 'ELECTRICITY';
UPDATE departments SET name = 'Water'         WHERE code = 'WATER';
UPDATE departments SET name = 'Public Safety' WHERE code = 'ANIMAL';
UPDATE departments SET name = 'General'       WHERE code = 'OTHER';

-- Step 4: Confirm — should show EXACTLY 6 rows
SELECT id, name, code FROM departments ORDER BY id;
