def call(Map config = [:]) {
    container('skaffold') {
        sh "skaffold run"
    }
}
