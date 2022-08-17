def MessageGitHubAboutStatus(name, summary, headSha, authToken, status, text){
    sh '''
    curl -X POST -H "Content-Type: application/json" \
    -H "Accept: application/vnd.github+json" \
    -H "authorization: Bearer ${authToken}" \
    -d '{ "name": "'${name}'", \
        "head_sha": "'${headSha}'", \
        "status": "completed", \
        "conclusion": "neutral", \
        "output": { "title": "'${name}' for: '${headSha}'", \
                    "summary": "'${summary}'", \
                    "text": "'{text}'"}}' https://api.github.com/repos/irishgordo/harvester-installer/check-runs
    '''
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
            // when{
            //     anyOf{
            //         expression{
            //             return ghprbCommentBody?.trim()
            //         }
            //     }
            // }
            steps {
                withCredentials([usernamePassword(credentialsId: 'jenkins-irishgordo-team-harvester',
                                            usernameVariable: 'GITHUB_APP',
                                            passwordVariable: 'GITHUB_ACCESS_TOKEN')]) {
                    MessageGitHubAboutStatus("inital_ghub_check", "inital check", "'${ghprbActualCommit}'", "'${GITHUB_ACCESS_TOKEN}'", 'completed', 'completed inital check')
                }
            }
        }
        stage('initalize'){
            steps {
                // withCredentials([usernamePassword(credentialsId: 'jenkins-irishgordo-team-harvester',
                //                                 usernameVariable: 'GITHUB_APP_ZA',
                //                                 passwordVariable: 'GITHUB_ACCESS_TOKEN_ZA')]) {
                script{
                    sh 'printenv' 
                    params.each {param ->
                        println "${param.key} -> ${param.value} "
                    }
                    sh "echo 'Building..'"
                    sh "ls -alh"
                    sh "cd ${env.WORKSPACE} && rm -rfv tests"
                    //publishChecks(name: 'InitializationCompleted', conclusion: 'success', summary: 'Initialization was successful...')
                    // try{
                    //     sh '''
                    //     curl -X POST -H "Content-Type: application/json" \
                    //         -H "Accept: application/vnd.github.antiope-preview+json" \
                    //         -H "authorization: Bearer ${GITHUB_ACCESS_TOKEN_ZA}" \
                    //         -d '{ "name": "initialization_stage", \
                    //             "head_sha": "'${ghprbActualCommit}'", \
                    //             "status": "completed", \
                    //             "conclusion": "success", \
                    //             "output": { "title": "Initialization Stage Completed for: '${ghprbActualCommit}'", \
                    //                         "summary": "Initalization Stage Was Completed - cleaning and logging env for: '${env.WORKSPACE}'", \
                    //                         "text": "initialization completed"}}' https://api.github.com/repos/irishgordo/harvester-installer/check-runs
                    //     '''
                    // } catch(err) {
                    //     echo err.getMessage()
                    // }
                }
                //}
                // sh "echo 'initalizing, cleaning up workspace...'"
                // cleanWs(cleanWhenNotBuilt: true,
                //     deleteDirs: true,
                //     disableDeferredWipeout: false,
                //     notFailBuild: true,
                //     patterns: [[pattern: '.gitignore', type: 'INCLUDE'],
                //             [pattern: '.propsfile', type: 'EXCLUDE']])
            
            }
        }
        stage('Checkout harvester-install pull request') {
            steps {
                dir('harvester-installer') {
                    checkout([$class: 'GitSCM', branches: [[name: "FETCH_HEAD"]],
                             extensions: [[$class: 'LocalBranch']],
                             userRemoteConfigs: [[refspec: "+refs/pull/${ghprbPullId}/head:refs/remotes/origin/PR-${ghprbPullId}", url: "https://github.com/irishgordo/harvester-installer"]]])
                }
                script{
                    sh "ls -alh"
                    //publishChecks(name: 'AcquiredHarvesterInstaller', conclusion: 'success', summary: 'Acquired Harvester-Installer repo at pull request was successful...')
                }
                // withCredentials([usernamePassword(credentialsId: 'jenkins-irishgordo-team-harvester',
                //                                 usernameVariable: 'GITHUB_APP_A',
                //                                 passwordVariable: 'GITHUB_ACCESS_TOKEN_A')]) {
                //         sh '''
                //         curl -X POST -H "Content-Type: application/json" \
                //             -H "Accept: application/vnd.github.antiope-preview+json" \
                //             -H "authorization: Bearer ${GITHUB_ACCESS_TOKEN_A}" \
                //             -d '{ "name": "checkout_harvester_installer_pr", \
                //                 "head_sha": "'${ghprbActualCommit}'", \
                //                 "status": "completed", \
                //                 "conclusion": "success", \
                //                 "output": { "title": "Checkout Harvester-Installer Stage Completed for: '${ghprbActualCommit}'", \
                //                             "summary": "Harvester Installer Checkout Stage Was Completed - was able to fetch branches etc: '${env.WORKSPACE}'", \
                //                             "text": "harvester-installer checkout completed"}}' https://api.github.com/repos/irishgordo/harvester-installer/check-runs
                //         '''
                // }
            }
        }
        stage('Pull inital Settings.YML Directly'){
            steps{
                script{
                    // TODO: temporary logic pending #45 gets merged in and all
                    sh "curl https://raw.githubusercontent.com/irishgordo/ipxe-examples/feat/2096-air-gapped-rancher-single-node-proof-of-concept/vagrant-pxe-harvester/settings.yml > /tmp/inital_settings.yml"
                    // if (params.settings_yaml_url_override != 'DEFAULT'){
                    //     sh "curl ${params['settings_yaml_url_override']} > /tmp/inital_settings.yml"
                    // }else{
                    //     sh "curl https://raw.githubusercontent.com/harvester/ipxe-examples/main/vagrant-pxe-harvester/settings.yml > /tmp/inital_settings.yml"
                    // }
                }
                // withCredentials([usernamePassword(credentialsId: 'jenkins-irishgordo-team-harvester',
                //                                 usernameVariable: 'GITHUB_APP',
                //                                 passwordVariable: 'GITHUB_ACCESS_TOKEN')]) {
                //     publishChecks name: 'example', title: 'Pipeline Check', summary: 'check through pipeline',
                //         text: 'you can publish checks in pipeline script',
                //         detailsURL: 'https://github.com/jenkinsci/checks-api-plugin#pipeline-usage',
                //         actions: [[label:'an-user-request-action', description:'actions allow users to request pre-defined behaviours', identifier:'an unique identifier']]
                        // sh '''
                        // curl -X POST -H "Content-Type: application/json" \
                        //     -H "Accept: application/vnd.github.antiope-preview+json" \
                        //     -H "authorization: Bearer ${GITHUB_ACCESS_TOKEN}" \
                        //     -d '{ "name": "pull_initial_yaml_settings_for_vagrant_installer", \
                        //         "head_sha": "'${ghprbActualCommit}'", \
                        //         "status": "completed", \
                        //         "conclusion": "success", \
                        //         "output": { "title": "Pulled down inital yaml settings from ipxe-examples for: '${ghprbActualCommit}'", \
                        //                     "summary": "Pulled down the settings.yml and will overwrite if needed to change versions etc.: '${env.WORKSPACE}'", \
                        //                     "text": "inital yaml settings pulled down completed"}}' https://api.github.com/repos/irishgordo/harvester-installer/check-runs
                        // '''
//                }
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
                }       
                // withCredentials([usernamePassword(credentialsId: 'jenkins-irishgordo-team-harvester',
                //                                 usernameVariable: 'GITHUB_APP_C',
                //                                 passwordVariable: 'GITHUB_ACCESS_TOKEN_C')]) {
                //         sh '''
                //         curl -X POST -H "Content-Type: application/json" \
                //             -H "Accept: application/vnd.github.antiope-preview+json" \
                //             -H "authorization: Bearer ${GITHUB_ACCESS_TOKEN_C}" \
                //             -d '{ "name": "base_settings_yaml_adjusted", \
                //                 "head_sha": "'${ghprbActualCommit}'", \
                //                 "status": "completed", \
                //                 "conclusion": "success", \
                //                 "output": { "title": "base settings yaml has been adjusted for: '${ghprbActualCommit}'", \
                //                             "summary": "base settings yaml has been adjusted either by GitOps or manual rebuild in Jenkins for: '${env.WORKSPACE}'", \
                //                             "text": "'${baseSettingsYaml}'"}}' https://api.github.com/repos/irishgordo/harvester-installer/check-runs
                //         '''
                // } 
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
                // withCredentials([usernamePassword(credentialsId: 'jenkins-irishgordo-team-harvester',
                //                                 usernameVariable: 'GITHUB_APP_D',
                //                                 passwordVariable: 'GITHUB_ACCESS_TOKEN_D')]) {
                //         sh '''
                //         curl -X POST -H "Content-Type: application/json" \
                //             -H "Accept: application/vnd.github.antiope-preview+json" \
                //             -H "authorization: Bearer ${GITHUB_ACCESS_TOKEN_D}" \
                //             -d '{ "name": "built_docker_file", \
                //                 "head_sha": "'${ghprbActualCommit}'", \
                //                 "status": "completed", \
                //                 "conclusion": "success", \
                //                 "output": { "title": "Harvester/Tests Dockerfile Has Been Built For: '${ghprbActualCommit}'", \
                //                             "summary": "The Dockerfile that runs our harvester/tests repo has been built: '${env.WORKSPACE}'", \
                //                             "text": "we used: https://github.com/irishgordo/tests - at branch: feat/2096-Supporting-E2E-CI-Work-For-Harvester-Installer - for building the Dockerfile, if desired you can run this with your own fork of the tests repo and branch"}}' https://api.github.com/repos/irishgordo/harvester-installer/check-runs
                //         '''
                // }
            }
        }
        // stage("Acquire Harvester Installer - TEMPORARY STAGE TESTING ONLY PoC"){
        //     steps{
        //         script{
        //             dir("${params['WORKSPACE']}"){
        //                 sh "git clone https://github.com/irishgordo/harvester-installer.git && cd harvester-installer && git checkout feat/2096-temporary-edits-testing"
        //             }
        //         }
        //     }
        // }
        stage('Test harvester-installer pull request') {
            steps {
                // withCredentials([usernamePassword(credentialsId: 'jenkins-irishgordo-team-harvester',
                //                                 usernameVariable: 'GITHUB_APP_E',
                //                                 passwordVariable: 'GITHUB_ACCESS_TOKEN_E')]) {
                //     sh '''
                //     curl -X POST -H "Content-Type: application/json" \
                //         -H "Accept: application/vnd.github.antiope-preview+json" \
                //         -H "authorization: Bearer ${GITHUB_ACCESS_TOKEN_E}" \
                //         -d '{ "name": "long_provisioning_stage_start", \
                //             "head_sha": "'${ghprbActualCommit}'", \
                //             "status": "completed", \
                //             "conclusion": "success", \
                //             "output": { "title": "Finally Begining Provisioning For: '${ghprbActualCommit}'", \
                //                         "summary": "Begining Provisioning...: '${env.WORKSPACE}'", \
                //                         "text": "begining provisioning..."}}' https://api.github.com/repos/irishgordo/harvester-installer/check-runs
                //     '''
                // }
                script {
                    ansiblePlaybook extras: "-e WORKSPACE=${env.WORKSPACE} -e PR_ID=${ghprbPullId}", playbook: "${env.WORKSPACE}/harvester-installer/ci/run_vagrant_install_test.yml"
                }
                // withCredentials([usernamePassword(credentialsId: 'jenkins-irishgordo-team-harvester',
                //                                 usernameVariable: 'GITHUB_APP_F',
                //                                 passwordVariable: 'GITHUB_ACCESS_TOKEN_F')]) {
                //     sh '''
                //     curl -X POST -H "Content-Type: application/json" \
                //         -H "Accept: application/vnd.github.antiope-preview+json" \
                //         -H "authorization: Bearer ${GITHUB_ACCESS_TOKEN_F}" \
                //         -d '{ "name": "long_provisioning_stage_finished", \
                //             "head_sha": "'${ghprbActualCommit}'", \
                //             "status": "completed", \
                //             "conclusion": "success", \
                //             "output": { "title": "Finally Finished Provisioning For: '${ghprbActualCommit}'", \
                //                         "summary": "Finished Provisioning...: '${env.WORKSPACE}'", \
                //                         "text": "finished provisioning..."}}' https://api.github.com/repos/irishgordo/harvester-installer/check-runs
                //     '''
                // }
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
                            // TODO: Fix! Some tests are breaking...but pending those tests being fixed tenitively
                            // TODO: Terraform, the way we load the binary doesn't seem to work for this pipeline... debug/troubleshooting needed to figure it out 
                            // TODO: Ignored tests are hardcoded - not very resilient to if the name change of tests took place, but pipeline interation can take place so hopefully minimizes impact 
                            sh "cd tests && pytest -k 'not verify_host_maintenance_mode and not host_mgmt_maintenance_mode and not host_reboot_maintenance_mode and not host_poweroff_state and not create_images_using_terraform and not create_keypairs_using_terraform and not create_edit_network and not create_network_using_terraform and not create_volume_using_terraform' --junitxml=result_harvester_api_latest.xml -m 'not delete_host' harvester_e2e_tests/apis --username ${baseSettingsYaml.harvester_dashboard.admin_user} --password ${baseSettingsYaml.harvester_dashboard.admin_password} --endpoint https://${baseSettingsYaml.harvester_network_config.vip.ip} --harvester_cluster_nodes ${baseSettingsYaml.harvester_cluster_nodes}"
                        }
                    } catch (err) {
                        echo "failed within docker image inside command on harvester testing api stage"
                        echo err.getMessage()
                    }
                }
                withChecks('Harvester API Tests'){
                    junit checksName: 'harvester-api-tests', testResults: 'tests/result_harvester_api_latest.xml'
                    //junit 'tests/result_harvester_api_latest.xml'
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
                withChecks('Harvester Integration Tests With Rancher'){
                    junit 'tests/result_harvester_rancher_integration_latest.xml'
                }
            }
        }        
    }
    post {
        // Clean after build
        always {
            dir("${env.WORKSPACE}"){
                // script{
                //     try{
                //         junit 'tests/result_harvester_api_latest.xml'
                //     } catch(err) {
                //         echo err.getMessage()
                //     }
                //     try{
                //         junit 'tests/result_harvester_rancher_integration_latest.xml'
                //     } catch(err) {
                //         echo err.getMessage()
                //     }
                //     // try{
                //     //     sh "echo '' > /var/lib/jenkins/.ssh/known_hosts"
                //     // } catch(err) {
                //     //     echo err.getMessage()                    
                //     // }
                // }
                sh "cd ipxe-examples/vagrant-pxe-harvester/ && ls -alh && vagrant destroy -f"
                sh "cd ${env.WORKSPACE}/tests && make destroy-docker-e2e-ci-image"
                sh "cd ${env.WORKSPACE} && rm -rfv tests"
                deleteDir()
            }
            cleanWs()
        }
    }
}