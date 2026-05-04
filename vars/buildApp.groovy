def call(Map config = [:]) {
    container('skaffold') {
        sh 'skaffold build --file-output=artifacts.json'
    }
}
