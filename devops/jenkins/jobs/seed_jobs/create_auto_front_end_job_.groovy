def folderName = 'build_jobs'
def auto = 'auto'

folder(folderName) {
    description('Folder for all build jobs')
}

folder("${folderName}/${auto}") {
    description('folder for all manual build jobs')
}

pipelineJob("${folderName}/${auto}/auto_front_end_build") {
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