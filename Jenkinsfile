pipeline {
  agent any

  options {
    buildDiscarder(logRotator(numToKeepStr: '20'))
    disableConcurrentBuilds()
    skipDefaultCheckout(true)
    timestamps()
  }

  environment {
    ACR_REGISTRY = 'crpi-ekwujpeg6f954ar3.cn-wulanchabu.personal.cr.aliyuncs.com'
    ACR_NAMESPACE = 'cloud-ops-hub'
    IMAGE_VERSION_PREFIX = '0.1.0-airesume'
    K8S_NAMESPACE = 'ai-resume'
    API_DEPLOYMENT = 'ai-resume-api'
    WEB_DEPLOYMENT = 'ai-resume-web'
    API_IMAGE_NAME = 'ai-resume-api'
    WEB_IMAGE_NAME = 'ai-resume-web'
    API_DEPLOYMENT_FILE = 'deploy/k8s/api-deployment.yaml'
    WEB_DEPLOYMENT_FILE = 'deploy/k8s/web-deployment.yaml'
    GIT_PUSH_URL = 'https://github.com/Deriou/ai-resume.git'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
        sh '''
          set -eu
          git status --short
          git rev-parse --short=7 HEAD
        '''
      }
    }

    stage('Prepare Tag') {
      steps {
        script {
          env.GIT_SHORT_SHA = sh(script: 'git rev-parse --short=7 HEAD', returnStdout: true).trim()
          env.IMAGE_TAG = "${env.IMAGE_VERSION_PREFIX}-${env.BUILD_NUMBER}-${env.GIT_SHORT_SHA}"
          env.API_IMAGE = "${env.ACR_REGISTRY}/${env.ACR_NAMESPACE}/${env.API_IMAGE_NAME}:${env.IMAGE_TAG}"
          env.WEB_IMAGE = "${env.ACR_REGISTRY}/${env.ACR_NAMESPACE}/${env.WEB_IMAGE_NAME}:${env.IMAGE_TAG}"
          currentBuild.displayName = "#${env.BUILD_NUMBER} ${env.IMAGE_TAG}"
        }
        sh '''
          set -eu
          echo "IMAGE_TAG=${IMAGE_TAG}"
          echo "API_IMAGE=${API_IMAGE}"
          echo "WEB_IMAGE=${WEB_IMAGE}"
        '''
      }
    }

    stage('Preflight') {
      steps {
        sh '''
          set -eu
          test -f Dockerfile
          test -f web/Dockerfile
          test -f "${API_DEPLOYMENT_FILE}"
          test -f "${WEB_DEPLOYMENT_FILE}"
          docker version
          kubectl version --client
          kubectl -n "${K8S_NAMESPACE}" auth can-i patch deployments
          kubectl -n "${K8S_NAMESPACE}" auth can-i get pods/log
        '''
      }
    }

    stage('Docker Login') {
      steps {
        withCredentials([usernamePassword(
          credentialsId: 'acr-cloud-ops-hub',
          usernameVariable: 'ACR_USERNAME',
          passwordVariable: 'ACR_PASSWORD'
        )]) {
          sh '''
            set +x
            echo "${ACR_PASSWORD}" | docker login "${ACR_REGISTRY}" -u "${ACR_USERNAME}" --password-stdin
          '''
        }
      }
    }

    stage('Build API Image') {
      steps {
        sh '''
          set -eu
          docker build --platform linux/amd64 -t "${API_IMAGE}" .
        '''
      }
    }

    stage('Build Web Image') {
      steps {
        sh '''
          set -eu
          docker build --platform linux/amd64 -f web/Dockerfile -t "${WEB_IMAGE}" web
        '''
      }
    }

    stage('Push Images') {
      steps {
        sh '''
          set -eu
          docker push "${API_IMAGE}"
          docker push "${WEB_IMAGE}"
        '''
      }
    }

    stage('Update Manifests') {
      steps {
        sh '''
          set -eu
          sed -i -E "s#(image: )${ACR_REGISTRY}/${ACR_NAMESPACE}/${API_IMAGE_NAME}:.*#\\1${API_IMAGE}#" "${API_DEPLOYMENT_FILE}"
          sed -i -E "s#(image: )${ACR_REGISTRY}/${ACR_NAMESPACE}/${WEB_IMAGE_NAME}:.*#\\1${WEB_IMAGE}#" "${WEB_DEPLOYMENT_FILE}"
          grep -n "image:" "${API_DEPLOYMENT_FILE}"
          grep -n "image:" "${WEB_DEPLOYMENT_FILE}"
        '''
      }
    }

    stage('Commit Manifests') {
      steps {
        withCredentials([usernamePassword(
          credentialsId: 'github-cloud-ops-hub-token',
          usernameVariable: 'GITHUB_USERNAME',
          passwordVariable: 'GITHUB_TOKEN'
        )]) {
          sh '''
            set -eu
            git config user.name "cloud-ops-jenkins"
            git config user.email "jenkins@cloud-ops-hub.local"
            git add "${API_DEPLOYMENT_FILE}" "${WEB_DEPLOYMENT_FILE}"

            if git diff --cached --quiet; then
              echo "No deployment image change to commit."
            else
              git commit -m "chore(cicd): bump ai-resume images ${IMAGE_TAG} [skip ci]"
            fi

            set +x
            askpass_file="$(pwd)/.git/jenkins-askpass.sh"
            cat > "${askpass_file}" <<'EOF'
#!/bin/sh
case "$1" in
  *Username*) echo "${GITHUB_USERNAME}" ;;
  *Password*) echo "${GITHUB_TOKEN}" ;;
esac
EOF
            chmod 700 "${askpass_file}"
            GIT_ASKPASS="${askpass_file}" GIT_TERMINAL_PROMPT=0 git push "${GIT_PUSH_URL}" HEAD:main
            rm -f "${askpass_file}"
          '''
        }
      }
    }

    stage('Deploy') {
      steps {
        sh '''
          set -eu
          kubectl apply -f "${API_DEPLOYMENT_FILE}"
          kubectl apply -f "${WEB_DEPLOYMENT_FILE}"
        '''
      }
    }

    stage('Verify') {
      steps {
        sh '''
          set -eu
          kubectl -n "${K8S_NAMESPACE}" rollout status "deploy/${API_DEPLOYMENT}" --timeout=300s
          kubectl -n "${K8S_NAMESPACE}" rollout status "deploy/${WEB_DEPLOYMENT}" --timeout=180s

          actual_api_image="$(kubectl -n "${K8S_NAMESPACE}" get deploy "${API_DEPLOYMENT}" -o jsonpath='{.spec.template.spec.containers[0].image}')"
          actual_web_image="$(kubectl -n "${K8S_NAMESPACE}" get deploy "${WEB_DEPLOYMENT}" -o jsonpath='{.spec.template.spec.containers[0].image}')"
          echo "Actual API image: ${actual_api_image}"
          echo "Actual Web image: ${actual_web_image}"
          test "${actual_api_image}" = "${API_IMAGE}"
          test "${actual_web_image}" = "${WEB_IMAGE}"
        '''
      }
    }
  }

  post {
    always {
      sh '''
        docker logout "${ACR_REGISTRY}" || true
        rm -f .git/jenkins-askpass.sh || true
      '''
    }
  }
}
