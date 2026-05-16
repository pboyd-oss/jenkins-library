def call(Map config = [:]) {
    def promoteTo      = config.promoteTo      ?: 'latest'
    def signingContainer = config.container   ?: 'deploy-sec-base'

    if (!env.IMAGE?.startsWith('harbor.tuxgrid.com/')) {
        echo "platformPromote: skipping — image '${env.IMAGE}' is not in harbor.tuxgrid.com"
        return
    }

    def imageRepo = env.IMAGE.replaceFirst('harbor\\.tuxgrid\\.com/', '')

    container(signingContainer) {
        withCredentials([usernamePassword(
            credentialsId:    'harbor-robot-platform',
            usernameVariable: 'HARBOR_USER',
            passwordVariable: 'HARBOR_PASS')]) {
            withEnv(["IMAGE_REPO=${imageRepo}", "DIGEST=${env.IMAGE_DIGEST}", "PROMOTE_TO=${promoteTo}"]) {
                sh '''
                    AUTH=$(printf '%s:%s' "${HARBOR_USER}" "${HARBOR_PASS}" | base64 | tr -d '\n')
                    BASE="https://harbor.tuxgrid.com/v2/${IMAGE_REPO}/manifests"
                    ACCEPT="application/vnd.oci.image.manifest.v1+json,application/vnd.docker.distribution.manifest.v2+json"

                    # Fetch manifest and content-type from the signed digest
                    curl -sf \
                        -H "Authorization: Basic ${AUTH}" \
                        -H "Accept: ${ACCEPT}" \
                        "${BASE}/${DIGEST}" > /tmp/manifest.json

                    CTYPE=$(curl -sf -I \
                        -H "Authorization: Basic ${AUTH}" \
                        -H "Accept: ${ACCEPT}" \
                        "${BASE}/${DIGEST}" \
                        | grep -i '^content-type:' | awk '{print $2}' | tr -d '\r')

                    # PUT manifest to promotion tag — no layer re-upload needed
                    curl -sf -X PUT \
                        -H "Authorization: Basic ${AUTH}" \
                        -H "Content-Type: ${CTYPE}" \
                        --data-binary @/tmp/manifest.json \
                        "${BASE}/${PROMOTE_TO}"

                    rm -f /tmp/manifest.json
                '''
            }
        }
    }

    echo "Promoted ${env.IMAGE}@${env.IMAGE_DIGEST} → ${env.IMAGE}:${promoteTo}"
}
