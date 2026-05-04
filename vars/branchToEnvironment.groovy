def call(String branch) {
    switch (branch) {
        case 'main':    return 'production'
        case 'staging': return 'test'
        default:        return 'dev'
    }
}
