pipeline {
    agent any

    environment {
        GIT_CREDENTIALS_ID = 'github'
        DOCKERHUB_USERNAME = 'samdroberts'
        DOCKER_IMAGE_NAME = "${DOCKERHUB_USERNAME}/visa-back"
        MAJOR_VERSION = '1.'
        IMAGE_TAG = "${BUILD_NUMBER}-test"
        CONTAINER_NAME = 'java-app-container'
        HEALTHCHECK_TIMEOUT = '120'
        REPO = "https://github.com/mustaqmstk/Visa-Holder-Movement-Tracker.git"
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'back-end', credentialsId: "${GIT_CREDENTIALS_ID}", url: "${REPO}"
            }
        }

        stage('Build with Maven') {
            steps {
                dir('back-end') {
                    sh 'mvn clean package'
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    dir('back-end') {
                        docker.build("${DOCKER_IMAGE_NAME}:${MAJOR_VERSION}${IMAGE_TAG}")
                    }
                }
            }
        }

        stage('Trivy Scan') {
            steps {
                script {
                    def status = sh(
                        script: "trivy image --severity CRITICAL --exit-code 1 --quiet ${DOCKER_IMAGE_NAME}:${MAJOR_VERSION}${IMAGE_TAG}",
                        returnStatus: true
                    )
                    if (status != 0) {
                        error("Trivy scan failed: critical vulnerabilities found.")
                    }
                }
            }
        }


        stage('Run Container and db') {
            steps {
                dir('healthcheck-script') {
                    git branch: 'devops', credentialsId: "${GIT_CREDENTIALS_ID}", url: "${REPO}"
                }
                sh '''
                    cd healthcheck-script/devops/jenkins/portable_db
                    docker-compose up -d
                    sleep 10
                    '''
            }
        }

        stage('Health Check') {
            steps {
                script {
                    def status = sh(
                        script: "./healthcheck-script/devops/scripts/healthcheck.sh ${CONTAINER_NAME} ${HEALTHCHECK_TIMEOUT}",
                        returnStatus: true
                    )
                    if (status != 0) {
                        error("Health check failed with exit code ${status}")
                    }
                }
            }
        }
    

        stage('Push Docker Image back-end') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKERHUB_USER', passwordVariable: 'DOCKERHUB_PASS')]) {
                    script {
                        sh """
                            echo "$DOCKERHUB_PASS" | docker login -u "$DOCKERHUB_USER" --password-stdin
                        docker push ${DOCKER_IMAGE_NAME}:${MAJOR_VERSION}${IMAGE_TAG}
                        """
                    }
                }
            }
        }
    }

    post {
        always {
                sh '''
                    cd healthcheck-script/devops/jenkins/portable_db
                    docker-compose down
                    '''
        }
    }

}
