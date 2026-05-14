def call(Map config = [:]) {
    def includeDeps = config.containsKey('includeDeps') ? config.includeDeps : true

    if (includeDeps) {
        container('kaniko') {
            sh '''
                cp /mitm-data/deps.ndjson ${WORKSPACE}/deps.ndjson 2>/dev/null \
                    || printf '[]' > ${WORKSPACE}/deps.ndjson
            '''
        }
    } else {
        writeFile file: 'deps.ndjson', text: '[]'
    }

    env.IMAGE_DIGEST = readFile("${env.WORKSPACE}/image.digest").trim()
    writeJSON file: 'artifacts.json', json: [
        builds: [[imageName: env.IMAGE, tag: "${env.IMAGE}@${env.IMAGE_DIGEST}", number: env.BUILD_NUMBER]]
    ]
    archiveArtifacts artifacts: 'artifacts.json,deps.ndjson', fingerprint: true
}
