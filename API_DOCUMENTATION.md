# Supply Chain Management — API Documentation & Testing Guide

This document provides the complete API reference for the Supply Chain Management application, including request methods, URL paths, headers, payload examples, and ready-to-run `curl` commands.

- **Base URL**: `http://localhost:8080`
- **Default Content-Type**: `application/json`

---

## Table of Contents
1. [Actuator & Monitoring Endpoints](#1-actuator--monitoring-endpoints)
2. [Product Management APIs](#2-product-management-apis-apiproducts)
3. [Order Management APIs](#3-order-management-apis-apiorders)
4. [Failure Simulation & Logging APIs](#4-failure-simulation--logging-apis-apisimulate)
5. [Data Models & Enums](#5-data-models--enums)
6. [Automated Smoke Test Script](#6-automated-smoke-test-script)

---

## 1. Actuator & Monitoring Endpoints

### 1.1 Application Health Check
Checks application readiness, disk space, and H2 database connectivity.

```bash
curl -X GET http://localhost:8080/actuator/health
```

**Expected Response (200 OK):**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "H2",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 158914838528,
        "free": 88452755456,
        "threshold": 10485760,
        "exists": true
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### 1.2 Application Info
Displays build and application metadata.

```bash
curl -X GET http://localhost:8080/actuator/info
```

### 1.3 Application Metrics
Returns a list of all metrics tracked by Micrometer.

```bash
curl -X GET http://localhost:8080/actuator/metrics
```

To view a specific metric (e.g. JVM memory used):
```bash
curl -X GET http://localhost:8080/actuator/metrics/jvm.memory.used
```

---

## 2. Product Management APIs (`/api/products`)

Controller: `ProductController`

### 2.1 Get All Products
Fetches all products stored in the inventory.

```bash
curl -X GET http://localhost:8080/api/products
```

### 2.2 Get Product by ID
Retrieves single product details by its numeric ID.

```bash
curl -X GET http://localhost:8080/api/products/1
```

### 2.3 Get Product by SKU
Retrieves product details by its unique SKU code.

```bash
curl -X GET http://localhost:8080/api/products/sku/SKU-MOTOR-001
```

### 2.4 Get Low-Stock Products
Filters products whose inventory level has reached or dropped below the configured `reorderLevel` or has `LOW_STOCK` status.

```bash
curl -X GET http://localhost:8080/api/products/low-stock
```

### 2.5 Create a Product
Adds a new product to inventory.

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Optical Sensor Pro",
    "sku": "SKU-SENS-101",
    "description": "High precision proximity optical sensor",
    "price": 149.99,
    "quantityInStock": 80,
    "reorderLevel": 20,
    "supplier": "AcmeCorp",
    "category": "Electronics",
    "status": "ACTIVE"
  }'
```

**Expected Response (201 Created):**
```json
{
  "id": 6,
  "name": "Optical Sensor Pro",
  "sku": "SKU-SENS-101",
  "description": "High precision proximity optical sensor",
  "price": 149.99,
  "quantityInStock": 80,
  "reorderLevel": 20,
  "supplier": "AcmeCorp",
  "category": "Electronics",
  "status": "ACTIVE",
  "createdAt": "2026-10-22T23:45:00.000",
  "updatedAt": "2026-10-22T23:45:00.000"
}
```

### 2.6 Update a Product
Modifies an existing product by ID.

```bash
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Industrial Servo Motor v2",
    "sku": "SKU-MOTOR-001",
    "description": "Upgraded high-torque servo motor for automated assembly lines",
    "price": 1350.00,
    "quantityInStock": 40,
    "reorderLevel": 10,
    "supplier": "AcmeCorp",
    "category": "Electronics",
    "status": "ACTIVE"
  }'
```

### 2.7 Delete a Product
Deletes a product by ID.

```bash
curl -i -X DELETE http://localhost:8080/api/products/5
```
**Expected Response:** `HTTP/1.1 204 No Content`

---

## 3. Order Management APIs (`/api/orders`)

Controller: `OrderController`

### 3.1 Get All Orders
Returns all registered orders.

```bash
curl -X GET http://localhost:8080/api/orders
```

### 3.2 Get Order by ID
Retrieves order details by numeric database ID.

```bash
curl -X GET http://localhost:8080/api/orders/1
```

### 3.3 Get Order by Order Number
Retrieves order by its business identifier string.

```bash
curl -X GET http://localhost:8080/api/orders/number/PO-2024-001
```

### 3.4 Filter Orders by Status
Returns all orders matching a specific status (`PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`, `FAILED`).

```bash
curl -X GET http://localhost:8080/api/orders/status/PROCESSING
```

### 3.5 Create an Order
Creates a new purchase/shipment order with line items.

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "orderNumber": "PO-2024-100",
    "orderType": "PURCHASE",
    "supplier": "AcmeCorp",
    "destination": "Warehouse-Central",
    "totalAmount": 7500.00,
    "notes": "Emergency restock batch",
    "items": [
      {
        "productSku": "SKU-MOTOR-001",
        "productName": "Industrial Servo Motor",
        "quantity": 5,
        "unitPrice": 1250.00
      },
      {
        "productSku": "SKU-BEAR-002",
        "productName": "Steel Ball Bearings",
        "quantity": 10,
        "unitPrice": 89.99
      }
    ]
  }'
```

**Expected Response (201 Created):**
```json
{
  "id": 4,
  "orderNumber": "PO-2024-100",
  "orderType": "PURCHASE",
  "status": "PENDING",
  "supplier": "AcmeCorp",
  "destination": "Warehouse-Central",
  "totalAmount": 7500.0,
  "items": [...],
  "createdAt": "2026-10-22T23:45:00.000",
  "updatedAt": "2026-10-22T23:45:00.000"
}
```

### 3.6 Update Order Status
Updates the progress state of an existing order.

```bash
curl -X PATCH http://localhost:8080/api/orders/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "SHIPPED"}'
```

### 3.7 Delete an Order
Deletes an order by ID.

```bash
curl -i -X DELETE http://localhost:8080/api/orders/3
```
**Expected Response:** `HTTP/1.1 204 No Content`

---

## 4. Failure Simulation & Logging APIs (`/api/simulate`)

Controller: `SimulationController`  
These endpoints trigger simulated enterprise failures, outputting structured JSON logs for Logstash, Filebeat, and Kibana alerting.

### 4.1 Simulation Health & Endpoints Discovery
Lists all available simulation routes.

```bash
curl -X GET http://localhost:8080/api/simulate/health
```

### 4.2 Simulate Database Connection Pool Exhaustion
Simulates database failure with pool starvation (`HTTP 500`).

```bash
curl -X POST http://localhost:8080/api/simulate/db-failure
```

### 4.3 Simulate External Supplier Sync Failure
Simulates external supplier API HTTP 503 outage and retries exhaustion.

```bash
curl -X POST http://localhost:8080/api/simulate/inventory-sync-failure
```

### 4.4 Simulate Lost In-Transit Shipment
Simulates lost tracking signal for shipping containers.

```bash
curl -X POST http://localhost:8080/api/simulate/shipment-failure
```

### 4.5 Simulate Warehouse Capacity Pipeline Failure
Simulates fulfillment blocker due to maxed warehouse storage.

```bash
curl -X POST http://localhost:8080/api/simulate/order-processing-failure
```

### 4.6 Simulate Cascade Memory Pressure & Timeouts
Simulates critical JVM memory pressure and request timeouts.

```bash
curl -X POST http://localhost:8080/api/simulate/cascade-failure
```

---

## 5. Data Models & Enums

### 5.1 Product Status (`Product.ProductStatus`)
- `ACTIVE`
- `LOW_STOCK`
- `OUT_OF_STOCK`
- `DISCONTINUED`

### 5.2 Order Type (`Order.OrderType`)
- `PURCHASE`
- `SHIPMENT`
- `RETURN`
- `TRANSFER`

### 5.3 Order Status (`Order.OrderStatus`)
- `PENDING`
- `CONFIRMED`
- `PROCESSING`
- `SHIPPED`
- `DELIVERED`
- `CANCELLED`
- `FAILED`

---

## 6. Automated Smoke Test Script

Copy and execute the command below in your terminal to test the key endpoints sequentially:

```bash
echo "=== 1. Health Check ==="
curl -s http://localhost:8080/actuator/health

echo -e "\n\n=== 2. List All Products ==="
curl -s http://localhost:8080/api/products | head -c 250

echo -e "\n\n=== 3. Low Stock Products ==="
curl -s http://localhost:8080/api/products/low-stock | head -c 250

echo -e "\n\n=== 4. List All Orders ==="
curl -s http://localhost:8080/api/orders | head -c 250

echo -e "\n\n=== 5. Simulation Endpoints Info ==="
curl -s http://localhost:8080/api/simulate/health

echo -e "\n\n=== Done ==="
```
