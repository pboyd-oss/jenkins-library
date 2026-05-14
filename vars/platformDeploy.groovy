def call(Map config = [:]) {
    container('deploy-sec-base') {
        sh '''
            skaffold render --build-artifacts=artifacts.json --output=rendered.yaml
            skaffold apply rendered.yaml
        '''
    }
}
