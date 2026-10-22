// ============================================================
// Jenkinsfile — CI/CD Pipeline for Supply Chain Management
// ============================================================
// Pipeline stages:
//   1. Checkout      → Pull source from GitHub
//   2. Build         → Compile with Maven
//   3. Test          → Run unit & integration tests
//   4. Code Coverage → Generate JaCoCo reports
//   5. Package       → Build JAR artifact
//   6. Docker Build  → Build container image
//   7. Docker Push   → Push to container registry
//   8. Deploy to K8s → Apply Kubernetes manifests
//   9. Verify        → Health check the deployment
// ============================================================

pipeline {
    agent any

    environment {
        // Docker registry settings
        DOCKER_REGISTRY   = 'docker.io'
        DOCKER_REPO       = 'supplychain'
        IMAGE_NAME        = 'supply-chain-management'
        IMAGE_TAG         = "${env.BUILD_NUMBER}-${env.GIT_COMMIT?.take(7) ?: 'latest'}"

        // Kubernetes
        K8S_NAMESPACE     = 'supply-chain'
        KUBECONFIG_CRED   = 'kubeconfig-credential-id'

        // Maven
        MAVEN_OPTS        = '-Xmx1024m'
    }

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {

        // ────────────────────────────────────────
        // Stage 1: Checkout Source Code
        // ────────────────────────────────────────
        stage('Checkout') {
            steps {
                echo '📥 Checking out source code from GitHub...'
                checkout scm
                sh 'git log --oneline -5'
            }
        }

        // ────────────────────────────────────────
        // Stage 2: Build with Maven
        // ────────────────────────────────────────
        stage('Build') {
            steps {
                echo '🔨 Building application with Maven...'
                sh 'mvn clean compile -B -DskipTests'
            }
        }

        // ────────────────────────────────────────
        // Stage 3: Run Tests
        // ────────────────────────────────────────
        stage('Test') {
            steps {
                echo '🧪 Running unit and integration tests...'
                sh 'mvn test -B'
            }
            post {
                always {
                    // Publish test results
                    junit allowEmptyResults: true,
                         testResults: '**/target/surefire-reports/*.xml'
                }
                failure {
                    echo '❌ Tests failed! Check the test reports.'
                }
            }
        }

        // ────────────────────────────────────────
        // Stage 4: Code Coverage Report
        // ────────────────────────────────────────
        stage('Code Coverage') {
            steps {
                echo '📊 Generating code coverage report...'
                sh 'mvn jacoco:report -B'
            }
            post {
                always {
                    publishHTML(target: [
                        allowMissing: true,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'target/site/jacoco',
                        reportFiles: 'index.html',
                        reportName: 'JaCoCo Code Coverage'
                    ])
                }
            }
        }

        // ────────────────────────────────────────
        // Stage 5: Package Application
        // ────────────────────────────────────────
        stage('Package') {
            steps {
                echo '📦 Packaging application JAR...'
                sh 'mvn package -B -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        // ────────────────────────────────────────
        // Stage 6: Docker Build
        // ────────────────────────────────────────
        stage('Docker Build') {
            steps {
                echo "🐳 Building Docker image: ${IMAGE_NAME}:${IMAGE_TAG}"
                sh """
                    docker build \
                        -t ${DOCKER_REGISTRY}/${DOCKER_REPO}/${IMAGE_NAME}:${IMAGE_TAG} \
                        -t ${DOCKER_REGISTRY}/${DOCKER_REPO}/${IMAGE_NAME}:latest \
                        --label build-number=${env.BUILD_NUMBER} \
                        --label git-commit=${env.GIT_COMMIT ?: 'unknown'} \
                        .
                """
            }
        }

        // ────────────────────────────────────────
        // Stage 7: Docker Push
        // ────────────────────────────────────────
        stage('Docker Push') {
            steps {
                echo '📤 Pushing Docker image to registry...'
                withCredentials([usernamePassword(
                    credentialsId: 'docker-registry-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh """
                        echo \$DOCKER_PASS | docker login ${DOCKER_REGISTRY} -u \$DOCKER_USER --password-stdin
                        docker push ${DOCKER_REGISTRY}/${DOCKER_REPO}/${IMAGE_NAME}:${IMAGE_TAG}
                        docker push ${DOCKER_REGISTRY}/${DOCKER_REPO}/${IMAGE_NAME}:latest
                        docker logout ${DOCKER_REGISTRY}
                    """
                }
            }
        }

        // ────────────────────────────────────────
        // Stage 8: Deploy to Kubernetes
        // ────────────────────────────────────────
        stage('Deploy to Kubernetes') {
            steps {
                echo '☸️  Deploying to Kubernetes cluster...'
                withCredentials([file(credentialsId: "${KUBECONFIG_CRED}", variable: 'KUBECONFIG')]) {
                    sh """
                        # Create namespace if not exists
                        kubectl apply -f k8s/namespace.yaml

                        # Apply ConfigMap and secrets
                        kubectl apply -f k8s/configmap.yaml -n ${K8S_NAMESPACE}

                        # Update image tag in deployment
                        sed -i 's|IMAGE_PLACEHOLDER|${DOCKER_REGISTRY}/${DOCKER_REPO}/${IMAGE_NAME}:${IMAGE_TAG}|g' k8s/deployment.yaml

                        # Apply deployment and service
                        kubectl apply -f k8s/deployment.yaml -n ${K8S_NAMESPACE}
                        kubectl apply -f k8s/service.yaml -n ${K8S_NAMESPACE}

                        # Wait for rollout
                        kubectl rollout status deployment/supply-chain-app -n ${K8S_NAMESPACE} --timeout=120s
                    """
                }
            }
        }

        // ────────────────────────────────────────
        // Stage 9: Verify Deployment
        // ────────────────────────────────────────
        stage('Verify Deployment') {
            steps {
                echo '✅ Verifying deployment health...'
                withCredentials([file(credentialsId: "${KUBECONFIG_CRED}", variable: 'KUBECONFIG')]) {
                    sh """
                        echo "--- Pod Status ---"
                        kubectl get pods -n ${K8S_NAMESPACE} -l app=supply-chain-app

                        echo "--- Service Status ---"
                        kubectl get svc -n ${K8S_NAMESPACE}

                        echo "--- Recent Events ---"
                        kubectl get events -n ${K8S_NAMESPACE} --sort-by='.lastTimestamp' | tail -10
                    """
                }
            }
        }
    }

    // ============================================================
    // Post-Pipeline Actions
    // ============================================================
    post {
        success {
            echo """
            ✅ ============================================
               PIPELINE COMPLETED SUCCESSFULLY
               Image: ${DOCKER_REGISTRY}/${DOCKER_REPO}/${IMAGE_NAME}:${IMAGE_TAG}
               Deployed to: ${K8S_NAMESPACE}
            ============================================
            """
        }
        failure {
            echo """
            ❌ ============================================
               PIPELINE FAILED
               Check the logs above for details.
            ============================================
            """
        }
        always {
            // Clean up Docker images on build agent
            sh "docker rmi ${DOCKER_REGISTRY}/${DOCKER_REPO}/${IMAGE_NAME}:${IMAGE_TAG} || true"
            cleanWs()
        }
    }
}
