// Hook points available in config:
//   preCheckout, postCheckout
//   preTest, test, postTest       (stage skipped if test not provided)
//   preBuild, build, postBuild
//   preRelease, postRelease
//   environment                   (string: 'dev' | 'test' | 'production')
def call(Map config = [:]) {
    pipeline {
        agent {
            kubernetes {
                inheritFrom 'skaffold'
                idleMinutes 10
            }
        }
        stages {
            stage('Checkout') {
                steps {
                    script {
                        if (config.preCheckout) config.preCheckout()
                        checkout scm
                        if (config.postCheckout) config.postCheckout()
                    }
                }
            }
            stage('Test') {
                when {
                    expression { config.test != null }
                }
                steps {
                    script {
                        if (config.preTest) config.preTest()
                        config.test()
                        if (config.postTest) config.postTest()
                    }
                }
            }
            stage('Build') {
                steps {
                    script {
                        if (config.preBuild) config.preBuild()
                        if (config.build) {
                            config.build()
                        } else {
                            buildApp()
                        }
                        if (config.postBuild) config.postBuild()
                    }
                }
            }
            stage('Archive') {
                steps {
                    script { platformArchive(fromSkaffold: true) }
                }
            }
            stage('Sign') {
                steps {
                    script { platformSign() }
                }
            }
            stage('Provenance') {
                steps {
                    script { platformBuildProvenance(simple: true) }
                }
            }
            stage('Release') {
                steps {
                    script {
                        if (config.preRelease) config.preRelease()
                        platformDeploy(profile: config.environment ?: 'dev')
                        if (config.postRelease) config.postRelease()
                    }
                }
            }
        }
    }
}
