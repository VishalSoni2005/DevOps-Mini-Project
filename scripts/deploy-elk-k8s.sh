#!/usr/bin/env bash
# ============================================================
# deploy-elk-k8s.sh
# Deploys the full ELK stack to Kubernetes cluster.
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

BOLD='\033[1m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo ""
echo -e "${BOLD}╔═══════════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}║  Deploying ELK Stack to Kubernetes                   ║${NC}"
echo -e "${BOLD}╚═══════════════════════════════════════════════════════╝${NC}"
echo ""

# Step 1: Create ELK namespace
echo -e "${CYAN}[1/5] Creating ELK namespace...${NC}"
kubectl apply -f "${PROJECT_DIR}/k8s/elk/namespace.yaml"
echo -e "${GREEN}✓ Namespace 'elk' created${NC}"

# Step 2: Deploy Elasticsearch
echo -e "${CYAN}[2/5] Deploying Elasticsearch...${NC}"
kubectl apply -f "${PROJECT_DIR}/k8s/elk/elasticsearch.yaml"
echo -e "${YELLOW}  Waiting for Elasticsearch to be ready...${NC}"
kubectl rollout status deployment/elasticsearch -n elk --timeout=120s
echo -e "${GREEN}✓ Elasticsearch deployed${NC}"

# Step 3: Deploy Logstash
echo -e "${CYAN}[3/5] Deploying Logstash...${NC}"
kubectl apply -f "${PROJECT_DIR}/k8s/elk/logstash.yaml"
echo -e "${YELLOW}  Waiting for Logstash to be ready...${NC}"
kubectl rollout status deployment/logstash -n elk --timeout=120s
echo -e "${GREEN}✓ Logstash deployed${NC}"

# Step 4: Deploy Kibana
echo -e "${CYAN}[4/5] Deploying Kibana...${NC}"
kubectl apply -f "${PROJECT_DIR}/k8s/elk/kibana.yaml"
echo -e "${YELLOW}  Waiting for Kibana to be ready...${NC}"
kubectl rollout status deployment/kibana -n elk --timeout=180s
echo -e "${GREEN}✓ Kibana deployed${NC}"

# Step 5: Deploy Filebeat
echo -e "${CYAN}[5/5] Deploying Filebeat DaemonSet...${NC}"
kubectl apply -f "${PROJECT_DIR}/k8s/elk/filebeat.yaml"
echo -e "${GREEN}✓ Filebeat DaemonSet deployed${NC}"

echo ""
echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}${BOLD}  ✓ ELK Stack Deployment Complete!${NC}"
echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Display access info
KIBANA_PORT=$(kubectl get svc kibana-service -n elk -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "30601")
echo -e "  ${BOLD}Kibana URL:${NC}          http://<node-ip>:${KIBANA_PORT}"
echo -e "  ${BOLD}Elasticsearch URL:${NC}   (cluster-internal) http://elasticsearch-service.elk.svc.cluster.local:9200"
echo -e "  ${BOLD}Logstash Beats:${NC}      (cluster-internal) logstash-service.elk.svc.cluster.local:5044"
echo ""

echo -e "  ${BOLD}Verify:${NC}"
echo -e "    kubectl get pods -n elk"
echo -e "    kubectl get svc -n elk"
echo ""
