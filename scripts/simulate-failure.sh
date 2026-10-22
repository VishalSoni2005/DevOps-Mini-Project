#!/usr/bin/env bash
# ============================================================
# simulate-failure.sh
# Simulates various application failures to generate error logs
# for Kibana dashboard demonstration.
# ============================================================
#
# Usage:
#   ./scripts/simulate-failure.sh [all|db|inventory|shipment|order|cascade]
#
# Prerequisites:
#   - Application running at http://localhost:8080
#   - curl installed
# ============================================================

set -euo pipefail

APP_URL="${APP_URL:-http://localhost:8080}"
BOLD='\033[1m'
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'  # No Color

print_header() {
    echo ""
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BOLD}  $1${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

print_step() {
    echo -e "${YELLOW}  ▶ $1${NC}"
}

print_success() {
    echo -e "${GREEN}  ✓ $1${NC}"
}

print_error() {
    echo -e "${RED}  ✗ $1${NC}"
}

# ──────────────────────────────────────────
# Step 1: Generate normal activity first
# ──────────────────────────────────────────
generate_normal_traffic() {
    print_header "Generating Normal Application Traffic"

    print_step "Listing all products..."
    curl -s "${APP_URL}/api/products" > /dev/null 2>&1 || true
    print_success "GET /api/products"

    print_step "Listing all orders..."
    curl -s "${APP_URL}/api/orders" > /dev/null 2>&1 || true
    print_success "GET /api/orders"

    print_step "Creating a new product..."
    curl -s -X POST "${APP_URL}/api/products" \
        -H "Content-Type: application/json" \
        -d '{
            "name": "Pressure Sensor Module",
            "sku": "SKU-SENSOR-'$RANDOM'",
            "description": "Industrial pressure sensor",
            "price": 320.00,
            "quantityInStock": 75,
            "reorderLevel": 15,
            "supplier": "SensorTech",
            "category": "Sensors"
        }' > /dev/null 2>&1 || true
    print_success "POST /api/products (new product created)"

    print_step "Creating a new order..."
    curl -s -X POST "${APP_URL}/api/orders" \
        -H "Content-Type: application/json" \
        -d '{
            "orderNumber": "PO-SIM-'$RANDOM'",
            "orderType": "PURCHASE",
            "supplier": "SensorTech",
            "destination": "Warehouse-Central",
            "totalAmount": 16000.00,
            "notes": "Simulation order for testing"
        }' > /dev/null 2>&1 || true
    print_success "POST /api/orders (new order created)"

    print_step "Checking low stock products..."
    curl -s "${APP_URL}/api/products/low-stock" > /dev/null 2>&1 || true
    print_success "GET /api/products/low-stock"

    print_step "Checking health..."
    curl -s "${APP_URL}/actuator/health" > /dev/null 2>&1 || true
    print_success "GET /actuator/health"

    echo ""
    print_success "Normal traffic generation complete."
}

# ──────────────────────────────────────────
# Step 2: Simulate failures
# ──────────────────────────────────────────
simulate_db_failure() {
    print_header "Simulating Database Connection Failure"
    print_step "Triggering database connection pool exhaustion..."

    response=$(curl -s -w "\n%{http_code}" -X POST "${APP_URL}/api/simulate/db-failure" 2>&1 || true)
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    print_error "Database failure triggered (HTTP $http_code)"
    echo -e "  Response: $body" | head -5
}

simulate_inventory_sync_failure() {
    print_header "Simulating Inventory Sync Failure"
    print_step "Triggering supplier API sync failure (AcmeCorp)..."

    response=$(curl -s -w "\n%{http_code}" -X POST "${APP_URL}/api/simulate/inventory-sync-failure" 2>&1 || true)
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    print_error "Inventory sync failure triggered (HTTP $http_code)"
    echo -e "  Response: $body" | head -5
}

