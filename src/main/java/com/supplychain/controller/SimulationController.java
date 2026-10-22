package com.supplychain.controller;

import com.supplychain.exception.SupplyChainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Controller that provides endpoints to simulate various application failures.
 * Used for demonstrating log analysis and error detection in Kibana dashboards.
 */
@RestController
@RequestMapping("/api/simulate")
public class SimulationController {

    private static final Logger logger = LoggerFactory.getLogger(SimulationController.class);
    private final Random random = new Random();

    /**
     * Simulates a database connection failure.
     */
    @PostMapping("/db-failure")
    public ResponseEntity<Map<String, Object>> simulateDatabaseFailure() {
        logger.error("SIMULATION_FAILURE: type=DATABASE_CONNECTION_FAILURE, " +
                "message=Unable to acquire database connection from pool, " +
                "host=db-primary.supplychain.internal, port=5432, " +
                "pool_active=50, pool_max=50, wait_timeout_ms=30000");

        logger.error("CRITICAL_ALERT: component=DATABASE, severity=CRITICAL, " +
                "action=CONNECTION_POOL_EXHAUSTED, " +
                "impact=All database operations are failing");

        throw new SupplyChainException(
                "Database connection pool exhausted - unable to process requests",
                "DB_CONNECTION_FAILURE");
    }

    /**
     * Simulates an inventory sync failure with an external supplier system.
     */
    @PostMapping("/inventory-sync-failure")
    public ResponseEntity<Map<String, Object>> simulateInventorySyncFailure() {
        logger.warn("SYNC_EVENT: action=INVENTORY_SYNC_STARTED, source=SUPPLIER_API, supplier=AcmeCorp");

        logger.error("SYNC_FAILURE: action=INVENTORY_SYNC_FAILED, source=SUPPLIER_API, " +
                "supplier=AcmeCorp, error=HTTP_503_SERVICE_UNAVAILABLE, " +
                "endpoint=https://api.acmecorp.com/v2/inventory, " +
                "retries_attempted=3, retry_interval_ms=5000");

        logger.error("CRITICAL_ALERT: component=INVENTORY_SYNC, severity=HIGH, " +
                "action=SYNC_FAILED, supplier=AcmeCorp, " +
                "impact=Inventory data may be stale for 847 products, " +
                "last_successful_sync=2026-10-22T18:30:00Z");

        throw new SupplyChainException(
                "Inventory synchronization with supplier AcmeCorp failed after 3 retries",
                "INVENTORY_SYNC_FAILURE");
    }

    /**
     * Simulates a shipment tracking failure.
     */
    @PostMapping("/shipment-failure")
    public ResponseEntity<Map<String, Object>> simulateShipmentFailure() {
        String trackingId = "SHIP-" + (10000 + random.nextInt(90000));

        logger.info("SHIPMENT_EVENT: action=TRACKING_UPDATE_RECEIVED, trackingId={}", trackingId);

        logger.error("SHIPMENT_FAILURE: action=SHIPMENT_LOST, trackingId={}, " +
                "carrier=GlobalFreight, origin=Shanghai, destination=Los_Angeles, " +
                "last_known_location=Pacific_Ocean_Transit, " +
                "estimated_value_usd=145000.00, containers=3", trackingId);

        logger.error("CRITICAL_ALERT: component=SHIPMENT_TRACKING, severity=CRITICAL, " +
                "action=SHIPMENT_LOST, trackingId={}, " +
                "impact=3 containers with estimated value $145,000 are unaccounted for, " +
                "affected_orders=PO-2024-001,PO-2024-002,PO-2024-003", trackingId);

        throw new SupplyChainException(
                "Shipment " + trackingId + " has been lost in transit",
                "SHIPMENT_LOST");
    }

    /**
     * Simulates an order processing pipeline failure.
     */
    @PostMapping("/order-processing-failure")
    public ResponseEntity<Map<String, Object>> simulateOrderProcessingFailure() {
        String orderNumber = "ORD-" + (100000 + random.nextInt(900000));

        logger.info("ORDER_PIPELINE: action=ORDER_RECEIVED, orderNumber={}, stage=INTAKE", orderNumber);
        logger.info("ORDER_PIPELINE: action=VALIDATION_PASSED, orderNumber={}, stage=VALIDATION", orderNumber);
        logger.info("ORDER_PIPELINE: action=PAYMENT_VERIFIED, orderNumber={}, stage=PAYMENT", orderNumber);

        logger.error("ORDER_PIPELINE: action=FULFILLMENT_FAILED, orderNumber={}, stage=FULFILLMENT, " +
                "error=WAREHOUSE_CAPACITY_EXCEEDED, " +
                "warehouse=WH-EAST-01, current_capacity_pct=100, " +
                "requested_allocation=500_units, available_space=0_units", orderNumber);

        logger.error("CRITICAL_ALERT: component=ORDER_PROCESSING, severity=HIGH, " +
                "action=PIPELINE_FAILURE, orderNumber={}, stage=FULFILLMENT, " +
                "impact=Order cannot be fulfilled - warehouse at maximum capacity, " +
                "fallback_action=ROUTE_TO_ALTERNATE_WAREHOUSE", orderNumber);

        throw new SupplyChainException(
                "Order " + orderNumber + " fulfillment failed: warehouse capacity exceeded",
                "FULFILLMENT_FAILURE");
    }

    /**
     * Simulates a cascade of errors (memory pressure + timeouts).
     */
    @PostMapping("/cascade-failure")
    public ResponseEntity<Map<String, Object>> simulateCascadeFailure() {
        logger.warn("SYSTEM_ALERT: component=JVM, action=MEMORY_PRESSURE_DETECTED, " +
                "heap_used_pct=92, heap_max_mb=2048, gc_pause_ms=1500");

        logger.error("SYSTEM_ALERT: component=JVM, action=MEMORY_CRITICAL, " +
                "heap_used_pct=98, heap_max_mb=2048, gc_overhead_pct=45");

        logger.error("API_TIMEOUT: endpoint=/api/orders, method=GET, " +
                "response_time_ms=30000, timeout_limit_ms=5000, " +
                "concurrent_requests=250");

        logger.error("API_TIMEOUT: endpoint=/api/products, method=GET, " +
                "response_time_ms=28500, timeout_limit_ms=5000, " +
                "concurrent_requests=180");

        logger.error("CRITICAL_ALERT: component=APPLICATION, severity=CRITICAL, " +
                "action=CASCADE_FAILURE, " +
                "impact=Multiple API endpoints timing out due to memory pressure, " +
                "affected_endpoints=/api/orders,/api/products,/api/shipments, " +
                "recommended_action=SCALE_UP_OR_RESTART");

        throw new SupplyChainException(
                "Cascade failure: application under severe memory pressure causing API timeouts",
                "CASCADE_FAILURE");
    }

    /**
     * Health check endpoint that returns current system status.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSimulationHealth() {
        logger.info("HEALTH_CHECK: action=STATUS_REQUESTED");

        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("application", "supply-chain-management");
        health.put("version", "1.0.0");
        health.put("simulation_endpoints", new String[]{
                "/api/simulate/db-failure",
                "/api/simulate/inventory-sync-failure",
                "/api/simulate/shipment-failure",
                "/api/simulate/order-processing-failure",
                "/api/simulate/cascade-failure"
        });

        return ResponseEntity.ok(health);
    }
}
