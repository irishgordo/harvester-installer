def TellGitHubToCreateACheck(String name = 'default', String summary = 'default', String status = 'completed', String conclusion = 'success', String text = 'default', String token = 'empty'){
    try{
        echo "this is name: ${name}"
        echo "this is summary: ${summary}"
        echo "this is status: ${status}"
        echo "this is conclusion: ${conclusion}"
        echo "this is text: ${text}"
        commit = "${ghprbActualCommit}"
        echo "this is commit: ${commit}"
        def result = sh(script: '''
        curl -X POST -H "Content-Type: application/json" \
            -H "Accept: application/vnd.github+json" \
            -H "authorization: Bearer ''' + token + '''" \
            -d '{ "name": "''' + name + '''", \
                "head_sha": "''' + commit + '''", \
                "status": "''' + status +'''", \
                "conclusion": "''' + conclusion + '''", \
                "output": { "title": "''' + name + ''' for: ''' + commit + '''", \
                            "summary": "''' + summary + '''", \
                            "text": "''' + text + '''"}}' https://api.github.com/repos/irishgordo/harvester-installer/check-runs
        ''', returnStdout: true)
        echo "${result}"
        return result
    } catch(err) {
        echo "'issue communicating with GitHub API...'"
        echo err.getMessage()
    }                                
}

