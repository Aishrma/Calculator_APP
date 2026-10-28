// =============================================================================
// Declarative CI/CD pipeline for the Calculator REST API
//   Checkout -> Build -> Test -> Docker Build -> Push -> Deploy (+ health check)
// Triggered automatically by a GitHub push webhook.
// =============================================================================
pipeline {
    agent any

    triggers {
        // Fired by the GitHub webhook (http://<jenkins>/github-webhook/).
        // Fallback if no webhook is possible:  pollSCM('H/2 * * * *')
        githubPush()
    }

    options {
        skipDefaultCheckout(true)          // checkout happens in our own stage
        disableConcurrentBuilds()          // two deploys never race each other
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }

    environment {
        // ---- change these two values if yours are different ----
        DOCKERHUB_USER = 'aishdocked'                         // Docker Hub username (not a secret)
        NOTIFY_EMAIL   = 'aishsharma1102@gmail.com'    // build notifications go here

        IMAGE_NAME = "${DOCKERHUB_USER}/calculator-api"
        IMAGE_TAG  = "${BUILD_NUMBER}"
        HTTP_PORT  = '8081'                                // host port of the nginx proxy
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log -1 --pretty=format:"%h - %an: %s"'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/calculator-api.jar', fingerprint: true
                }
            }
        }

        stage('Test') {
            steps {
                // A failing test makes `mvn` exit non-zero -> stage and build fail
                sh 'mvn -B test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: false
                    recordCoverage(tools: [[parser: 'JACOCO', pattern: 'target/site/jacoco/jacoco.xml']],
                                   sourceCodeRetention: 'EVERY_BUILD')
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    docker build \
                        -t "$IMAGE_NAME:$IMAGE_TAG" \
                        -t "$IMAGE_NAME:latest" \
                        .
                    docker image ls "$IMAGE_NAME"
                '''
            }
        }

        stage('Push') {
            steps {
                // Credentials come from the Jenkins Credentials store (ID: dockerhub-creds).
                // Single quotes => Groovy never interpolates the secret; Jenkins masks it in logs.
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds',
                                                  usernameVariable: 'DH_USER',
                                                  passwordVariable: 'DH_TOKEN')]) {
                    sh '''
                        echo "$DH_TOKEN" | docker login -u "$DH_USER" --password-stdin
                        docker push "$IMAGE_NAME:$IMAGE_TAG"
                        docker push "$IMAGE_NAME:latest"
                    '''
                }
            }
            post {
                always {
                    sh 'docker logout || true'
                }
            }
        }

        stage('Deploy') {
            steps {
                // IMAGE_TAG / DOCKERHUB_USER / HTTP_PORT are read by docker-compose.yml.
                // `up -d` only recreates containers whose image/config changed, so a
                // re-run is safe; --wait blocks until every health check is healthy.
                sh '''
                    docker compose config --quiet
                    docker compose build nginx
                    docker compose up -d --no-build --remove-orphans --wait --wait-timeout 180
                    docker compose ps
                '''
            }
        }

        stage('Verify') {
            steps {
                // Smoke test through the proxy, from inside the nginx container
                sh '''
                    set -e
                    echo "--- /actuator/health via nginx ---"
                    docker compose exec -T nginx wget -qO- http://127.0.0.1/actuator/health | tee health.json
                    grep -q '"status":"UP"' health.json

                    echo; echo "--- /api/v1/add?a=2&b=3 via nginx ---"
                    docker compose exec -T nginx wget -qO- "http://127.0.0.1/api/v1/add?a=2&b=3" | tee add.json
                    grep -q '"result":5' add.json

                    echo; echo "--- /actuator/info (deployed tag) ---"
                    docker compose exec -T nginx wget -qO- http://127.0.0.1/actuator/info
                    echo; echo "Deployment of $IMAGE_NAME:$IMAGE_TAG is healthy"
                '''
            }
        }
    }

    post {
        success {
            emailext(
                to: "${env.NOTIFY_EMAIL}",
                subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                mimeType: 'text/html',
                body: """
                    <h2 style="color:green">Build SUCCESS</h2>
                    <p><b>Job:</b> ${env.JOB_NAME}<br/>
                       <b>Build:</b> #${env.BUILD_NUMBER}<br/>
                       <b>Image:</b> ${env.IMAGE_NAME}:${env.IMAGE_TAG}<br/>
                       <b>Deployed at:</b> http://localhost:${env.HTTP_PORT}/api/v1/add?a=2&amp;b=3<br/>
                       <b>Details:</b> <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>
                """
            )
        }
        failure {
            emailext(
                to: "${env.NOTIFY_EMAIL}",
                subject: "FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                mimeType: 'text/html',
                attachLog: true,
                body: """
                    <h2 style="color:red">Build FAILED</h2>
                    <p><b>Job:</b> ${env.JOB_NAME}<br/>
                       <b>Build:</b> #${env.BUILD_NUMBER}<br/>
                       <b>Console:</b> <a href="${env.BUILD_URL}console">${env.BUILD_URL}console</a></p>
                    <p>The build log is attached.</p>
                """
            )
        }
        always {
            // Remove dangling layers left by rebuilds; never touches running containers
            sh 'docker image prune -f || true'
        }
    }
}
