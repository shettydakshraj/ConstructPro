-- =======================================================
-- CONSTRUCTPRO: CONSTRUCTION MANAGEMENT SYSTEM
-- DBMS LAB SUBMISSION: DDL & DML COMMANDS
-- Domain: Construction Management & Project Schedule Analysis
-- =======================================================

-- -------------------------------------------------------
-- SECTION 1: DATA DEFINITION LANGUAGE (DDL)
-- -------------------------------------------------------

-- 1. Create PROJECTS Table
-- Uses only core project columns: ID, Name, Location, Manager, Progress, Status
CREATE TABLE PROJECTS (
    PROJECT_ID VARCHAR2(10) PRIMARY KEY,
    PROJECT_NAME VARCHAR2(100) NOT NULL,
    LOCATION VARCHAR2(50) NOT NULL,
    MANAGER VARCHAR2(50) NOT NULL,
    PROGRESS NUMBER(3) CHECK (PROGRESS BETWEEN 0 AND 100),
    STATUS VARCHAR2(20) CHECK (STATUS IN ('ACTIVE', 'COMPLETED', 'PLANNING'))
);


-- -------------------------------------------------------
-- SECTION 2: DATA MANIPULATION LANGUAGE (DML)
-- -------------------------------------------------------

-- 1. INSERT INITIAL DATA (DML: INSERT)
INSERT INTO PROJECTS (PROJECT_ID, PROJECT_NAME, LOCATION, MANAGER, PROGRESS, STATUS)
VALUES ('P001', 'Academic Block', 'Chennai', 'R. Kumar', 80, 'ACTIVE');

INSERT INTO PROJECTS (PROJECT_ID, PROJECT_NAME, LOCATION, MANAGER, PROGRESS, STATUS)
VALUES ('P002', 'Hostel Complex', 'Chennai', 'S. Arun', 62, 'ACTIVE');

INSERT INTO PROJECTS (PROJECT_ID, PROJECT_NAME, LOCATION, MANAGER, PROGRESS, STATUS)
VALUES ('P003', 'Central Library', 'Vellore', 'A. Prakash', 100, 'COMPLETED');

INSERT INTO PROJECTS (PROJECT_ID, PROJECT_NAME, LOCATION, MANAGER, PROGRESS, STATUS)
VALUES ('P004', 'Research Centre', 'Chennai', 'V. Raj', 35, 'PLANNING');


-- 2. SELECT QUERIES (DML: SELECT)
-- View all projects
SELECT * FROM PROJECTS;

-- Query active projects with high progress (>= 75%)
SELECT PROJECT_ID, PROJECT_NAME, LOCATION, MANAGER, PROGRESS, STATUS
FROM PROJECTS
WHERE STATUS = 'ACTIVE' AND PROGRESS >= 75;

-- Aggregate query: Summary of projects by status and average progress
SELECT 
    STATUS,
    COUNT(*) AS TOTAL_PROJECTS,
    AVG(PROGRESS) AS AVERAGE_PROGRESS,
    MIN(PROGRESS) AS MIN_PROGRESS,
    MAX(PROGRESS) AS MAX_PROGRESS
FROM PROJECTS
GROUP BY STATUS;


-- 3. UPDATE QUERIES (DML: UPDATE)
-- Update project progress and status based on evaluation
UPDATE PROJECTS 
SET PROGRESS = 85, STATUS = 'ACTIVE' 
WHERE PROJECT_ID = 'P001';

-- Mark completed project
UPDATE PROJECTS
SET PROGRESS = 100, STATUS = 'COMPLETED'
WHERE PROJECT_ID = 'P002';


-- 4. DELETE QUERIES (DML: DELETE)
-- Delete a project by ID
DELETE FROM PROJECTS 
WHERE PROJECT_ID = 'P004';

COMMIT;
