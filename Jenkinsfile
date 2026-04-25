pipeline {
    agent any

    tools {
        maven 'Maven'
    }

    parameters {
        choice(
            name: 'ACTION',
            choices: ['deploy', 'rollback', 'setup', 'status'],
            description: 'Select action'
        )
        booleanParam(
            name: 'RUN_TESTS',
            defaultValue: true,
            description: 'Run tests?'
        )
        booleanParam(
            name: 'BUILD_DOCKER',
            defaultValue: false,
            description: 'Build Docker images? (optional)'
        )
    }

    environment {
        PROJECT = 'carwash'
        TEAM = 'lintshiwe'
        STACK = 'spring-boot'
        APP_DIR = '/opt/carwash'
        FRAMEWORK_DIR = '/home/lintshiwe/devops-framework'
        PROJECT_DIR = '/home/lintshiwe/devops-framework/projects/carwash'
        JAVA_HOME = "${sh(script: 'dirname $(dirname $(readlink -f $(which java)))', returnStdout: true).trim()}"
        PATH = "${JAVA_HOME}/bin:${env.PATH}"
        ANSIBLE_HOST_KEY_CHECKING = 'False'
    }

    stages {

        stage('Checkout') {
            steps {
                echo "📥 [${PROJECT}] Pulling code..."
                git credentialsId: 'github-token',
                    url: 'https://github.com/Lintshiwe/Smart-Car-Wash-System-Backend.git',
                    branch: 'main'
            }
        }

        stage('Build') {
            when {
                expression { params.ACTION == 'deploy' }
            }
            steps {
                echo "🔨 [${PROJECT}] Building..."
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Test') {
            when {
                allOf {
                    expression { params.ACTION == 'deploy' }
                    expression { params.RUN_TESTS == true }
                }
            }
            steps {
                echo "🧪 [${PROJECT}] Testing..."
                sh 'mvn test'
            }
        }

        stage('Setup Infrastructure') {
            when {
                expression { params.ACTION == 'setup' }
            }
            steps {
                echo "🏗️ [${PROJECT}] Setting up infrastructure..."
                sh """
                    ansible-playbook ${FRAMEWORK_DIR}/ansible/playbooks/setup.yml \
                        -i ${PROJECT_DIR}/inventory.ini \
                        -e @${PROJECT_DIR}/config.yml \
                        -v
                """
            }
        }

        stage('Deploy') {
            when {
                expression { params.ACTION == 'deploy' }
            }
            steps {
                echo "🚀 [${PROJECT}] Deploying..."
                sh """
                    ansible-playbook ${FRAMEWORK_DIR}/ansible/playbooks/deploy.yml \
                        -i ${PROJECT_DIR}/inventory.ini \
                        -e @${PROJECT_DIR}/config.yml \
                        -v
                """
            }
        }

        stage('Rollback') {
            when {
                expression { params.ACTION == 'rollback' }
            }
            steps {
                echo "⏪ [${PROJECT}] Rolling back..."
                sh """
                    ansible-playbook ${FRAMEWORK_DIR}/ansible/playbooks/rollback.yml \
                        -i ${PROJECT_DIR}/inventory.ini \
                        -e @${PROJECT_DIR}/config.yml \
                        -v
                """
            }
        }

        stage('Status') {
            when {
                expression { params.ACTION == 'status' }
            }
            steps {
                echo "📊 [${PROJECT}] Checking status..."
                sh """
                    ansible-playbook ${FRAMEWORK_DIR}/ansible/playbooks/status.yml \
                        -i ${PROJECT_DIR}/inventory.ini \
                        -e @${PROJECT_DIR}/config.yml \
                        -v
                """
            }
        }

        stage('Docker (Optional)') {
            when {
                expression { params.BUILD_DOCKER == true }
            }
            steps {
                echo "🐳 [${PROJECT}] Building Docker images..."
                sh """
                    ${FRAMEWORK_DIR}/scripts/docker-build.sh carwash
                """
            }
        }

        stage('Health Check') {
            when {
                expression { params.ACTION == 'deploy' }
            }
            steps {
                echo "💓 [${PROJECT}] Health check..."
                sh """
                    sleep 15
                    ansible-playbook ${FRAMEWORK_DIR}/ansible/playbooks/status.yml \
                        -i ${PROJECT_DIR}/inventory.ini \
                        -e @${PROJECT_DIR}/config.yml
                """
            }
        }
    }

    post {
        success {
            echo "✅ [${PROJECT}] Pipeline completed!"
        }
        failure {
            echo "❌ [${PROJECT}] Pipeline failed! Logs: /opt/carwash/logs/"
        }
        always {
            cleanWs()
        }
    }
}
