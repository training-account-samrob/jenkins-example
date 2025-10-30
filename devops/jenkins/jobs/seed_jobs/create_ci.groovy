def folderName = 'Automated_Release_pipeline'

folder(folderName) {
    description('fully automated CI/CD pipeline for each release.')
}


pipelineJob("${folderName}/Automated_Release_Pipeline") {
    authenticationToken('release') // Enables remote triggering with this token

    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('https://github.com/mustaqmstk/Visa-Holder-Movement-Tracker.git')
                        credentials('github')
                    }
                    branch('main')
                }
        }
        scriptPath('devops/jenkins/jobs/ci_pipelines/cicd.groovy')
        }
    }
}