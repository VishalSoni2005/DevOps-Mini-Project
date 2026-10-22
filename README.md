# Supply Chain Management — CI/CD & Centralized Logging System

> Enterprise-grade Supply Chain Management Application with a full CI/CD pipeline (GitHub → Jenkins → Maven → Docker → Kubernetes) and centralized ELK logging (Elasticsearch, Logstash, Kibana) for real-time monitoring and failure detection.

---

## 📋 Table of Contents

- [Architecture Overview](#architecture-overview)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Local Development (Docker Compose)](#local-development-docker-compose)
  - [Kubernetes Deployment](#kubernetes-deployment)
- [CI/CD Pipeline (Jenkins)](#cicd-pipeline-jenkins)
- [Application API Reference](#application-api-reference)
- [Centralized Logging (ELK Stack)](#centralized-logging-elk-stack)
  - [Log Flow Architecture](#log-flow-architecture)
  - [Logstash Pipeline](#logstash-pipeline)
  - [Kibana Dashboards](#kibana-dashboards)
- [Simulating Application Failures](#simulating-application-failures)
- [Identifying Failures Through Log Analysis](#identifying-failures-through-log-analysis)

---

## Architecture Overview

```
┌─────────────┐     ┌──────────┐     ┌───────────┐     ┌──────────┐     ┌──────────────┐
│   GitHub    │────▶│ Jenkins  │────▶│   Maven   │────▶│  Docker  │────▶│  Kubernetes  │
│ (Source)    │     │ (CI/CD)  │     │ (Build &  │     │ (Package)│     │  (Deploy)    │
│             │     │          │     │   Test)   │     │          │     │              │
└─────────────┘     └──────────┘     └───────────┘     └──────────┘     └──────┬───────┘
                                                                               │
                            ┌──────────────────────────────────────────────────┘
                            │
                            ▼
┌───────────────────────────────────────────────────────────────────────────────────────┐
│                           Kubernetes Cluster                                         │
│                                                                                      │
│  ┌─────────────────────────┐          ┌──────────────────────────────────────────┐   │
│  │  supply-chain namespace │          │  elk namespace                           │   │
│  │                         │          │                                          │   │
│  │  ┌───────────────────┐  │  logs    │  ┌──────────┐   ┌─────────────────────┐ │   │
│  │  │ supply-chain-app  │──┼─────────▶│  │ Filebeat │──▶│     Logstash        │ │   │
│  │  │ (3 replicas)      │  │          │  │(DaemonSet│   │ (Parse & Enrich)    │ │   │
│  │  └───────────────────┘  │          │  └──────────┘   └──────────┬──────────┘ │   │
│  │                         │          │                            │            │   │
│  │  ┌───────────────────┐  │          │                 ┌──────────▼──────────┐ │   │
│  │  │ LoadBalancer Svc  │  │          │                 │   Elasticsearch     │ │   │
│  │  │ :80 → :8080       │  │          │                 │ (Store & Index)     │ │   │
│  │  └───────────────────┘  │          │                 └──────────┬──────────┘ │   │
│  │                         │          │                            │            │   │
│  │  ┌───────────────────┐  │          │                 ┌──────────▼──────────┐ │   │
│  │  │ HPA (3-10 pods)   │  │          │                 │      Kibana         │ │   │
│  │  └───────────────────┘  │          │                 │ (Visualize & Alert) │ │   │
│  └─────────────────────────┘          │                 │ NodePort :30601     │ │   │
│                                       │                 └─────────────────────┘ │   │
│                                       └──────────────────────────────────────────┘   │
└───────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Technology Stack

| Layer              | Technology                       | Purpose                                    |
|--------------------|----------------------------------|--------------------------------------------|
| **Source Control**  | GitHub                           | Version control, webhook triggers          |
| **CI/CD**          | Jenkins (Declarative Pipeline)   | Automated build, test, deploy              |
| **Build & Test**   | Maven 3.9, JUnit 5, JaCoCo      | Compile, unit tests, code coverage         |
| **Application**    | Spring Boot 3.2, Java 17        | REST API for supply chain management       |
| **Containerization** | Docker (multi-stage)           | Lightweight, secure container images       |
| **Orchestration**  | Kubernetes (Deployment, HPA)     | Scalable deployment, auto-scaling          |
| **Log Shipping**   | Filebeat (DaemonSet)             | Collect logs from all K8s nodes            |
| **Log Processing** | Logstash                         | Parse, enrich, route log events            |
| **Log Storage**    | Elasticsearch                    | Index and search log data                  |
| **Visualization**  | Kibana                           | Dashboards, KQL queries, alerting          |

---

## Project Structure

```
supply-chain-management/
├── src/
│   ├── main/java/com/supplychain/
│   │   ├── SupplyChainApplication.java          # Application entry point
│   │   ├── controller/
│   │   │   ├── ProductController.java           # Product REST endpoints
│   │   │   ├── OrderController.java             # Order REST endpoints
│   │   │   └── SimulationController.java        # Failure simulation endpoints
│   │   ├── model/
│   │   │   ├── Product.java                     # Product entity
│   │   │   ├── Order.java                       # Order entity
│   │   │   └── OrderItem.java                   # Order line item entity
│   │   ├── repository/
│   │   │   ├── ProductRepository.java           # Product data access
│   │   │   └── OrderRepository.java             # Order data access
│   │   ├── service/
│   │   │   ├── ProductService.java              # Product business logic
│   │   │   └── OrderService.java                # Order business logic
│   │   └── exception/
│   │       ├── GlobalExceptionHandler.java      # Centralized error handling
│   │       ├── ResourceNotFoundException.java   # 404 exception
│   │       └── SupplyChainException.java        # Business logic exception
│   ├── main/resources/
│   │   ├── application.properties               # Spring Boot config
│   │   ├── logback-spring.xml                   # Structured JSON logging
│   │   └── data.sql                             # Seed data
│   └── test/java/com/supplychain/
│       ├── SupplyChainApplicationTests.java     # Context load test
│       ├── service/
│       │   ├── ProductServiceTest.java          # Product service tests
│       │   └── OrderServiceTest.java            # Order service tests
│       └── controller/
│           └── ProductControllerTest.java       # Controller integration tests
│
├── Dockerfile                                    # Multi-stage Docker build
├── docker-compose.yml                           # Local dev stack (App + ELK)
├── Jenkinsfile                                  # CI/CD pipeline definition
├── pom.xml                                      # Maven build configuration
│
├── k8s/                                         # Kubernetes manifests
│   ├── namespace.yaml                           # supply-chain namespace
│   ├── configmap.yaml                           # Application config
│   ├── deployment.yaml                          # App deployment (3 replicas)
│   ├── service.yaml                             # LoadBalancer + HPA
│   └── elk/                                     # ELK stack on Kubernetes
│       ├── namespace.yaml                       # elk namespace
│       ├── elasticsearch.yaml                   # ES deployment + PVC
│       ├── logstash.yaml                        # Logstash + pipeline config
│       ├── kibana.yaml                          # Kibana + NodePort service
│       └── filebeat.yaml                        # Filebeat DaemonSet + RBAC
│
├── logstash/pipeline/
│   └── logstash.conf                            # Logstash pipeline (local dev)
├── filebeat/
│   └── filebeat.yml                             # Filebeat config (local dev)
│
└── scripts/
    ├── simulate-failure.sh                      # Trigger simulated failures
    ├── setup-kibana-dashboards.sh               # Create Kibana data views
    └── deploy-elk-k8s.sh                        # Deploy ELK to K8s
```

---

## Getting Started

### Prerequisites

- **Java 17+** and **Maven 3.8+**
- **Docker** and **Docker Compose**
- **kubectl** (for Kubernetes deployment)
- **Jenkins** (for CI/CD pipeline)
- **Git**

### Local Development (Docker Compose)

**1. Clone and build the application:**
```bash
git clone <repository-url>
cd supply-chain-management

# Build locally (optional — Docker does this too)
mvn clean package -DskipTests
```

**2. Start the full stack:**
```bash
docker-compose up -d
```

This starts:
| Service         | URL                          |
|----------------|-------------------------------|
| Application    | http://localhost:8080          |
| Elasticsearch  | http://localhost:9200          |
| Kibana         | http://localhost:5601          |
| Logstash       | localhost:5044 (Beats input)   |

**3. Set up Kibana data views:**
```bash
chmod +x scripts/*.sh
./scripts/setup-kibana-dashboards.sh
```

**4. Simulate failures and generate logs:**
```bash
./scripts/simulate-failure.sh all
```

**5. Open Kibana** at http://localhost:5601 and navigate to **Discover** to explore logs.

### Kubernetes Deployment

**1. Deploy the ELK stack:**
```bash
./scripts/deploy-elk-k8s.sh
```

**2. Deploy the application:**
```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml -n supply-chain
kubectl apply -f k8s/deployment.yaml -n supply-chain
kubectl apply -f k8s/service.yaml -n supply-chain
```

**3. Verify deployment:**
```bash
kubectl get pods -n supply-chain
kubectl get pods -n elk
kubectl get svc -n elk
```

---

## CI/CD Pipeline (Jenkins)

### Pipeline Stages

```
┌──────────┐   ┌─────────┐   ┌──────────┐   ┌──────────────┐   ┌─────────┐
│ Checkout │──▶│  Build  │──▶│   Test   │──▶│Code Coverage│──▶│ Package │
└──────────┘   └─────────┘   └──────────┘   └──────────────┘   └────┬────┘
                                                                     │
    ┌────────────────┐   ┌─────────────┐   ┌──────────────┐         │
    │Verify Deployment│◀──│Deploy to K8s│◀──│ Docker Push │◀──── Docker Build
    └────────────────┘   └─────────────┘   └──────────────┘
```

### Jenkins Setup

1. **Install required plugins:** Pipeline, Git, Docker Pipeline, Kubernetes CLI
2. **Configure credentials:**
   - `docker-registry-credentials` — Docker Hub username/password
   - `kubeconfig-credential-id` — Kubernetes config file
3. **Create a Pipeline job** pointing to the `Jenkinsfile` in this repository
4. **Configure GitHub webhook** to trigger builds on push

### Pipeline Features

- ✅ Automatic Maven build and compilation
- ✅ Unit & integration test execution with JUnit reports
- ✅ JaCoCo code coverage reports
- ✅ Multi-stage Docker image build
- ✅ Docker image push with build-number + git-hash tags
- ✅ Kubernetes rolling deployment with zero downtime
- ✅ Post-deployment health verification

---

## Application API Reference

### Products

| Method | Endpoint                      | Description                |
|--------|-------------------------------|----------------------------|
| GET    | `/api/products`               | List all products          |
| GET    | `/api/products/{id}`          | Get product by ID          |
| GET    | `/api/products/sku/{sku}`     | Get product by SKU         |
| POST   | `/api/products`               | Create new product         |
| PUT    | `/api/products/{id}`          | Update product             |
| DELETE | `/api/products/{id}`          | Delete product             |
| GET    | `/api/products/low-stock`     | List low-stock products    |

### Orders

| Method | Endpoint                          | Description              |
|--------|-----------------------------------|--------------------------|
| GET    | `/api/orders`                     | List all orders          |
| GET    | `/api/orders/{id}`                | Get order by ID          |
| GET    | `/api/orders/number/{number}`     | Get order by number      |
| POST   | `/api/orders`                     | Create new order         |
| PATCH  | `/api/orders/{id}/status`         | Update order status      |
| DELETE | `/api/orders/{id}`                | Delete order             |
| GET    | `/api/orders/status/{status}`     | Filter by status         |

### Simulation (Failure Injection)

| Method | Endpoint                                  | Description                         |
|--------|-------------------------------------------|-------------------------------------|
| POST   | `/api/simulate/db-failure`                | Database connection pool exhaustion |
| POST   | `/api/simulate/inventory-sync-failure`    | Supplier API sync failure           |
| POST   | `/api/simulate/shipment-failure`          | Shipment lost in transit            |
| POST   | `/api/simulate/order-processing-failure`  | Warehouse capacity exceeded         |
| POST   | `/api/simulate/cascade-failure`           | Memory pressure + API timeouts      |
| GET    | `/api/simulate/health`                    | Simulation endpoint health check    |

---

## Centralized Logging (ELK Stack)

### Log Flow Architecture

```
Application (Spring Boot)
    │
    ├── logback-spring.xml ──▶ JSON logs ──▶ /var/log/supply-chain/application.json
    │                                        /var/log/supply-chain/error.json
    │
    ▼
Filebeat (DaemonSet)
    │
    ├── Collects container logs from /var/log/containers/
    ├── Collects application JSON logs from /var/log/supply-chain/
    ├── Enriches with Kubernetes metadata (pod, namespace, node)
    │
    ▼
Logstash (Pipeline)
    │
    ├── INPUT:  Beats (port 5044), TCP (port 5000), File
    ├── FILTER: JSON parsing, Grok pattern extraction,
    │           Severity classification, Domain tagging
    ├── OUTPUT: Elasticsearch (3 indices)
    │           ├── supply-chain-logs-*        (all logs)
    │           ├── supply-chain-errors-*      (errors only)
    │           └── supply-chain-deployments-* (deployment events)
    │
    ▼
Elasticsearch
    │
    ├── Indexes structured log documents
    ├── Full-text search with KQL
    │
    ▼
Kibana (Port 5601 / NodePort 30601)
    │
    ├── Data Views (Index Patterns)
    ├── Discover (Log Exploration)
    └── Dashboards (Visualizations)
```

### Logstash Pipeline

The pipeline extracts structured fields from application logs:

| Extracted Field     | Description                           | Example Values                    |
|--------------------|---------------------------------------|-----------------------------------|
| `event_category`   | Domain category of the event          | ORDER, INVENTORY, SHIPMENT, SYNC  |
| `event_type`       | Type of event within the category     | EVENT, ERROR, ALERT, CRITICAL     |
| `action`           | Specific action that occurred         | CREATE_ORDER, PRODUCT_CREATED     |
| `severity`         | Computed severity level               | info, warning, error              |
| `domain`           | Business domain classification        | orders, inventory, shipments      |
| `component`        | System component (for alerts)         | DATABASE, JVM, ORDER_PROCESSING   |

### Kibana Dashboards

After running `setup-kibana-dashboards.sh`, create these dashboards:

#### Dashboard 1: Error Analysis
- **Pie Chart:** Errors grouped by `event_category`
- **Line Chart:** Error count over time (per 5-minute intervals)
- **Data Table:** Recent `CRITICAL_ALERT` messages with timestamps
- **Filter:** `severity:error`

#### Dashboard 2: Application Events
- **Bar Chart:** Events grouped by `domain`
- **Metric:** Total event count in time range
- **Tag Cloud:** Most frequent `action` values
- **Timeline:** Order status transitions

#### Dashboard 3: Deployment Activity
- **Table:** Deployment events with status
- **Timeline:** Health check results
- **Metric:** Pod restart count

#### Dashboard 4: System Health
- **Gauge:** Error rate (errors / total logs × 100)
- **Line Chart:** Log volume per minute
- **Heatmap:** Error distribution by hour and day

---

## Simulating Application Failures

The project includes a simulation suite that generates realistic supply chain failures:

```bash
# Run all simulations
./scripts/simulate-failure.sh all

# Run specific simulations
./scripts/simulate-failure.sh db           # Database failure
./scripts/simulate-failure.sh inventory    # Inventory sync failure
./scripts/simulate-failure.sh shipment     # Shipment lost
./scripts/simulate-failure.sh order        # Order processing failure
./scripts/simulate-failure.sh cascade      # Cascade failure
./scripts/simulate-failure.sh traffic      # Normal traffic only
./scripts/simulate-failure.sh notfound     # Resource not found errors
```

---

## Identifying Failures Through Log Analysis

### Demo: Simulated Cascade Failure Detection

This section demonstrates how to identify an application failure using the ELK stack.

**Step 1: Trigger the failure**
```bash
./scripts/simulate-failure.sh cascade
```

**Step 2: Open Kibana Discover** (http://localhost:5601 → Discover)

**Step 3: Search for critical alerts:**
```
CRITICAL_ALERT AND CASCADE_FAILURE
```

**Step 4: Analyze the log sequence:**

The cascade failure produces this sequence of log entries:

```
TIMESTAMP              LEVEL   MESSAGE
─────────────────────  ─────   ───────────────────────────────────────────────
2026-10-22 23:15:01    WARN    SYSTEM_ALERT: component=JVM, action=MEMORY_PRESSURE_DETECTED,
                                heap_used_pct=92, heap_max_mb=2048, gc_pause_ms=1500

2026-10-22 23:15:01    ERROR   SYSTEM_ALERT: component=JVM, action=MEMORY_CRITICAL,
                                heap_used_pct=98, gc_overhead_pct=45

2026-10-22 23:15:01    ERROR   API_TIMEOUT: endpoint=/api/orders, method=GET,
                                response_time_ms=30000, timeout_limit_ms=5000,
                                concurrent_requests=250

2026-10-22 23:15:01    ERROR   API_TIMEOUT: endpoint=/api/products, method=GET,
                                response_time_ms=28500, concurrent_requests=180

2026-10-22 23:15:01    ERROR   CRITICAL_ALERT: component=APPLICATION, severity=CRITICAL,
                                action=CASCADE_FAILURE,
                                impact=Multiple API endpoints timing out due to memory pressure,
                                recommended_action=SCALE_UP_OR_RESTART
```

**Step 5: Identify root cause in Kibana:**

Using KQL queries:
```
# Find the initial trigger
message:MEMORY_PRESSURE_DETECTED

# Find all cascade effects
message:API_TIMEOUT

# Get the critical alert with recommended action
message:CASCADE_FAILURE AND message:recommended_action
```

**Conclusion:** The log analysis reveals that:
1. **Root Cause:** JVM memory pressure (92% → 98% heap usage)
2. **Effect:** GC overhead increased to 45%, causing long pauses
3. **Impact:** API endpoints `/api/orders` and `/api/products` started timing out
4. **Resolution:** The system recommends scaling up or restarting the application

This demonstrates how centralized logging with ELK enables rapid root-cause analysis of production failures.

---

## License

This project is for educational and demonstration purposes.
