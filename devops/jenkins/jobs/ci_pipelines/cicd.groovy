pipeline {
    agent any

    environment {
        GIT_CREDENTIALS_ID = 'mustaq'
        GIT_BRANCH = "main"
        DOCKERHUB_USERNAME = 'samdroberts'
        MAJOR_VERSION = '1.'
        IMAGE_TAG = "${BUILD_NUMBER}"
        FRONTEND_IMAGE_NAME = "${DOCKERHUB_USERNAME}/visa-front"
        BACKEND_IMAGE_NAME = "${DOCKERHUB_USERNAME}/visa-back"
        FRONTEND_CONTAINER_NAME = 'react-app-container'
        BACKEND_CONTAINER_NAME = 'java-app-container'
        K8_MANIFEST_ROOT = 'devops/kubernetes/gitops/manifests'
        FRONTEND_DEPLOYMENT = "${K8_MANIFEST_ROOT}/front-end/front-end-deployment.yaml"
        BACKEND_DEPLOYMENT = "${K8_MANIFEST_ROOT}/back-end/back-end-deployment.yaml"
        HEALTHCHECK_TIMEOUT = '120'
        REPO = "https://github.com/mustaqmstk/Visa-Holder-Movement-Tracker.git"
    }

    stages {

        stage('Checkout') {
            steps {
                git branch: 'main', credentialsId: "${GIT_CREDENTIALS_ID}", url: "${REPO}"
            }
        }

        // stage('ls') {
        //     steps {
        //         sh 'tree'
        //     }
        // }

        // this is used for updating the image tag in the yaml files to make sure that the indentation isn't screwed up
        stage('Install yq') {
            steps {
                sh """
                    if ! command -v yq >/dev/null 2>&1; then
                        wget https://github.com/mikefarah/yq/releases/latest/download/yq_linux_amd64 -O /usr/local/bin/yq
                        chmod +x /usr/local/bin/yq
                    fi
                """
            }
        }


/*

FRONT END CI

*/

        stage('STARTED: front end build') {
            steps {
                echo "front end build started for version ${MAJOR_VERSION}${IMAGE_TAG}"
            }
        }

        // stage('something') {
        //     steps {
        //         sh 'git branch --no-color | tee /dev/stdout'
        //     }
        // }

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

        //         stage('test') {
        //     steps {
        //         dir('front-end') {
        //             // work out how to run unit tests
        //         }
        //     }
        // }

        stage('Build Docker front-end Image') {
            steps {
                script {
                    dir('front-end') {
                        docker.build("${FRONTEND_IMAGE_NAME}:${MAJOR_VERSION}${IMAGE_TAG}")
                    }
                }
            }
        }

        stage('Trivy Scan front-end') {
            steps {
                script {
                    def status = sh(
                        script: "trivy image --severity CRITICAL --exit-code 1 --quiet ${FRONTEND_IMAGE_NAME}:${MAJOR_VERSION}${IMAGE_TAG}",
                        returnStatus: true
                    )
                    if (status != 0) {
                        error("Trivy scan failed: critical vulnerabilities found.")
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
                            docker push ${FRONTEND_IMAGE_NAME}:${MAJOR_VERSION}${IMAGE_TAG}
                        """
                    }
                }
            }
        }


        stage('Update Front-end Kubernetes Manifest') {
            steps {
                script {
                    // Match the image name regardless of current tag
                    sh """
                        yq e '(.spec.template.spec.containers[] | select(.image | test("^${FRONTEND_IMAGE_NAME}:"))).image = "${FRONTEND_IMAGE_NAME}:${MAJOR_VERSION}${IMAGE_TAG}"' -i ${FRONTEND_DEPLOYMENT}
                    """
                }
            }
        }

        stage('testing branch') {
            steps {
                sh 'git branch --no-color | tee /dev/stdout'
            }
        }

        stage('COMPLETE: front end build') {
            steps {
                echo "front end build complete for version ${MAJOR_VERSION}${IMAGE_TAG}"
            }
        }



/*

BACKEND CI

*/

        stage('STARTED: back end build') {
            steps {
                echo "back end build started for version ${MAJOR_VERSION}${IMAGE_TAG}"
            }
        }

        stage('Build with Maven') {
            steps {
                dir('back-end') {
                    sh 'mvn clean package'
                }
            }
        }

        stage('Build Docker back-end Image') {
            steps {
                script {
                    dir('back-end') {
                        docker.build("${BACKEND_IMAGE_NAME}:${MAJOR_VERSION}${IMAGE_TAG}")
                    }
                }
            }
        }

        stage('Trivy Scan back-end') {
            steps {
                script {
                    def status = sh(
                        script: "trivy image --severity CRITICAL --exit-code 1 --quiet ${BACKEND_IMAGE_NAME}:${MAJOR_VERSION}${IMAGE_TAG}",
                        returnStatus: true
                    )
                    if (status != 0) {
                        error("Trivy scan failed: critical vulnerabilities found.")
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
                        docker push ${BACKEND_IMAGE_NAME}:${MAJOR_VERSION}${IMAGE_TAG}
                        """
                    }
                }
            }
        }

        stage('Update Back-end Kubernetes Manifest ') {
            steps {
                script {
                    // Match the image name regardless of current tag
                    sh """
                        yq e '(.spec.template.spec.containers[] | select(.image | test("^${BACKEND_IMAGE_NAME}:"))).image = "${BACKEND_IMAGE_NAME}:${MAJOR_VERSION}${IMAGE_TAG}"' -i ${BACKEND_DEPLOYMENT}
                    """
                }
            }
        }

        stage('COMPLETE: back end build') {
            steps {
                echo "back end build complete for version ${MAJOR_VERSION}${IMAGE_TAG}"
            }
        }

// need to write





/*

CODE MERGE AND DEPLOYMENT

*/

        stage('STARTED: Deployment') {
            steps {
                echo "Deployment of version ${MAJOR_VERSION}${IMAGE_TAG} started"
            }
        }


        // stage('Commit & Push Changes') {
        //     steps {
        //         script {
        //             def branch = env.GIT_BRANCH ?: sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()
        //             withCredentials([usernamePassword(credentialsId: GIT_CREDENTIALS_ID, usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASS')]) {
        //                 sh """
        //                     git config user.name "training-account-samrob"
        //                     git config user.email "samdroberts87@icloud.com"
        //                     git add ${FRONTEND_DEPLOYMENT}
        //                     git add ${BACKEND_DEPLOYMENT}
        //                     git commit -m "Update image tag to ${MAJOR_VERSION}${IMAGE_TAG} [ci skip]" || echo "No changes to commit"
        //                     git push origin ${main}
        //                 """
        //             }
        //         }
        //     }
        // }

        stage('commit and push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'mustaq', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD')]) {
                    sh '''
                        git config user.name "$GIT_USERNAME"
                        git config user.email "samdroberts87@icloud.com"
                        git remote set-url origin https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/mustaqmstk/Visa-Holder-Movement-Tracker.git

                        git add ${FRONTEND_DEPLOYMENT}
                        git add ${BACKEND_DEPLOYMENT}
                        git commit -m "Update image tag to ${MAJOR_VERSION}${IMAGE_TAG} [ci skip]" || echo "No changes to commit"
                        git push origin main
                    '''
                }
            }
        }



        stage('Deployment initialised') {
            steps {
                echo "Deployment of version ${MAJOR_VERSION}${IMAGE_TAG} initialised (argoCD sync pending)"
            }
        }

        stage('WAIT: Deployment commencing') {
            steps {
                sh 'sleep 300'
            }
        }

        stage('Deployment Complete') {
            steps {
                sh 'echo deployment complete'
            }
        }

        stage('Run UI Tests') {
            steps {
                dir('ui-testing') {
                    sh 'mvn clean test'
                }
            }
        }

        stage('New version deployed and tested'){
            steps {
                echo "COMPLETE"
            }
        }
    }

    // post {
    //     always {
    //         sh "docker rm -f ${FRONTEND_CONTAINER_NAME} ${BACKEND_CONTAINER_NAME} || true"
    //     }
    // }

}
