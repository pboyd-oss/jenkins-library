// Hook points available in config:
//   preCheckout, postCheckout
//   preTest, test, postTest       (stage skipped if test not provided)
//   preBuild, build, postBuild    (stage skipped if build not provided)
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
                when {
                    expression { config.build != null }
                }
                steps {
                    script {
                        if (config.preBuild) config.preBuild()
                        config.build()
                        if (config.postBuild) config.postBuild()
                    }
                }
            }
            stage('Scan') {
                // Triggers the platform-controlled scan job on platform infrastructure.
                // Runs Trivy + Checkov in digest-pinned containers, signs scan/v1 attestation.
                // Fails fast here if scans fail — teams see the failure immediately.
                when {
                    expression { config.build != null }
                }
                steps {
                    script {
                        build(
                            job: "platform/${env.TUXGRID_TEAM_SLUG}/scan",
                            parameters: [
                                string(name: 'UPSTREAM_JOB',   value: env.JOB_NAME),
                                string(name: 'UPSTREAM_BUILD', value: env.BUILD_NUMBER),
                                string(name: 'GIT_URL',        value: env.GIT_URL ?: ''),
                                string(name: 'GIT_COMMIT',     value: env.GIT_COMMIT ?: ''),
                            ],
                            wait:      true,
                            propagate: true
                        )
                    }
                }
            }
            stage('Release') {
                steps {
                    script {
                        if (config.preRelease) config.preRelease()
                        releaseApp(environment: config.environment ?: 'dev')
                        if (config.postRelease) config.postRelease()
                    }
                }
            }
        }
    }
}
