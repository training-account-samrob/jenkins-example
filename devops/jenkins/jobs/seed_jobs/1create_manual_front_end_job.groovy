def folderName = 'build_jobs'
def manual = 'manual'

folder(folderName) {
    description('Folder for all build jobs')
}

folder("${folderName}/${manual}") {
    description('folder for all manual build jobs')
}

pipelineJob("${folderName}/${manual}/manual_front_end_build") {
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
        scriptPath('devops/jenkins/jobs/build_jobs/manual_front_end_job.groovy')
        }
    }
}