simulate_shipment_failure() {
    print_header "Simulating Shipment Lost in Transit"
    print_step "Triggering shipment tracking failure..."

    response=$(curl -s -w "\n%{http_code}" -X POST "${APP_URL}/api/simulate/shipment-failure" 2>&1 || true)
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    print_error "Shipment failure triggered (HTTP $http_code)"
    echo -e "  Response: $body" | head -5
}

simulate_order_processing_failure() {
    print_header "Simulating Order Processing Pipeline Failure"
    print_step "Triggering warehouse capacity overflow..."

    response=$(curl -s -w "\n%{http_code}" -X POST "${APP_URL}/api/simulate/order-processing-failure" 2>&1 || true)
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    print_error "Order processing failure triggered (HTTP $http_code)"
    echo -e "  Response: $body" | head -5
}

simulate_cascade_failure() {
    print_header "Simulating Cascade Failure (Memory + Timeouts)"
    print_step "Triggering memory pressure and API timeouts..."

    response=$(curl -s -w "\n%{http_code}" -X POST "${APP_URL}/api/simulate/cascade-failure" 2>&1 || true)
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    print_error "Cascade failure triggered (HTTP $http_code)"
    echo -e "  Response: $body" | head -5
}

simulate_resource_not_found() {
    print_header "Simulating Resource Not Found Errors"

    print_step "Requesting non-existent product (ID: 99999)..."
    curl -s "${APP_URL}/api/products/99999" > /dev/null 2>&1 || true
    print_error "Product not found error triggered"

    print_step "Requesting non-existent order (ID: 99999)..."
    curl -s "${APP_URL}/api/orders/99999" > /dev/null 2>&1 || true
    print_error "Order not found error triggered"

    print_step "Requesting non-existent SKU..."
    curl -s "${APP_URL}/api/products/sku/INVALID-SKU-000" > /dev/null 2>&1 || true
    print_error "SKU not found error triggered"
}

# ──────────────────────────────────────────
# Main execution
# ──────────────────────────────────────────
run_all() {
    echo ""
    echo -e "${BOLD}╔═══════════════════════════════════════════════════════╗${NC}"
    echo -e "${BOLD}║  Supply Chain Failure Simulation Suite                ║${NC}"
    echo -e "${BOLD}║  Generating logs for ELK Stack analysis              ║${NC}"
    echo -e "${BOLD}╚═══════════════════════════════════════════════════════╝${NC}"

    generate_normal_traffic
    sleep 1

    simulate_resource_not_found
    sleep 1

    simulate_db_failure
    sleep 1

    simulate_inventory_sync_failure
    sleep 1

    simulate_shipment_failure
    sleep 1

    simulate_order_processing_failure
    sleep 1

    simulate_cascade_failure

    echo ""
    print_header "Simulation Complete!"
    echo -e "  ${GREEN}All failure scenarios have been triggered.${NC}"
    echo -e "  ${CYAN}Open Kibana at http://localhost:5601 to analyze the logs.${NC}"
    echo ""
    echo -e "  ${BOLD}Recommended Kibana queries:${NC}"
    echo -e "    • severity:error            — View all errors"
    echo -e "    • CRITICAL_ALERT            — View critical alerts"
    echo -e "    • event_category:ORDER      — View order events"
    echo -e "    • event_category:INVENTORY  — View inventory events"
    echo -e "    • SIMULATION_FAILURE        — View simulated failures"
    echo -e "    • CASCADE_FAILURE           — View cascade failures"
    echo ""
}

# Parse command line argument
case "${1:-all}" in
    all)         run_all ;;
    db)          simulate_db_failure ;;
    inventory)   simulate_inventory_sync_failure ;;
    shipment)    simulate_shipment_failure ;;
    order)       simulate_order_processing_failure ;;
    cascade)     simulate_cascade_failure ;;
    traffic)     generate_normal_traffic ;;
    notfound)    simulate_resource_not_found ;;
    *)
        echo "Usage: $0 [all|db|inventory|shipment|order|cascade|traffic|notfound]"
        exit 1
        ;;
esac
