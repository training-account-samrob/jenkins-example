pipeline {
    agent any

    environment {
        GIT_CREDENTIALS_ID = 'github'
        DOCKERHUB_USERNAME = 'samdroberts'
        DOCKER_IMAGE_NAME = "${DOCKERHUB_USERNAME}/visa-front"
        MAJOR_VERSION = '1.'
        IMAGE_TAG = "${BUILD_NUMBER}-test"
        CONTAINER_NAME = 'react-app-container'
        HEALTHCHECK_TIMEOUT = '120'
        REPO = "https://github.com/mustaqmstk/Visa-Holder-Movement-Tracker.git"
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'front-end', credentialsId: "${GIT_CREDENTIALS_ID}", url: "${REPO}"
            }
        }
        stage('npm install') {
            steps {
                dir('front-end') {
                    sh 'npm ci'
                }
            }
        }

        // stage('lint') {
        //     steps {
        //         dir('front-end') {
        //             sh 'npm run lint'
        //         }
        //     }
        // }


        // stage('Run Unit Tests') {
        //     steps {
        //         dir('front-end') {                
        //             sh 'npm test'
        //         }
        //     }
        // }


        // stage('Build Docker Image') {
        //     steps {
        //         script {
        //             dir('front-end') {
        //                 docker.build("${DOCKER_IMAGE_NAME}:${MAJOR_VERSION}${IMAGE_TAG}")
        //             }
        //         }
        //     }
        // }

        stage('build dev image') {
            steps {
                dir('front-end') {
                    sh "docker build -f Dockerfile.dev -t ${DOCKER_IMAGE_NAME}:${MAJOR_VERSION}${IMAGE_TAG}dev ."
                }
            }
        }

        stage('build prod image') {
            steps {
                dir('front-end') {
                    sh "docker build -f Dockerfile.prod -t ${DOCKER_IMAGE_NAME}:${MAJOR_VERSION}${IMAGE_TAG} ."
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

        stage('Run Container') {
            steps {
                sh '''
                    docker run -d -p 80:80 --name ${CONTAINER_NAME} ${DOCKER_IMAGE_NAME}:${MAJOR_VERSION}${IMAGE_TAG}
                    sleep 10
                    '''
            }
        }

        stage('Health Check') {
            steps {
                dir('healthcheck-script') {
                    git branch: 'devops', credentialsId: "${GIT_CREDENTIALS_ID}", url: "${REPO}"
                }

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

        stage('Push Docker Image front-end') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKERHUB_USER', passwordVariable: 'DOCKERHUB_PASS')]) {
                    script {
                        sh """
                            echo "$DOCKERHUB_PASS" | docker login -u "$DOCKERHUB_USER" --password-stdin
                            docker push ${DOCKER_IMAGE_NAME}:${MAJOR_VERSION}${IMAGE_TAG}
                            docker push ${DOCKER_IMAGE_NAME}:${MAJOR_VERSION}${IMAGE_TAG}dev
                        """
                    }
                }
            }
        }
    }

    post {
        always {
            sh "docker rm -f ${CONTAINER_NAME} || true"
        }
    }
}
