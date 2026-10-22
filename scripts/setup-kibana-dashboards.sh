#!/usr/bin/env bash
# ============================================================
# setup-kibana-dashboards.sh
# Configures Kibana index patterns, visualizations, and
# dashboards for Supply Chain log analysis.
# ============================================================
#
# Usage:
#   ./scripts/setup-kibana-dashboards.sh
#
# Prerequisites:
#   - Kibana running at http://localhost:5601
#   - Elasticsearch running at http://localhost:9200
#   - Logs already indexed (run simulate-failure.sh first)
# ============================================================

set -euo pipefail

KIBANA_URL="${KIBANA_URL:-http://localhost:5601}"
ES_URL="${ES_URL:-http://localhost:9200}"

BOLD='\033[1m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo ""
echo -e "${BOLD}╔═══════════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}║  Kibana Dashboard Setup for Supply Chain Monitoring  ║${NC}"
echo -e "${BOLD}╚═══════════════════════════════════════════════════════╝${NC}"
echo ""

# ──────────────────────────────────────────
# Wait for Kibana to be ready
# ──────────────────────────────────────────
echo -e "${YELLOW}⏳ Waiting for Kibana to be ready...${NC}"
until curl -s "${KIBANA_URL}/api/status" | grep -q '"level":"available"' 2>/dev/null; do
    sleep 5
    echo -e "  Kibana not ready yet, retrying..."
done
echo -e "${GREEN}✓ Kibana is ready!${NC}"
echo ""

# ──────────────────────────────────────────
# Create Data View: All Logs
# ──────────────────────────────────────────
echo -e "${CYAN}📊 Creating Data View: supply-chain-logs-*${NC}"
curl -s -X POST "${KIBANA_URL}/api/data_views/data_view" \
    -H "kbn-xsrf: true" \
    -H "Content-Type: application/json" \
    -d '{
        "data_view": {
            "title": "supply-chain-logs-*",
            "name": "Supply Chain - All Logs",
            "timeFieldName": "@timestamp"
        }
    }' > /dev/null 2>&1 || echo "  (Data view may already exist)"
echo -e "${GREEN}✓ Data View created: supply-chain-logs-*${NC}"

# ──────────────────────────────────────────
# Create Data View: Errors Only
# ──────────────────────────────────────────
echo -e "${CYAN}📊 Creating Data View: supply-chain-errors-*${NC}"
curl -s -X POST "${KIBANA_URL}/api/data_views/data_view" \
    -H "kbn-xsrf: true" \
    -H "Content-Type: application/json" \
    -d '{
        "data_view": {
            "title": "supply-chain-errors-*",
            "name": "Supply Chain - Errors Only",
            "timeFieldName": "@timestamp"
        }
    }' > /dev/null 2>&1 || echo "  (Data view may already exist)"
echo -e "${GREEN}✓ Data View created: supply-chain-errors-*${NC}"

# ──────────────────────────────────────────
# Create Data View: Deployments
# ──────────────────────────────────────────
echo -e "${CYAN}📊 Creating Data View: supply-chain-deployments-*${NC}"
curl -s -X POST "${KIBANA_URL}/api/data_views/data_view" \
    -H "kbn-xsrf: true" \
    -H "Content-Type: application/json" \
    -d '{
        "data_view": {
            "title": "supply-chain-deployments-*",
            "name": "Supply Chain - Deployments",
            "timeFieldName": "@timestamp"
        }
    }' > /dev/null 2>&1 || echo "  (Data view may already exist)"
echo -e "${GREEN}✓ Data View created: supply-chain-deployments-*${NC}"

echo ""
echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}${BOLD}  ✓ Kibana Setup Complete!${NC}"
echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "  ${BOLD}Access Kibana:${NC} ${CYAN}${KIBANA_URL}${NC}"
echo ""
echo -e "  ${BOLD}Recommended Dashboard Setup (Manual in Kibana):${NC}"
echo ""
echo -e "  ${YELLOW}1. Error Analysis Dashboard:${NC}"
echo -e "     • Visualization: Pie chart of errors by 'event_category'"
echo -e "     • Visualization: Line chart of error count over time"
echo -e "     • Visualization: Data table of recent CRITICAL_ALERT messages"
echo -e "     • Filter: severity:error"
echo ""
echo -e "  ${YELLOW}2. Application Events Dashboard:${NC}"
echo -e "     • Visualization: Bar chart of events by 'domain'"
echo -e "     • Visualization: Metric showing total events count"
echo -e "     • Visualization: Tag cloud of 'action' field values"
echo -e "     • Visualization: Timeline of order status transitions"
echo ""
echo -e "  ${YELLOW}3. Deployment Activity Dashboard:${NC}"
echo -e "     • Visualization: Table of deployment events"
echo -e "     • Visualization: Health check status over time"
echo -e "     • Visualization: Pod restart events"
echo ""
echo -e "  ${YELLOW}4. System Health Dashboard:${NC}"
echo -e "     • Visualization: Gauge for error rate percentage"
echo -e "     • Visualization: Line chart of log volume per minute"
echo -e "     • Visualization: Heatmap of errors by hour/day"
echo ""
echo -e "  ${BOLD}Quick KQL Queries for Discover:${NC}"
echo -e "     • ${CYAN}severity:error${NC}                    — All errors"
echo -e "     • ${CYAN}message:CRITICAL_ALERT${NC}            — Critical alerts"
echo -e "     • ${CYAN}event_category:ORDER${NC}              — Order events"
echo -e "     • ${CYAN}event_category:INVENTORY${NC}          — Inventory events"
echo -e "     • ${CYAN}domain:simulation${NC}                 — Simulated failures"
echo -e "     • ${CYAN}message:CASCADE_FAILURE${NC}           — Cascade failures"
echo -e "     • ${CYAN}action:ORDER_FAILED${NC}               — Failed orders"
echo -e "     • ${CYAN}message:SHIPMENT_LOST${NC}             — Lost shipments"
echo ""
