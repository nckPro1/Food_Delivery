-- =============================================
-- MIGRATION SCRIPT: Update Order Status Enum
-- =============================================

-- Backup existing orders table (optional)
-- CREATE TABLE orders_backup AS SELECT * FROM orders;

-- Step 1: Update existing data to match new enum values
-- Convert any old status values to new ones
UPDATE orders SET status = 'DONE' WHERE status IN ('DELIVERED', 'COMPLETED');
UPDATE orders SET status = 'CONFIRMED' WHERE status IN ('PREPARING', 'READY');
-- Keep PENDING, CONFIRMED, DELIVERING as they are

-- Step 2: Modify the enum column
-- Note: MySQL doesn't support direct enum modification, so we need to recreate the column

-- Add temporary column
ALTER TABLE orders ADD COLUMN status_new ENUM('PENDING', 'CONFIRMED', 'DELIVERING', 'DONE') NOT NULL DEFAULT 'PENDING';

-- Copy data from old column to new column
UPDATE orders SET status_new = status;

-- Drop old column
ALTER TABLE orders DROP COLUMN status;

-- Rename new column to original name
ALTER TABLE orders CHANGE COLUMN status_new status ENUM('PENDING', 'CONFIRMED', 'DELIVERING', 'DONE') NOT NULL DEFAULT 'PENDING';

-- Step 3: Update indexes
DROP INDEX IF EXISTS idx_orders_status;
CREATE INDEX idx_orders_status ON orders(status);

-- Step 4: Verify the changes
SELECT DISTINCT status, COUNT(*) as count FROM orders GROUP BY status;

-- =============================================
-- ROLLBACK SCRIPT (if needed)
-- =============================================
-- To rollback, restore from backup:
-- DROP TABLE orders;
-- RENAME TABLE orders_backup TO orders;

