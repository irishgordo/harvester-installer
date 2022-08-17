pipelineJob('harvester-vagrant-installation-test') {
    description('Test installation of Harvester cluster in a virtual (Vagrant) environment')
    properties {
        githubProjectUrl('https://github.com/irishgordo/harvester-installer')
    }
    logRotator {
      numToKeep(-1)
      daysToKeep(10)
    }
    parameters {
        booleanParam('keep_environment', false, 'Keep the Vagrant environment around after the test had finish.')
        booleanParam('slack_notify', false, 'Send notifications to Slack')
        stringParam('WORKSPACE', '/var/lib/jenkins/workspace/harvester-vagrant-installation-test', 'Required: Provide A Workspace')
        // harvester params
        stringParam('harvester_cluster_nodes', 'YAML_PROVIDED', 'Override the YAML Provided harvester_cluster_nodes')
        // single node rancher params
        stringParam('rancher_config_node_disk_size', 'YAML_PROVIDED', 'Override the YAML Provided rancher_config.node_disk_size - must be in appropriate runtime_configuration')
        stringParam('rancher_config_cert_manager_version', 'YAML_PROVIDED', 'Override the YAML Provided rancher_config.cert_manager_version - must be in appropriate runtime_configuration')
        stringParam('rancher_config_k3s_url_escaped_version', 'YAML_PROVIDED', 'Override the YAML Provided rancher_config.k3s_url_escaped_version - must be in appropriate runtime_configuration')
        stringParam('rancher_config_rancher_version', 'YAML_PROVIDED', 'Override the YAML Provided rancher_config.rancher_version - must be in appropriate runtime_configuration')
        stringParam('rancher_config_rancher_version_no_prefix', 'YAML_PROVIDED', 'Override the YAML Provided rancher_config.rancher_version_no_prefix - must be in appropriate runtime_configuration')
        stringParam('rancher_config_cpu', 'YAML_PROVIDED', 'Override the YAML Provded rancher_config.cpu - must be in appropriate runtime_configuration')
        stringParam('rancher_config_memory', 'YAML_PROVIDED', 'Override the YAML Provded rancher_config.memory - must be in the appropriate runtime_configuration')
        stringParam('rancher_config_rancher_install_domain', 'YAML_PROVIDED', 'OVerride the YAML Provided rancher_config.rancher_install_domain - must be in appropriate runtime_configuration')
        stringParam('rancher_config_registry_domain', 'YAML_PROVIDED', 'Override the YAML Provided rancher_config.registry_domain - must be in the appropriate runtime_configuration')
        // toggle of environments
        choiceParam('runtime_configuration', ['air_gap_single_rancher_with_harvester', 'harvester_offline', 'normal', 'single_node_rancher_non_airgapped'], 'the runtime for the harvester install')
        // login params:
        stringParam('harvester_dashboard_admin_user', 'YAML_PROVIDED', 'Override the YAML provided harvester_dashbaord.admin_user')
        stringParam('harvester_dashboard_admin_password', 'YAML_PROVIDED', 'Override the default YAML provided harvester_dashboard.admin_password')
        stringParam('rancher_config_bootstrap_password', 'YAML_PROVIDED', 'OVerride the default YAML Provided rancher password')
        // settings.yaml override
        stringParam('settings_yaml_url_override', 'DEFAULT', 'overrides fetching settings.yml raw github url')
        // tests repo info
        stringParam('tests_repo_url', 'DEFAULT', 'overrides the default test repo for e2e tests')
        stringParam('tests_repo_branch', 'DEFAULT', 'overrides the default test repo branch for e2e tests')
    }
    // definition {
    //     cpsScm {
    //         scm {
    //             git {
    //                 remote {
    //                     url('https://github.com/irishgordo/harvester-installer.git')
    //                     refspec('+refs/pull/*:refs/remotes/origin/pr/*')
    //                 }
    //                 branch('${sha1}')
    //             }
    //         }
    //     }
    // }
    triggers {
        githubPullRequest {
            admin('irishgordo')
            admins(['irishgordo'])
            orgWhitelist('harvester')
            cron('')
            triggerPhrase('.*(re)?run-tests-.*')
            useGitHubHooks(true)
            permitAll(true)
            displayBuildErrorsOnDownstreamBuilds(true)
            allowMembersOfWhitelistedOrgsAsAdmin(true)
            extensions {
                commitStatus {
                    context('Vagrant installation testing')
                    triggeredStatus('Started Vagrant installation testing...')
                    completedStatus('SUCCESS', 'All good!')
                    completedStatus('FAILURE', 'Failed!')
                    completedStatus('PENDING', 'Still working on it...')
                    completedStatus('ERROR', 'Something went wrong. Need investigation.')
                }
            }
        }
    }
    definition {
        cps {
            script(readFileFromWorkspace('jobs/ci_cd_job_pipeline.groovy'))
            sandbox()
        }
    }
}