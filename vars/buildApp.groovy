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
                    echo "DEBUG: registry user=\$REG_USER"
                    curl -s -o /dev/null -w "DEBUG: registry token exchange HTTP %{http_code}\\n" -u "\$REG_USER:\$REG_PASS" "https://gitea.tuxgrid.com/v2/token?service=container_registry&scope=repository:pboyd/dummy-nginx:push,pull"
                    skaffold build --file-output=artifacts.json
                """
            }
        }
    }
}
