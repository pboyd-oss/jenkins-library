def call(Map config = [:]) {
    def includeDeps  = config.containsKey('includeDeps') ? config.includeDeps : true
    def fromSkaffold = config.fromSkaffold ?: false

    if (fromSkaffold) {
        writeFile file: 'deps.ndjson', text: '[]'
        // Reuse artifacts.json already written by buildApp() (skaffold build --file-output)
        // Extract imageName and digest from the first build entry.
        def artifacts = readJSON file: "${env.WORKSPACE}/artifacts.json"
        def b = artifacts.builds[0]
        // b.tag is "harbor.tuxgrid.com/platform/foo:3@sha256:abc..." or "...sha256-abc..."
        def tag = b.tag
        def atIdx = tag.indexOf('@sha256:')
        if (atIdx < 0) error("platformArchive: could not find @sha256: in skaffold tag: ${tag}")
        env.IMAGE         = b.imageName
        env.IMAGE_DIGEST  = tag.substring(atIdx + 1)  // "sha256:abc..."
        // Rewrite artifacts.json in platform-standard format
        writeJSON file: 'artifacts.json', json: [
            builds: [[imageName: env.IMAGE, tag: "${env.IMAGE}@${env.IMAGE_DIGEST}", number: env.BUILD_NUMBER]]
        ]
    } else {
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
    }
    archiveArtifacts artifacts: 'artifacts.json,deps.ndjson', fingerprint: true
}
