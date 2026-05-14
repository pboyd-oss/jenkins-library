def call(Map config = [:]) {
    def profile = config.profile ?: 'dev'

    container('deploy-sec-base') {
        sh """
            skaffold render --build-artifacts=artifacts.json --profile=${profile} --output=rendered.yaml
            skaffold apply rendered.yaml
        """
    }
}
