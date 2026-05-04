def call(Map config = [:]) {
    container('kaniko') {
        sh """
            /kaniko/executor \
              --context=${config.context ?: '.'} \
              --dockerfile=${config.dockerfile ?: 'Dockerfile'} \
              --destination=${config.image}:${env.BUILD_NUMBER} \
              --destination=${config.image}:latest
        """
    }
}
