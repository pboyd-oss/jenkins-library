def call(Map config = [:]) {
    def artifacts = readJSON file: 'artifacts.json'

    def cosignVersion = config.cosignVersion ?: '2.5.2'
    def syftVersion = config.syftVersion ?: '1.21.0'

    artifacts.builds.each { build ->
        def imageRef = build.tag

        container('skaffold') {
            sh """
                curl -sL https://github.com/sigstore/cosign/releases/download/v${cosignVersion}/cosign-linux-amd64 -o /tmp/cosign
                chmod +x /tmp/cosign
                curl -sL https://github.com/anchore/syft/releases/download/v${syftVersion}/syft_${syftVersion}_linux_amd64.tar.gz | tar -xz -C /tmp syft
                chmod +x /tmp/syft
            """

            sh "/tmp/syft '${imageRef}' -o spdx-json > sbom.json"

            writeJSON file: 'provenance.json', json: [
                builder: [id: 'https://jenkins.tuxgrid.com'],
                buildType: 'https://tuxgrid.com/jenkins/build/v1',
                invocation: [
                    configSource: [
                        uri: env.GIT_URL ?: '',
                        digest: [sha1: env.GIT_COMMIT ?: ''],
                        entryPoint: 'Jenkinsfile'
                    ]
                ],
                metadata: [
                    buildInvocationId: env.BUILD_URL ?: '',
                    completeness: [parameters: false, environment: false, materials: false],
                    reproducible: false
                ],
                materials: [[
                    uri: env.GIT_URL ?: '',
                    digest: [sha1: env.GIT_COMMIT ?: '']
                ]]
            ]

            sh "COSIGN_PASSWORD='' DOCKER_CONFIG=/root/.docker /tmp/cosign sign --key /cosign-key/cosign.key --tlog-upload=false '${imageRef}'"
            sh "COSIGN_PASSWORD='' DOCKER_CONFIG=/root/.docker /tmp/cosign attest --key /cosign-key/cosign.key --tlog-upload=false --predicate sbom.json --type spdx '${imageRef}'"
            sh "COSIGN_PASSWORD='' DOCKER_CONFIG=/root/.docker /tmp/cosign attest --key /cosign-key/cosign.key --tlog-upload=false --predicate provenance.json --type slsaprovenance '${imageRef}'"
        }
    }
}
