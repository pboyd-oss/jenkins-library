def call() {
    build(
        job:        "platform/${env.TUXGRID_TEAM_SLUG}/scan",
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
