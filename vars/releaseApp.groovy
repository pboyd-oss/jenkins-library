def call(Map config = [:]) {
    container('skaffold') {
        sh "skaffold render --profile=${config.environment ?: 'dev'} --digest-source=remote --output=rendered.yaml"
        sh "kubectl apply -f rendered.yaml"
    }
}
