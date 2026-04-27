pipeline {
  agent any

  parameters {
    booleanParam(name: 'DEPLOY_PRODUCTION', defaultValue: false, description: 'Deploy this build to production')
    string(name: 'PROD_DEPLOY_CMD', defaultValue: '', description: 'Shell command used to deploy backend to production target')
  }

  options {
    timestamps()
    disableConcurrentBuilds()
  }

  environment {
    MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
  }

  stages {
    stage('Build') {
      steps {
        echo 'Building backend modules...'
        sh 'mvn -B -ntp clean compile'
      }
    }

    stage('Test') {
      steps {
        echo 'Running backend tests...'
        sh 'mvn -B -ntp test'
      }
    }

    stage('Package') {
      steps {
        echo 'Packaging backend...'
        sh 'mvn -B -ntp -DskipTests package'
      }
    }

    stage('Approve Production') {
      when {
        expression {
          return params.DEPLOY_PRODUCTION && (env.BRANCH_NAME == null || env.BRANCH_NAME == 'main')
        }
      }
      steps {
        input message: 'Deploy backend build to PRODUCTION?', ok: 'Deploy'
      }
    }

    stage('Deploy Production') {
      when {
        expression {
          return params.DEPLOY_PRODUCTION && (env.BRANCH_NAME == null || env.BRANCH_NAME == 'main')
        }
      }
      steps {
        script {
          if (!params.PROD_DEPLOY_CMD?.trim()) {
            error 'DEPLOY_PRODUCTION=true but PROD_DEPLOY_CMD is empty. Provide deployment command.'
          }
        }
        echo 'Deploying backend to production...'
        sh '''#!/usr/bin/env bash
set -euo pipefail
eval "$PROD_DEPLOY_CMD"
'''
      }
    }
  }

  post {
    success {
      echo 'Backend pipeline completed successfully.'
    }
    failure {
      echo 'Backend pipeline failed.'
    }
  }
}
