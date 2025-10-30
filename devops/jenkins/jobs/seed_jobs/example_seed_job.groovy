pipelineJob('example') {
    authenticationToken('release') // Enables remote triggering with this token
    
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('https://github.com/training-account-samrob/jenkins-example.git')
                        credentials('github')
                    }
                    branch('front-end')
                }
        }
        scriptPath('devops/jenkins/jobs/build_jobs/auto_front_end_job.groovy')
        }
    }
}