def call(Map config = [:]) {
    def signingContainer = config.container ?: 'deploy-sec-base'

    if (!env.IMAGE?.startsWith('harbor.tuxgrid.com/')) {
        echo "platformSign: skipping — image '${env.IMAGE}' is not in harbor.tuxgrid.com"
        return
    }

    container(signingContainer) {
        withCredentials([
            string(credentialsId: 'cosign-key', variable: 'COSIGN_PRIVATE_KEY'),
            usernamePassword(
                credentialsId: 'harbor-robot-platform',
                usernameVariable: 'HARBOR_USER',
                passwordVariable: 'HARBOR_PASS'),
        ]) {
            sh '''
                printf '%s' "${COSIGN_PRIVATE_KEY}" > /tmp/cosign.key
                chmod 600 /tmp/cosign.key
                AUTH=$(printf '%s:%s' "${HARBOR_USER}" "${HARBOR_PASS}" | base64 | tr -d '\n')
                mkdir -p /tmp/.docker
                printf '{"auths":{"harbor.tuxgrid.com":{"auth":"%s"}}}' "${AUTH}" \
                    > /tmp/.docker/config.json
                DOCKER_CONFIG=/tmp/.docker COSIGN_PASSWORD="" cosign sign --key /tmp/cosign.key --yes \
                    "${IMAGE}@${IMAGE_DIGEST}"
                rm -f /tmp/cosign.key /tmp/.docker/config.json
            '''
        }
    }
}
