def call(Map config = [:]) {
    def tag            = config.tag            ?: env.BUILD_NUMBER
    def dockerfile     = config.dockerfile     ?: 'Dockerfile'
    def extraBuildArgs = config.extraBuildArgs ?: []
    def useCache       = config.cache          ?: false
    def cacheRepo      = config.cacheRepo      ?: ''

    def extraArgFlags = extraBuildArgs.collect { "--build-arg ${it}=\${${it}}" }.join(" \\\n                        ")
    def cacheFlags    = (useCache && cacheRepo) ?
        "--cache=true \\\n                        --snapshot-mode=redo \\\n                        --compressed-caching=false \\\n                        --cache-repo=${cacheRepo}" : ''

    container('kaniko') {
        withCredentials([usernamePassword(
            credentialsId:    'harbor-robot-platform',
            usernameVariable: 'HARBOR_USER',
            passwordVariable: 'HARBOR_PASS')]) {
            sh """
                mkdir -p /kaniko/.docker
                AUTH=\$(printf '%s:%s' "\${HARBOR_USER}" "\${HARBOR_PASS}" | base64 | tr -d '\\n')
                printf '{"auths":{"harbor.tuxgrid.com":{"auth":"%s"}}}' "\${AUTH}" \\
                    > /kaniko/.docker/config.json
                PLATFORM_CA_B64=\$(base64 -w0 /mitm-data/ca.pem 2>/dev/null || true)
                /kaniko/executor \\
                    --context=dir://. \\
                    --dockerfile=${dockerfile} \\
                    --build-arg "PLATFORM_CA_B64=\${PLATFORM_CA_B64}" \\
                    --build-arg HTTPS_PROXY=http://127.0.0.1:8080 \\
                    --build-arg HTTP_PROXY=http://127.0.0.1:8080 \\
                    ${extraArgFlags} \\
                    --destination=${env.IMAGE}:${tag} \\
                    --destination=${env.IMAGE}:latest \\
                    --digest-file=${env.WORKSPACE}/image.digest \\
                    ${cacheFlags}
            """
        }
    }
}
