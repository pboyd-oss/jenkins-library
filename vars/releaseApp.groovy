def call(Map config = [:]) {
    container('skaffold') {
        sh "skaffold render --profile=${config.environment ?: 'dev'} --build-artifacts=artifacts.json --output=rendered.yaml"
        sh """
            TOKEN=\$(kubectl create token deploy-sa -n jenkins --duration=15m)
            cat > /tmp/deploy-kubeconfig <<EOF
apiVersion: v1
kind: Config
clusters:
- cluster:
    certificate-authority: /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
    server: https://kubernetes.default.svc
  name: cluster
contexts:
- context:
    cluster: cluster
    user: deploy-sa
  name: ctx
current-context: ctx
users:
- name: deploy-sa
  user:
    token: \${TOKEN}
EOF
            skaffold apply rendered.yaml --kubeconfig=/tmp/deploy-kubeconfig
            rm -f /tmp/deploy-kubeconfig
        """
    }
}
