// Reads KEY=VALUE entries from the Jenkins-provided .env file.
def loadEnvFile(String path) {
    readFile(path).split('\n').each { rawLine ->
        def line = rawLine.trim()
        def equalsIndex = line.indexOf('=')

        if (equalsIndex > 0 && !line.startsWith('#')) {
            def key = line.substring(0, equalsIndex).trim()
            def value = line.substring(equalsIndex + 1).trim()
            env."${key}" = value
        }
    }
}

pipeline {
    agent any

    environment {
        IMAGE_TAG = "${BUILD_NUMBER}"
        DOCKERHUB_CREDENTIALS = 'dockerhub-credentials'
        ENV_FILE_CREDENTIALS = 'calculator-env'
    }

    triggers {
        githubPush()
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm

                // .env is ignored by Git, so Jenkins supplies it as a Secret File credential.
                withCredentials([file(
                    credentialsId: env.ENV_FILE_CREDENTIALS,
                    variable: 'ENV_FILE'
                )]) {
                    sh 'cp "$ENV_FILE" .env'
                }

                script {
                    loadEnvFile('.env')
                }
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker build -t "$DOCKER_IMAGE:$IMAGE_TAG" -t "$DOCKER_IMAGE:latest" .'
            }
        }

        stage('Push') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: env.DOCKERHUB_CREDENTIALS,
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASSWORD'
                )]) {
                    sh '''
                        echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USER" --password-stdin
                        docker push "$DOCKER_IMAGE:$IMAGE_TAG"
                        docker push "$DOCKER_IMAGE:latest"
                    '''
                }
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    CALCULATOR_IMAGE="$DOCKER_IMAGE:$IMAGE_TAG" \
                        docker compose up -d --no-build --remove-orphans
                '''
                sh '''
                    for i in $(seq 1 12); do
                        if curl -fsS "http://localhost:$APP_PORT/actuator/health" | grep -q '"status":"UP"'; then
                            echo "Deployment is healthy"
                            curl -fsS "http://localhost:$APP_PORT/api/v1/add?a=2&b=3"
                            exit 0
                        fi
                        sleep 5
                    done

                    echo "Deployment did not become healthy"
                    docker compose ps
                    docker compose logs --tail=50 calculator-api
                    exit 1
                '''
            }
        }
    }

    post {
        always {
            sh 'docker logout || true'
        }

        success {
            mail(
                to: env.NOTIFICATION_EMAIL,
                subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: "Build and deployment succeeded.\nImage: ${env.DOCKER_IMAGE}:${env.IMAGE_TAG}\nDetails: ${env.BUILD_URL}"
            )
        }

        failure {
            mail(
                to: env.NOTIFICATION_EMAIL,
                subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: "Build or deployment failed.\nDetails: ${env.BUILD_URL}console"
            )
        }
    }
}
