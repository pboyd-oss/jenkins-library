def call(Map config = [:]) {
    container('skaffold') {
        sh 'skaffold build --file-output=artifacts.json && chmod 666 artifacts.json'
        archiveArtifacts artifacts: 'artifacts.json', fingerprint: true
    }
}
