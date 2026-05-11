def call(Map config = [:]) {
    def services = config.services ?: []
    def results  = []

    sh 'mkdir -p test-results'

    container('golang') {
        services.each { svc ->
            def exitCode = sh(script: "cd ${svc} && go test ./...", returnStatus: true)
            results << [name: svc, passed: exitCode == 0]
        }
    }

    def failures = results.count { !it.passed }
    def xml = '<?xml version="1.0" encoding="UTF-8"?>\n<testsuites>'
    results.each { r ->
        def failTag = r.passed ? '' : '<failure message="go test ./... failed"/>'
        xml += "\n  <testsuite name=\"${r.name}\" tests=\"1\" failures=\"${r.passed ? 0 : 1}\">" +
               "\n    <testcase classname=\"${r.name}\" name=\"go test\">${failTag}</testcase>" +
               "\n  </testsuite>"
    }
    xml += '\n</testsuites>'

    writeFile file: 'test-results/go-tests.xml', text: xml

    if (failures > 0) error("${failures} service(s) failed: go test ./...")
}
