def call(Map config = [:]) {
    withCredentials([usernamePassword(
        credentialsId: 'gitea-registry',
        usernameVariable: 'REG_USER',
        passwordVariable: 'REG_PASS'
    )]) {
        container('skaffold') {
            sh """
                mkdir -p /tmp/.docker
                echo '{"auths":{"${env.REGISTRY.split('/')[0]}":{"auth":"'"\$(echo -n \$REG_USER:\$REG_PASS | base64)"'"}}}' > /tmp/.docker/config.json
                DOCKER_CONFIG=/tmp/.docker skaffold build --file-output=artifacts.json
            """
        }
    }
}
