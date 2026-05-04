def call(Map config = [:]) {
    container('kaniko') {
        sh """
            /kaniko/executor \
              --context=${config.context ?: '.'} \
              --dockerfile=${config.dockerfile ?: 'Dockerfile'} \
              --docker-cfg=/kaniko/.docker \
              --destination=${config.image ?: 'gitea.tuxgrid.com/pboyd/' + config.name}:${env.BUILD_NUMBER} \
              --destination=${config.image ?: 'gitea.tuxgrid.com/pboyd/' + config.name}:latest
        """
    }
}
