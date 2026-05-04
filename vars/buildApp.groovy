def call(Map config = [:]) {
    withCredentials([usernamePassword(
        credentialsId: 'gitea-registry',
        usernameVariable: 'REG_USER',
        passwordVariable: 'REG_PASS'
    )]) {
        withEnv(["DOCKER_CONFIG=${env.WORKSPACE}/.docker"]) {
            container('skaffold') {
                sh '''
                    echo "$REG_PASS" | docker login -u "$REG_USER" --password-stdin $REGISTRY
                    skaffold build --file-output=artifacts.json
                '''
            }
        }
    }
}
