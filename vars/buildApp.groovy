// Registry credential resolution order:
//   1. config.credentialsId (explicit override)
//   2. TUXGRID_REGISTRY_CREDENTIALS_ID (baked in by seed job per team)
//   3. 'gitea-registry' (global fallback for legacy jobs)
def call(Map config = [:]) {
    def credId   = config.credentialsId ?: env.TUXGRID_REGISTRY_CREDENTIALS_ID ?: 'gitea-registry'
    def registry = config.registry      ?: env.TUXGRID_REGISTRY_URL             ?: 'gitea.tuxgrid.com'

    withCredentials([usernamePassword(
        credentialsId:    credId,
        usernameVariable: 'REG_USER',
        passwordVariable: 'REG_PASS'
    )]) {
        withEnv(["DOCKER_CONFIG=${env.WORKSPACE}/.docker"]) {
            container('skaffold') {
                sh """
                    mkdir -p \$DOCKER_CONFIG
                    AUTH=\$(printf '%s:%s' "\$REG_USER" "\$REG_PASS" | base64 | tr -d '\\n')
                    printf '{"auths":{"${registry}":{"auth":"%s"}}}' "\$AUTH" > "\$DOCKER_CONFIG/config.json"
                    cp "\$DOCKER_CONFIG/config.json" /tmp/kaniko-config.json
                    skaffold build --file-output=artifacts.json
                """
            }
        }
    }
}
