-- Sample data for Supply Chain Management Application
-- Loaded automatically by Spring Boot from classpath

INSERT INTO products (name, sku, description, price, quantity_in_stock, reorder_level, supplier, category, status, created_at, updated_at)
VALUES
    ('Industrial Servo Motor', 'SKU-MOTOR-001', 'High-torque servo motor for assembly lines', 1250.00, 45, 10, 'AcmeCorp', 'Electronics', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Steel Ball Bearings (Box/100)', 'SKU-BEAR-002', 'Precision steel ball bearings', 89.99, 200, 50, 'GlobalParts', 'Mechanical', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Hydraulic Cylinder 50mm', 'SKU-HYD-003', '50mm bore hydraulic cylinder', 475.00, 5, 10, 'HydroTech', 'Hydraulics', 'LOW_STOCK', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('PLC Controller Module', 'SKU-PLC-004', 'Programmable logic controller', 3200.00, 0, 5, 'AcmeCorp', 'Electronics', 'OUT_OF_STOCK', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Conveyor Belt 5m', 'SKU-CONV-005', '5-meter industrial conveyor belt', 890.00, 30, 8, 'BeltWorks', 'Material Handling', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO orders (order_number, order_type, status, supplier, destination, total_amount, created_at, updated_at, expected_delivery, notes)
VALUES
    ('PO-2024-001', 'PURCHASE', 'PROCESSING', 'AcmeCorp', 'Warehouse-East', 15400.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, DATEADD('DAY', 7, CURRENT_TIMESTAMP), 'Urgent restock of servo motors'),
    ('PO-2024-002', 'SHIPMENT', 'SHIPPED', 'GlobalParts', 'Warehouse-West', 4500.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, DATEADD('DAY', 3, CURRENT_TIMESTAMP), 'Regular weekly shipment'),
    ('PO-2024-003', 'PURCHASE', 'PENDING', 'HydroTech', 'Warehouse-East', 9500.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, DATEADD('DAY', 14, CURRENT_TIMESTAMP), 'Hydraulic components reorder');
