pipeline {
    agent any

    environment {
        GIT_CREDENTIALS_ID = 'github'
        REPO = "https://github.com/mustaqmstk/Visa-Holder-Movement-Tracker.git"
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'ui-testing', credentialsId: "${GIT_CREDENTIALS_ID}", url: "${REPO}"
            }
        }

        stage('Run UI Tests') {
            steps {
                dir('ui-testing') {
                    sh 'mvn clean test'
                }
            }
        }
    }

    post {
        always {
            // Archive the report so it's available in Jenkins UI
            archiveArtifacts artifacts: 'ui-testing/target/cucumber-reports.html', fingerprint: true

            // Optional: Publish the report as a tab in Jenkins UI (requires HTML Publisher plugin)
            publishHTML(target: [
                reportDir: 'ui-testing/target',
                reportFiles: 'cucumber-reports.html',
                reportName: 'Cucumber Report',
                keepAll: true,
                alwaysLinkToLastBuild: true,
                allowMissing: false
            ])
        }
    }
}
