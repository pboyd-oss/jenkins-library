def call(Map config = [:]) {
    def imageRef         = config.imageRef  ?: "${env.IMAGE}@${env.IMAGE_DIGEST}"
    def simple           = config.simple    ?: false
    def signingContainer = config.container ?: 'deploy-sec-base'

    if (!env.IMAGE?.startsWith('harbor.tuxgrid.com/')) {
        echo "platformBuildProvenance: skipping — image '${env.IMAGE}' is not in harbor.tuxgrid.com"
        return
    }

    if (!imageRef || !imageRef.contains('@sha256:')) {
        error('platformBuildProvenance: imageRef must be a digest reference (IMAGE@sha256:...). Ensure Archive stage sets IMAGE_DIGEST before calling this step.')
    }

    withEnv(["PROVENANCE_IMAGE_REF=${imageRef}"]) {
        container(signingContainer) {
            withCredentials([
                string(credentialsId: 'cosign-key', variable: 'COSIGN_PRIVATE_KEY'),
                usernamePassword(
                    credentialsId: 'harbor-robot-platform',
                    usernameVariable: 'HARBOR_USER',
                    passwordVariable: 'HARBOR_PASS'),
            ]) {
                if (simple) {
                    sh '''
                        printf '%s' "${COSIGN_PRIVATE_KEY}" > /tmp/cosign.key
                        chmod 600 /tmp/cosign.key
                        AUTH=$(printf '%s:%s' "${HARBOR_USER}" "${HARBOR_PASS}" | base64 | tr -d '\\n')
                        mkdir -p /tmp/.docker
                        printf '{"auths":{"harbor.tuxgrid.com":{"auth":"%s"}}}' "${AUTH}" \
                            > /tmp/.docker/config.json

                        NOW=$(date -u +%Y-%m-%dT%H:%M:%SZ)
                        STARTED=$(date -u -d "@$((BUILD_TIMESTAMP / 1000))" +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || echo "${NOW}")
                        cat > /tmp/provenance.json << EOF
{
  "buildDefinition": {
    "buildType": "https://tuxgrid.com/buildType/jenkins-kaniko/v1",
    "externalParameters": {
      "ref": "${GIT_COMMIT}",
      "repository": "${GIT_URL}",
      "dockerfile": "Dockerfile"
    },
    "resolvedDependencies": [
      {"uri": "${GIT_URL}", "digest": {"gitCommit": "${GIT_COMMIT}"}}
    ]
  },
  "runDetails": {
    "builder": {"id": "https://jenkins.tuxgrid.com/job/${JOB_NAME}/${BUILD_NUMBER}"},
    "metadata": {
      "invocationId": "${BUILD_URL}",
      "startedOn": "${STARTED}",
      "finishedOn": "${NOW}"
    }
  }
}
EOF

                        DOCKER_CONFIG=/tmp/.docker COSIGN_PASSWORD="" cosign attest --key /tmp/cosign.key --yes \
                            --type slsaprovenance1 \
                            --predicate /tmp/provenance.json \
                            "${PROVENANCE_IMAGE_REF}"

                        rm -f /tmp/cosign.key /tmp/.docker/config.json /tmp/provenance.json
                    '''
                } else {
                    sh '''
                        printf '%s' "${COSIGN_PRIVATE_KEY}" > /tmp/cosign.key
                        chmod 600 /tmp/cosign.key
                        AUTH=$(printf '%s:%s' "${HARBOR_USER}" "${HARBOR_PASS}" | base64 | tr -d '\n')
                        mkdir -p /tmp/.docker
                        printf '{"auths":{"harbor.tuxgrid.com":{"auth":"%s"}}}' "${AUTH}" \
                            > /tmp/.docker/config.json

                        python3 - << 'PYEOF'
import json, os, datetime

deps = []
deps_path = os.path.join(os.environ.get("WORKSPACE", "."), "deps.ndjson")
if os.path.exists(deps_path):
    with open(deps_path) as f:
        for line in f:
            try:
                e = json.loads(line.strip())
                if e.get("status", 0) < 400 and e.get("url"):
                    dep = {"uri": e["url"]}
                    if e.get("sha256"):
                        dep["digest"] = {"sha256": e["sha256"]}
                    deps.append(dep)
            except Exception:
                pass

git_commit = os.environ.get("GIT_COMMIT", "")
git_url    = os.environ.get("GIT_URL", "")
if git_commit:
    deps.insert(0, {"uri": git_url, "digest": {"gitCommit": git_commit}})

build_ts = os.environ.get("BUILD_TIMESTAMP", "")
if build_ts.isdigit():
    started_on = datetime.datetime.utcfromtimestamp(int(build_ts) / 1000).strftime("%Y-%m-%dT%H:%M:%SZ")
else:
    started_on = datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")
finished_on = datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")

job_name     = os.environ.get("JOB_NAME", "")
build_number = os.environ.get("BUILD_NUMBER", "")
build_url    = os.environ.get("BUILD_URL", "")

provenance = {
    "buildDefinition": {
        "buildType": "https://tuxgrid.com/buildType/jenkins-kaniko/v1",
        "externalParameters": {
            "ref":        git_commit,
            "repository": git_url,
            "dockerfile": "Dockerfile",
        },
        "resolvedDependencies": deps,
    },
    "runDetails": {
        "builder": {"id": "https://jenkins.tuxgrid.com/job/" + job_name + "/" + build_number},
        "metadata": {
            "invocationId": build_url,
            "startedOn":    started_on,
            "finishedOn":   finished_on,
        },
    },
}
with open("/tmp/provenance.json", "w") as f:
    json.dump(provenance, f, indent=2)
print("provenance.json: {} resolved dependencies".format(len(deps)))
PYEOF

                        DOCKER_CONFIG=/tmp/.docker COSIGN_PASSWORD="" cosign attest --key /tmp/cosign.key --yes \
                            --type slsaprovenance1 \
                            --predicate /tmp/provenance.json \
                            "${PROVENANCE_IMAGE_REF}"

                        SYFT_CHECK_FOR_APP_UPDATE=false syft "${PROVENANCE_IMAGE_REF}" \
                            --output cyclonedx-json=/tmp/sbom.json

                        DOCKER_CONFIG=/tmp/.docker COSIGN_PASSWORD="" cosign attest --key /tmp/cosign.key --yes \
                            --type cyclonedx \
                            --predicate /tmp/sbom.json \
                            "${PROVENANCE_IMAGE_REF}"

                        rm -f /tmp/cosign.key /tmp/.docker/config.json /tmp/provenance.json /tmp/sbom.json
                    '''
                }
            }
        }
    }
}
