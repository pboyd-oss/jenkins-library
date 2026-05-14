def call(Map config = [:]) {
    def environment = config.environment ?: 'dev'

    platformArchive()
    platformSign()
    platformBuildProvenance()
    platformDeploy(profile: environment)
}
