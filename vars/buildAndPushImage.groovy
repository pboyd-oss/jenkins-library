def call(Map config = [:]) {
    if (!config.image) error('buildAndPushImage: image is required')
    def image         = config.image
    def tag           = config.tag          ?: env.GIT_COMMIT?.take(7) ?: 'dev'
    def dockerfile    = config.dockerfile   ?: 'Dockerfile'
    def context       = config.context      ?: "dir://${env.WORKSPACE}"
    def credentialsId = config.credentialsId ?: 'harbor-robot-platform'

    withCredentials([usernamePassword(
        credentialsId: credentialsId,
        usernameVariable: 'HARBOR_USER',
        passwordVariable: 'HARBOR_TOKEN'
    )]) {
        container('kaniko') {
            sh '''
                mkdir -p /tmp/.docker
                printf '{"auths":{"harbor.tuxgrid.com":{"auth":"%s"}}}' \
                    "$(printf '%s:%s' "$HARBOR_USER" "$HARBOR_TOKEN" | base64 -w0)" \
                    > /tmp/.docker/config.json
            '''
            sh """
                DOCKER_CONFIG=/tmp/.docker /kaniko/executor \
                    --context=${context} \
                    --dockerfile=${dockerfile} \
                    --destination=${image}:${tag} \
                    --destination=${image}:latest \
                    --cache=true \
                    --cache-repo=${image}/cache
            """
        }
    }
}
