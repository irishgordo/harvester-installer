node {
    stage('Initalize...'){
        checkout scm
        script{
            sh "ls -la ${pwd()}"
            sh 'printenv' 
            params.each {param ->
            println "${param.key} -> ${param.value} "
            }
            sh "echo 'finishing initalizing..'"
        }
    }
    stage('Attempt deploy of seed..'){
        checkout scm
        script{
            jobDsl targets: ['jobs/ci_cd_job.groovy'].join('\n'),
                removedJobAction: 'IGNORE',
                removedViewAction: 'IGNORE',
                removedConfigFilesAction: 'IGNORE',
                lookupStrategy: 'JENKINS_ROOT',
                additionalParameters: [:],
                sandbox: true
        }
    }
}