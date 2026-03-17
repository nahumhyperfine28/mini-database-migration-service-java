USE source_db;

INSERT INTO customers (customer_id, email, full_name, status, loyalty_points, created_at, updated_at) VALUES
    (1, 'ava.lee@example.com', 'Ava Lee', 'ACTIVE', 120, NOW(), NOW()),
    (2, 'marcus.chen@example.com', 'Marcus Chen', 'ACTIVE', 45, NOW(), NOW()),
    (3, 'nina.patel@example.com', 'Nina Patel', 'SUSPENDED', 10, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    status = VALUES(status),
    loyalty_points = VALUES(loyalty_points),
    updated_at = VALUES(updated_at);

INSERT INTO products (product_id, sku, product_name, price, active, created_at, updated_at) VALUES
    (101, 'LAPTOP-14', '14 inch Laptop', 1299.00, TRUE, NOW(), NOW()),
    (102, 'MOUSE-WL', 'Wireless Mouse', 39.99, TRUE, NOW(), NOW()),
    (103, 'DOCK-USB-C', 'USB-C Dock', 149.50, TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    product_name = VALUES(product_name),
    price = VALUES(price),
    active = VALUES(active),
    updated_at = VALUES(updated_at);

INSERT INTO orders (order_id, customer_id, product_id, quantity, total_amount, order_status, created_at, updated_at) VALUES
    (1001, 1, 101, 1, 1299.00, 'PLACED', NOW(), NOW()),
    (1002, 2, 102, 2, 79.98, 'SHIPPED', NOW(), NOW()),
    (1003, 1, 103, 1, 149.50, 'PLACED', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    quantity = VALUES(quantity),
    total_amount = VALUES(total_amount),
    order_status = VALUES(order_status),
    updated_at = VALUES(updated_at);

INSERT INTO inventory (inventory_id, product_id, warehouse_code, available_quantity, reserved_quantity, updated_at) VALUES
    (5001, 101, 'SEA-1', 12, 1, NOW()),
    (5002, 102, 'SEA-1', 55, 4, NOW()),
    (5003, 103, 'LAX-2', 21, 2, NOW())
ON DUPLICATE KEY UPDATE
    available_quantity = VALUES(available_quantity),
    reserved_quantity = VALUES(reserved_quantity),
    updated_at = VALUES(updated_at);
