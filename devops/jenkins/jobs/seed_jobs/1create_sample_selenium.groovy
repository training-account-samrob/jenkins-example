def folderName = 'ui_testing'

folder(folderName) {
    description('Folder for ui testing')
}

pipelineJob("${folderName}/ui_test") {
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('https://github.com/training-account-samrob/jenkins-example.git')
                        credentials('github')
                    }
                    branch('devops')
                }
        }
        scriptPath('devops/jenkins/jobs/ui_testing/ui_test.groovy')
        }
    }
}