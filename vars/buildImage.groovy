def call(Map config = [:]) {
    withCredentials([usernamePassword(
        credentialsId: 'gitea-registry',
        usernameVariable: 'REG_USER',
        passwordVariable: 'REG_PASS'
    )]) {
        container('kaniko') {
            def image = config.image ?: "${env.REGISTRY}/${config.name}"
            sh """
                /kaniko/executor \
                  --context=${config.context ?: '.'} \
                  --dockerfile=${config.dockerfile ?: 'Dockerfile'} \
                  --registry-username=\$REG_USER \
                  --registry-password=\$REG_PASS \
                  --destination=${image}:${env.BUILD_NUMBER} \
                  --destination=${image}:latest
            """
        }
    }
}
