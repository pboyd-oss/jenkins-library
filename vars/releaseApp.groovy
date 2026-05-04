def call(Map config = [:]) {
    container('skaffold') {
        sh "skaffold render --profile=${config.environment ?: 'dev'} --build-artifacts=artifacts.json --output=rendered.yaml"
        stash name: 'rendered-manifests', includes: 'rendered.yaml'
    }

    podTemplate(
        serviceAccount: 'deploy-sa',
        containers: [
            containerTemplate(name: 'skaffold', image: 'gcr.io/k8s-skaffold/skaffold:v2.15.0', command: 'cat', ttyEnabled: true)
        ]
    ) {
        node(POD_LABEL) {
            unstash 'rendered-manifests'
            container('skaffold') {
                sh 'skaffold apply rendered.yaml'
            }
        }
    }
}
