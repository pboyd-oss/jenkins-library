def call(Map config = [:]) {
    def artifacts = new groovy.json.JsonSlurper().parseText(readFile('artifacts.json'))

    artifacts.builds.each { build ->
        def imageRef = build.tag

        container('syft') {
            sh "syft '${imageRef}' -o spdx-json > sbom.json"
        }

        container('cosign') {
            def provenance = groovy.json.JsonOutput.toJson([
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
            ])
            writeFile file: 'provenance.json', text: provenance

            withEnv(['COSIGN_PASSWORD=']) {
                sh "cosign sign --key /cosign-key/cosign.key --no-tlog-upload '${imageRef}'"
                sh "cosign attest --key /cosign-key/cosign.key --no-tlog-upload --predicate sbom.json --type spdx '${imageRef}'"
                sh "cosign attest --key /cosign-key/cosign.key --no-tlog-upload --predicate provenance.json --type slsaprovenance '${imageRef}'"
            }
        }
    }
}