pipeline {
    environment {
        GHUB_COMMENT_AIRGAP_INTEGRATION_TEST = "run-tests-airgapped-integration"
        GHUB_COMMENT_RUN_API_TESTS = "run-tests-api"
        GHUB_COMMENT_RUN_API_TESTS_OFFLINE = "run-tests-api-offline"
        GHUB_COMMENT_INTEGRATION_TEST = "run-tests-integration"
    }
    // TODO: Switch Agent To Original When Not Testing
    agent any
    stages {
        stage('checkin with github'){
            steps {
                withCredentials([usernamePassword(credentialsId: 'jenkins-irishgordo-team-harvester', usernameVariable: 'GITHUB_APP', passwordVariable: 'GITHUB_ACCESS_TOKEN')]) {
                    TellGitHubToCreateACheck('initalizing_github_checks_messaging', 'basic initialization of github checks api', 'completed', 'success', 'basic initalizaiton should be visible in github checks api', "${GITHUB_ACCESS_TOKEN}")                      
                }
            }
        }
        stage('initalize'){
            steps {
                script{
                    sh 'printenv' 
                    params.each {param ->
                        println "${param.key} -> ${param.value} "
                    }
                    sh "echo 'Building..'"
                    sh "ls -alh"
                    sh "cd ${env.WORKSPACE} && rm -rfv tests"
                }
                withCredentials([usernamePassword(credentialsId: 'jenkins-irishgordo-team-harvester', usernameVariable: 'GITHUB_APP', passwordVariable: 'GITHUB_ACCESS_TOKEN')]) {
                    TellGitHubToCreateACheck('initialization_stage', 'basic initialization of pipeline', 'completed', 'success', 'basic initalizaiton of pipeline is successful...', "${GITHUB_ACCESS_TOKEN}")                      
                }
            }
        }
        stage('Checkout harvester-install pull request') {
            steps {
                dir('harvester-installer') {
                    checkout([$class: 'GitSCM', branches: [[name: "FETCH_HEAD"]],
                             extensions: [[$class: 'LocalBranch']],
                             userRemoteConfigs: [[refspec: "+refs/pull/${ghprbPullId}/head:refs/remotes/origin/PR-${ghprbPullId}", url: "https://github.com/irishgordo/harvester-installer"]]])
                }
                withCredentials([usernamePassword(credentialsId: 'jenkins-irishgordo-team-harvester', usernameVariable: 'GITHUB_APP', passwordVariable: 'GITHUB_ACCESS_TOKEN')]) {
                    TellGitHubToCreateACheck('checkout_harvester_pull_request_stage', 'checking out harvester pull request stage', 'completed', 'success', "successfully fetched latest harvester-installer", "${GITHUB_ACCESS_TOKEN}")
                }     
            }
        }
        stage('Pull inital Settings.YML Directly'){
            steps{
                script{
                    // TODO: temporary logic pending #45 gets merged in and all
                    if (ghprbCommentBody?.trim()){
                        sh "curl https://raw.githubusercontent.com/irishgordo/ipxe-examples/feat/2096-air-gapped-rancher-single-node-proof-of-concept/vagrant-pxe-airgap-harvester/settings.yml > /tmp/inital_settings.yml"
                    } else{
                        if (params.settings_yaml_url_override != 'DEFAULT'){
                            sh "curl ${params['settings_yaml_url_override']} > /tmp/inital_settings.yml"
                        }else{
                            sh "curl https://raw.githubusercontent.com/harvester/ipxe-examples/main/vagrant-pxe-airgap-harvester/settings.yml > /tmp/inital_settings.yml"
                        }
                    }
                }
                withCredentials([usernamePassword(credentialsId: 'jenkins-irishgordo-team-harvester', usernameVariable: 'GITHUB_APP', passwordVariable: 'GITHUB_ACCESS_TOKEN')]) {
                    TellGitHubToCreateACheck('pulled_inital_yaml_settings_for_vagrant_installer', 'pulled down the base settings for the harvester installer vagrant-pxe-airgap-harvester', 'completed', 'success', "we've pulled down the settings.yml and will override them if necessary... ", "${GITHUB_ACCESS_TOKEN}")
                }     
            }
        }
        stage('Read Parameters, Override YAML If Needed'){
            steps{
                script{
                    // ghprbCommentBody (run-tests-x)
                    def baseSettingsYaml = readYaml file: '/tmp/inital_settings.yml'
                    sh "echo 'initial YAML Settings that will be parsed and overwritten:'"
                    print baseSettingsYaml
                    if (params.harvester_cluster_nodes != 'YAML_PROVIDED'){
                        baseSettingsYaml.harvester_cluster_nodes = params.harvester_cluster_nodes.trim() as Integer
                    }
                    if (ghprbCommentBody?.trim()){
                        // TODO: Implement logic for switching on git commit, setting values
                        sh "echo 'here'"
                        if( ghprbCommentBody == env.GHUB_COMMENT_RUN_API_TESTS ){
                            sh "echo 'running only api tests, no rancher integration...'"
                            baseSettingsYaml.harvester_network_config.offline = false
                            baseSettingsYaml.rancher_config.run_single_node_air_gapped_rancher = false
                            baseSettingsYaml.rancher_config.run_single_node_rancher = false
                        }
                        if ( ghprbCommentBody == env.GHUB_COMMENT_AIRGAP_INTEGRATION_TEST ){
                            sh "echo 'running airgapped integration testing, air-gapped Rancher and air-gapped Harvester...'"
                            baseSettingsYaml.harvester_network_config.offline = true 
                            baseSettingsYaml.rancher_config.run_single_node_air_gapped_rancher = true
                            baseSettingsYaml.rancher_config.run_single_node_rancher = true
                        }
                        if ( ghprbCommentBody == env.GHUB_COMMENT_INTEGRATION_TEST ){
                            sh "echo 'running integration tests non-air-gapped harvester & non-air-gapped rancher...'"
                            baseSettingsYaml.harvester_network_config.offline = false
                            baseSettingsYaml.rancher_config.run_signle_node_rancher = true 
                            baseSettingsYaml.rancher_config.run_single_node_air_gapped_rancher = false
                        }
                        if ( ghprbCommentBody == env.GHUB_COMMENT_RUN_API_TESTS_OFFLINE ){
                            sh "echo 'running only harvester api tests offline, no rancher integration...'"
                            baseSettingsYaml.harvester_network_config.offline = true
                            baseSettingsYaml.rancher_config.run_signle_node_rancher = false
                            baseSettingsYaml.rancher_config.run_single_node_air_gapped_rancher = false
                        }
                    } else{
                        // TODO: this would be for manual builds, not triggered by github
                        if (params.runtime_configuration?.trim()){
                            sh "echo 'runtime config defined...'"
                        }else{
                            sh "echo 'runtime config not defined, defaulting to normal mode..'"
                            params.runtime_configuration = 'normal'
                        }
                        if (params.runtime_configuration == 'air_gap_single_rancher_with_harvester'){
                            baseSettingsYaml.harvester_network_config.offline = true
                            baseSettingsYaml.rancher_config.run_single_node_air_gapped_rancher = true
                            baseSettingsYaml.rancher_config.run_signle_node_rancher = true
                        }
                        if (params.runtime_configuration == 'harvester_offline'){
                            baseSettingsYaml.harvester_network_config.offline = true
                            baseSettingsYaml.rancher_config.run_single_node_air_gapped_rancher = false
                            baseSettingsYaml.rancher_config.run_single_node_rancher = false
                        }
                        if (params.runtime_configuration == 'normal'){
                            baseSettingsYaml.harvester_network_config.offline = false
                            baseSettingsYaml.rancher_config.run_single_node_air_gapped_rancher = false
                            baseSettingsYaml.rancher_config.run_single_node_rancher = false
                        }
                        if (params.runtime_configuration == 'single_node_rancher_non_airgapped'){
                            baseSettingsYaml.rancher_config.run_single_node_rancher = true
                            baseSettingsYaml.rancher_config.run_single_node_air_gapped_rancher = false
                            baseSettingsYaml.harvester_network_config.offline = false
                        }
                        if (params.runtime_configuration == 'single_node_rancher_non_airgapped_with_offline_harvester'){
                            baseSettingsYaml.rancher_config.run_single_node_rancher = true
                            baseSettingsYaml.rancher_config.run_single_node_air_gapped_rancher = false
                            baseSettingsYaml.harvester_network_config.offline = true
                        }
                    }
                    if (params.rancher_config_node_disk_size != 'YAML_PROVIDED'){
                        baseSettingsYaml.rancher_config.node_disk_size = params.rancher_config_node_disk_size.trim() as Integer
                    }
                    if (params.rancher_config_run_single_node_air_gapped_rancher != false){
                        baseSettingsYaml.rancher_config.run_single_node_air_gapped_rancher = params.rancher_config_run_single_node_air_gapped_rancher
                    }
                    if (params.rancher_config_cert_manager_version != 'YAML_PROVIDED'){
                        baseSettingsYaml.rancher_config.cert_manager_version = params.rancher_config_cert_manager_version.trim()  
                    }
                    if (params.rancher_config_k3s_url_escaped_version != 'YAML_PROVIDED'){
                        baseSettingsYaml.rancher_config.k3s_url_escaped_version = params.rancher_config_k3s_url_escaped_version.trim()
                    }
                    if (params.rancher_config_rancher_version != 'YAML_PROVIDED'){
                        baseSettingsYaml.rancher_config.rancher_version = params.rancher_config_rancher_version.trim()
                        if (params.rancher_config_rancher_version_no_prefix == 'YAML_PROVIDED'){
                            error "If you specify params.rancher_config_rancher_version you must also specify rancher_config_rancher_version_no_prefix, currently - until future implementations refactor there to be only one rancher_version and prefix is either added or removed via additional business logic."
                        }
                    }
                    if (params.rancher_config_rancher_version_no_prefix != 'YAML_PROVIDED'){
                        baseSettingsYaml.rancher_config.rancher_version_no_prefix = params.rancher_config_rancher_version_no_prefix.trim()
                        if (params.rancher_config_rancher_version == 'YAML_PROVIDED'){
                            error "If you specify params.rancher_config_rancher_version_no_prefix you must also specify rancher_config_rancher_version, currently - until future implementations refactor there to be only one rancher_version and prefix is either added or removed via additional business logic."
                        }
                    }
                    if (params.rancher_config_cpu != 'YAML_PROVIDED'){
                        baseSettingsYaml.rancher_config.cpu = params.rancher_config_cpu.trim() as Integer
                    }
                    if (params.rancher_config_memory != 'YAML_PROVIDED'){
                        baseSettingsYaml.rancher_config.memory = params.rancher_config_memory.trim() as Integer
                    }
                    if (params.rancher_config_rancher_install_domain != 'YAML_PROVIDED'){
                        baseSettingsYaml.rancher_config.rancher_install_domain = params.rancher_config_rancher_install_domain.trim()
                    }
                    if (params.rancher_config_registry_domain != 'YAML_PROVIDED'){
                        baseSettingsYaml.rancher_config.registry_domain = params.rancher_config_registry_domain.trim()
                    }
                    if (params.rancher_config_bootstrap_password != 'YAML_PROVIDED'){
                        baseSettingsYaml.rancher_config.bootstrap_password = params.rancher_config_bootstrap_password.trim()
                    }
                    if (params.harvester_dashboard_admin_user != 'YAML_PROVIDED'){
                        baseSettingsYaml.harvester_dashboard.admin_user = params.harvester_dashboard_admin_user.trim()
                    }
                    if (params.harvester_dashboard_admin_password != 'YAML_PROVIDED'){
                        baseSettingsYaml.harvester_dashboard.admin_password = params.harvester_dashboard_admin_password.trim()
                    }
                    def result = writeYaml file: '/tmp/inital_settings.yml', data: baseSettingsYaml, overwrite: true
                    sh "echo 'YAML settings for this CI run is as follows, after overwriting if needed:'"
                    print baseSettingsYaml
                    withCredentials([usernamePassword(credentialsId: 'jenkins-irishgordo-team-harvester', usernameVariable: 'GITHUB_APP', passwordVariable: 'GITHUB_ACCESS_TOKEN')]) {
                        TellGitHubToCreateACheck('read_params_overwrite_base_settings_yaml_stage', 'params have been read, overwritten or adjusted - if needed for params and settings.yml', 'completed', 'success', "${baseSettingsYaml}", "${GITHUB_ACCESS_TOKEN}")
                    } 
                }       
            }
        }
        stage("Grab Tests Repo And Prepare CI E2E Image"){
            steps{
                script{
                    // TODO: FIX! Totally temporary scaffolding
                    dir("${params['WORKSPACE']}"){
                        sh "git clone https://github.com/irishgordo/tests.git"
                        sh "cd tests && git checkout feat/2096-Supporting-E2E-CI-Work-For-Harvester-Installer && make build-docker-e2e-ci-image"
                    }
            
                    // if(params.tests_repo_url != 'DEFAULT'){
                    //     dir("${params['WORKSPACE']}"){
                    //         sh "git clone ${params['tests_repo_url']}"
                    //     }
                    // }else{
                    //     dir("${params['WORKSPACE']}"){
                    //         sh "git clone https://github.com/harvester/tests.git"
                    //     }
                    // }
                    // if(params.tests_repo_branch != 'DEFAULT'){
                    //     dir("${params['WORKSPACE']}"){
                    //         sh "cd tests && git checkout ${params['tests_repo_branch']} && make build-docker-e2e-ci-image"
                    //     }
                    // }else{
                    //     dir("${params['WORKSPACE']}"){
                    //         sh "cd tests && make build-docker-e2e-ci-image"
                    //     }
                    // }
                }
            }
        }
        stage('Test harvester-installer pull request') {
            steps {
                script {
                    ansiblePlaybook extras: "-e WORKSPACE=${env.WORKSPACE} -e PR_ID=${ghprbPullId}", playbook: "${env.WORKSPACE}/harvester-installer/ci/run_vagrant_install_test.yml"
                }
            }
        }
        stage('Test Harvester API'){
            options {
                timeout(time: 1, unit: "HOURS")
            }
            when{
                anyOf{
                    expression{
                        return params.runtime_configuration == 'air_gap_single_rancher_with_harvester'
                    }
                    expression{
                        return params.runtime_configuration == 'single_node_rancher_non_airgapped_with_offline_harvester'
                    }
                    expression{
                        return params.runtime_configuration == 'single_node_rancher_non_airgapped'
                    }
                    expression{
                        return ghprbCommentBody?.trim()
                    }
                }
            }
            steps {
                script{
                    try { 
                        docker.image('harvester_installer_e2e_ci_image:latest').inside(){
                            sh "ls -alh"
                            def baseSettingsYaml = readYaml file: '/tmp/inital_settings.yml'
                            print baseSettingsYaml
                            // TODO: Fix! Some tests are breaking... these tests are ignored in harvester-e2e-tests/apis:
                            // - verify_host_maintenance_mode 
                            // - host_mgmt_maintenance_mode 
                            // - host_reboot_maintenance_mode 
                            // - host_poweroff_state 
                            // - create_images_using_terraform 
                            // - create_keypairs_using_terraform 
                            // - create_edit_network 
                            // - create_network_using_terraform 
                            // - create_volume_using_terraform
                            sh "cd tests && pytest -k 'not verify_host_maintenance_mode and not host_mgmt_maintenance_mode and not host_reboot_maintenance_mode and not host_poweroff_state and not create_images_using_terraform and not create_keypairs_using_terraform and not create_edit_network and not create_network_using_terraform and not create_volume_using_terraform' --junitxml=result_harvester_api_latest.xml -m 'not delete_host' harvester_e2e_tests/apis --username ${baseSettingsYaml.harvester_dashboard.admin_user} --password ${baseSettingsYaml.harvester_dashboard.admin_password} --endpoint https://${baseSettingsYaml.harvester_network_config.vip.ip} --harvester_cluster_nodes ${baseSettingsYaml.harvester_cluster_nodes}"
                        }
                    } catch (err) {
                        echo "failed within docker image inside command on harvester testing api stage"
                        echo err.getMessage()
                    }
                }
            }
        }
        stage('Test Rancher Integration'){
            options {
                timeout(time: 1, unit: "HOURS")
            }
            when{
                anyOf{
                    expression{
                        return params.runtime_configuration == 'air_gap_single_rancher_with_harvester'
                    }
                    expression{
                        return params.runtime_configuration == 'single_node_rancher_non_airgapped_with_offline_harvester'
                    }
                    expression{
                        return params.runtime_configuration == 'single_node_rancher_non_airgapped'
                    }
                    expression{
                        return ghprbCommentBody?.trim() && ghprbCommentBody == env.GHUB_COMMENT_INTEGRATION_TEST
                    }
                    expression{
                        return ghprbCommentBody?.trim() && ghprbCommentBody == env.GHUB_COMMENT_AIRGAP_INTEGRATION_TEST
                    }
                }
            }
            steps{
                script{
                    try{
                        docker.image('harvester_installer_e2e_ci_image:latest').inside(){
                            sh "ls -alh"
                            def baseSettingsYaml = readYaml file: '/tmp/inital_settings.yml'
                            print baseSettingsYaml
                            sh "cd tests && pytest --junitxml=result_harvester_rancher_integration_latest.xml harvester_e2e_tests/scenarios/test_rancher_integration.py --endpoint https://${baseSettingsYaml.harvester_network_config.vip.ip} --password ${baseSettingsYaml.harvester_dashboard.admin_password} --username ${baseSettingsYaml.harvester_dashboard.admin_user} --rancher-endpoint https://${baseSettingsYaml.rancher_config.rancher_install_domain} --rancher-admin-password ${baseSettingsYaml.rancher_config.bootstrap_password}"
                        }
                    } catch(err) {
                        echo err.getMessage()
                    }
                    
                }
                
            }
        }        
    }
    post {
        // Clean after build
        always {
            dir("${env.WORKSPACE}"){
                script{
                    try{
                        // TODO: We don't want to publish this way: https://plugins.jenkins.io/junit/#plugin-content-test-result-checks-for-github-projects
                        // Only because we would need to entirely work how our pipelines are set up
                        // we would need to instead of having just 'two' pipeline jobs, we'd need to nest the jobs
                        // in a GitHub Org and then create a GitHub Org for Harvester - only bringing in the Harvester repo
                        // yet, running a project under the GitHub Org it seemed to not entirely respect pipeline triggers - as well as, the seeder, would
                        // exist within the GitHub org, seeding/re-seeding pipeline changes, but the actual pipeline, there were some initial issues figuring out how
                        // to get that into the GitHub org, so this was timeboxed, test results will be sent manually via the API call with the GitHub App Credential
                        // instead of using junit's builtin for now, until we can get more cycles to figure out how to take advantage of the default junit builtin
                        junit allowEmptyResults: true, skipPublishingChecks: true, keepLongStdio: true, testResults: 'tests/result_harvester_api_latest.xml'
                        testResultAction =  currentBuild.rawBuild.getAction(hudson.tasks.test.AggregatedTestResultAction.class);
                        if (testResultAction == null) {
                            println("No tests")
                            return
                        }

                        childReports = testResultAction.getChildReports();

                        if (childReports == null || childReports.size() == 0) {
                            println("No child reports")
                            return
                        }

                        def failures = [:]
                        childReports.each { report ->
                            def result = report.result;

                            if (result == null) {
                                println("null result from child report")
                            }
                            else if (result.failCount < 1) {
                                println("result has no failures")
                            }
                            else {
                                println("overall fail count: ${result.failCount}")
                                failedTests = result.getFailedTests();

                                failedTests.each { test ->
                                    failures.put(test.fullDisplayName, test)
                                    println("Failed test: ${test.fullDisplayName}\n" +
                                            "name: ${test.name}\n" +
                                            "age: ${test.age}\n" +
                                            "failCount: ${test.failCount}\n" +
                                            "failedSince: ${test.failedSince}\n" +
                                            "errorDetails: ${test.errorDetails}\n")
                                }
                            }
                        }
                        def passes = [:]
                        childReports.each { report ->
                            def result = report.result;

                            if (result == null) {
                                println("null result from child report")
                            }
                            else {
                                println("overall pass count: ${result.passCount}")
                                passedTests = result.getPassedTests();

                                passedTests.each { test ->
                                    passes.put(test.fullDisplayName, test)
                                }
                            }
                        }
                        def subject = "testing..."
                        // TODO: see if there's a tangible fix for this...
                        // def testResult = build.testResultAction
                        // def didTestsPass = 0
                        // if ( testResult != null ) {
                        //     def testsTotal = testResult.totalCount
                        //     def testsFailed = testResult.failCount
                        //     didTestsPass = testResult.failCount
                        //     def testsSkipped = testResult.skipCount
                        //     def testsPassed = testsTotal - testsFailed - testsSkipped

                        //     subject = build.externalizableId + " : Tests passed " + testsPassed + ", out of " + testsTotal
                            
                        // }
                        if(didTestsPass == 0) {
                            withCredentials([usernamePassword(credentialsId: 'jenkins-irishgordo-team-harvester', usernameVariable: 'GITHUB_APP', passwordVariable: 'GITHUB_ACCESS_TOKEN')]) {
                                TellGitHubToCreateACheck('post_stage_test_api_results', 'Subject: ' + subject, 'completed', 'success', "${passes.toMapString()} \n ${failures.toMapString()}", "${GITHUB_ACCESS_TOKEN}")
                            } 
                        }
                        else{
                            withCredentials([usernamePassword(credentialsId: 'jenkins-irishgordo-team-harvester', usernameVariable: 'GITHUB_APP', passwordVariable: 'GITHUB_ACCESS_TOKEN')]) {
                                TellGitHubToCreateACheck('post_stage_test_api_results', 'Subject: ' + subject, 'completed', 'success', "${passes.toMapString()} \n ${failures.toMapString()}", "${GITHUB_ACCESS_TOKEN}")
                            } 
                        }
                    } catch(err) {
                        echo err.getMessage()
                    }
                    try{
                        // TODO: We don't want to publish this way: https://plugins.jenkins.io/junit/#plugin-content-test-result-checks-for-github-projects
                        // Only because we would need to entirely work how our pipelines are set up
                        // we would need to instead of having just 'two' pipeline jobs, we'd need to nest the jobs
                        // in a GitHub Org and then create a GitHub Org for Harvester - only bringing in the Harvester repo
                        // yet, running a project under the GitHub Org it seemed to not entirely respect pipeline triggers - as well as, the seeder, would
                        // exist within the GitHub org, seeding/re-seeding pipeline changes, but the actual pipeline, there were some initial issues figuring out how
                        // to get that into the GitHub org, so this was timeboxed, test results will be sent manually via the API call with the GitHub App Credential
                        // instead of using junit's builtin for now, until we can get more cycles to figure out how to take advantage of the default junit builtin
                        junit skipPublishingChecks: true, testResults: 'tests/result_harvester_rancher_integration_latest.xml'
                    } catch(err) {
                        echo err.getMessage()
                    }
                    // TODO: May not be needed anymore tbd...
                    // try{
                    //     sh "echo '' > /var/lib/jenkins/.ssh/known_hosts"
                    // } catch(err) {
                    //     echo err.getMessage()                    
                    // }
                }
                // We clean up things here in the pipeline, since prior, usually the cleanup would happen in the run_vagrant_install_test.yml
                // But if we had cleaned up then, the VMs would of disappeared and there would be no systems to test under
                sh "cd ipxe-examples/vagrant-pxe-airgap-harvester/ && ls -alh && vagrant destroy -f"
                sh "cd ${env.WORKSPACE}/tests && make destroy-docker-e2e-ci-image"
                sh "cd ${env.WORKSPACE} && rm -rfv tests"
                deleteDir()
            }
            cleanWs()
        }
    }
}