def call(Map config = [:]) {
    container('skaffold') {
        sh "skaffold render --profile=${config.environment ?: 'dev'} --build-artifacts=artifacts.json --output=rendered.yaml"
        sh "skaffold apply rendered.yaml"
    }
}
