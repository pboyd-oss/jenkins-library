def call(Map config = [:]) {
    def refs = sh(
        script: "python3 -c \"import json; [print(b['tag']) for b in json.load(open('artifacts.json'))['builds']]\"",
        returnStdout: true
    ).trim().split('\n') as List

    refs.each { imageRef ->
        container('syft') {
            sh "syft '${imageRef}' -o spdx-json > sbom.json"
        }

        container('cosign') {
            writeFile file: 'provenance.json', text: """{
  "builder": {"id": "https://jenkins.tuxgrid.com"},
  "buildType": "https://tuxgrid.com/jenkins/build/v1",
  "invocation": {
    "configSource": {
      "uri": "${env.GIT_URL ?: ''}",
      "digest": {"sha1": "${env.GIT_COMMIT ?: ''}"},
      "entryPoint": "Jenkinsfile"
    }
  },
  "metadata": {
    "buildInvocationId": "${env.BUILD_URL ?: ''}",
    "completeness": {"parameters": false, "environment": false, "materials": false},
    "reproducible": false
  },
  "materials": [{"uri": "${env.GIT_URL ?: ''}", "digest": {"sha1": "${env.GIT_COMMIT ?: ''}"}}]
}"""

            withEnv(['COSIGN_PASSWORD=']) {
                sh "cosign sign --key /cosign-key/cosign.key --no-tlog-upload '${imageRef}'"
                sh "cosign attest --key /cosign-key/cosign.key --no-tlog-upload --predicate sbom.json --type spdx '${imageRef}'"
                sh "cosign attest --key /cosign-key/cosign.key --no-tlog-upload --predicate provenance.json --type slsaprovenance '${imageRef}'"
            }
        }
    }
}
