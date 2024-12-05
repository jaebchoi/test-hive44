def gitId = 'github'
def gitRepo = 'https://github.com/jaebchoi/test-hive44'
// Set branch in the Jenkins Job or set here if not variable
def gitBranch = 'refs/heads/${branch}'
def projectDirectory = 'test-hive44'
def jenkinsSteps
// Set URL of ArgoCD server
def argocdUrl = ''
def argocdAppName = 'test-hive44'
def argocdNamespace = 'argocd'
def argocdDestinationServer = 'https://kubernetes.default.svc'
def argocdBranch = '${branch}'
def argocdSyncLabel = 'argocd.argoproj.io/instance=test-hive44'

node('master') {
    dir(projectDirectory) {
        checkout([$class: 'GitSCM', branches: [[name: gitBranch]], userRemoteConfigs: [[credentialsId: gitId, url: gitRepo]]])
        jenkinsSteps = load 'devops/jenkinsPipelineSteps.groovy'
    }
}

stage("Authenticate") {
    node('master') {
        withCredentials([string(credentialsId: 'argocdPassword', variable: 'argocdPassword')]) {
            try {
                dir(projectDirectory) {
                    slackSend color: "warning",
                            message: "test-hive44 authenticating"
                    jenkinsSteps.argocdAuthenticate(argocdUrl, '${argocdPassword}')
                }
            } catch (err) {
                slackSend color: "danger",
                        message: "test-hive44 failed to authenticate properly"
                throw err
            }
        }
    }
}

stage("Deploy") {
    node('master') {
        try {
            dir(projectDirectory) {
                slackSend color: "warning",
                        message: "test-hive44 deploying"
                jenkinsSteps.argocdDeploy(argocdAppName, argocdUrl, argocdDestinationServer, gitRepo, argocdBranch,
                        argocdNamespace, ['values.yaml'])
                slackSend color: "good",
                        message: "test-hive44 deployed successfully"
            }
        } catch (err) {
            slackSend color: "danger",
                    message: "test-hive44 failed to deploy properly"
            throw err
        }
    }
}

stage("Running") {
    node('master') {
        try {
            timeout(environmentUptime) {
                input message: 'Ready to kill the environment?', ok: 'Kill Test Server'
            }
        } catch (err) {}
    }
}

stage("Teardown") {
    node('master') {
        try {
            dir(projectDirectory) {
                slackSend color: "warning",
                        message: "test-hive44 shutting down"
                jenkinsSteps.argocdTerminate(argocdAppName)
            }
        } catch (err) {
            slackSend color: "danger",
                    message: "test-hive44 failed to shut down properly"
            throw err
        }
    }
}

