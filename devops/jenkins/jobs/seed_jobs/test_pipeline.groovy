pipelineJob('my_test_job') {
    // authenticationToken('release') // Enables remote triggering with this token
    
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('https://github.com/training-account-samrob/jenkins-example.git')
                        credentials('github')
                    }
                    branch('main')
                }
        }
        scriptPath('devops/jenkins/jobs/build_jobs/auto_back_end_job.groovy')
        }
    }
}