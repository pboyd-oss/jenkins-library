def call(Map config = [:]) {
    container('golang') {
        def services = config.services ?: []
        services.each { service ->
            sh "cd ${service} && go test ./..."
        }
    }
}
