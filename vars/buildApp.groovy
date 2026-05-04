def call(Map config = [:]) {
    withCredentials([usernamePassword(
        credentialsId: config.credentialsId ?: 'gitea-registry',
        usernameVariable: 'REG_USER',
        passwordVariable: 'REG_PASS'
    )]) {
        withEnv(["DOCKER_CONFIG=${env.WORKSPACE}/.docker"]) {
            container('skaffold') {
                sh """
                    mkdir -p \$DOCKER_CONFIG
                    AUTH=\$(printf '%s:%s' "\$REG_USER" "\$REG_PASS" | base64 | tr -d '\\n')
                    printf '{"auths":{"${config.registry ?: 'gitea.tuxgrid.com'}":{"auth":"%s"}}}' "\$AUTH" > "\$DOCKER_CONFIG/config.json"
                    skaffold build --file-output=artifacts.json
                """
            }
        }
    }
}
