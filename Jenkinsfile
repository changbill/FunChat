pipeline {
    agent any
    
    environment {
        BACKEND_IMAGE = "changbill/funchat-backend"
        FRONTEND_IMAGE = "changbill/funchat-frontend"
        // 배포 대상(미니 PC)로 바꾸세요.
        // 예) DEPLOY_HOST = "funchat.changee.cloud" 또는 내부/공인 IP
        DEPLOY_HOST = "192.168.45.40"
        DEPLOY_USER = "changbill"

        // 상시 롤링 배포 슬롯 수 (deploy/docker-compose.rolling.yml 기준 최대 3, 배포 중 surge 1개 추가)
        APP_REPLICAS = "3"
        DOCKER_HUB_CREDS = 'docker-hub-credentials'   // 젠킨스에 등록한 Docker ID
        MINI_PC_CREDS = 'minipc-ssh-key'         // 젠킨스에 등록한 MINI PC .pem 키 ID
    }

    stages {
        // stage('1. Unit Test') {
        //     steps {
        //         dir('backend') {
        //             sh "./gradlew test" 
        //         }
        //     }
        // }
        
        // stage('2. SonarQube Analysis') {
        //     steps {
        //         dir('backend') { // 소스 코드가 있는 디렉토리로 이동
        //             withSonarQubeEnv('sonar-server') { 
        //                 sh "./gradlew sonar"
        //             }
        //         }
        //     }
        // }
        
        stage('3. Backend Docker Build') {
            steps {
                dir('backend') {
                    script {
                        sh "docker build -t ${BACKEND_IMAGE}:latest ."
                    }
                }
            }
        }
        
        stage('4. Frontend Docker Build') {
            steps {
                dir('frontend/funchat') {
                    script {
                        sh "docker build -t ${FRONTEND_IMAGE}:latest ."
                    }
                }
            }
        }
        
        stage('5. Docker Push') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: "${DOCKER_HUB_CREDS}", passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER')]) {
                        sh 'bash deploy/scripts/docker-push-images.sh "${BACKEND_IMAGE}:latest" "${FRONTEND_IMAGE}:latest"'
                    }
                }
            }
        }

        stage('6. Deploy') {
            steps {
                sshagent(credentials: ["${MINI_PC_CREDS}"]) {
                    script {
                        withCredentials([
                            file(credentialsId: 'funchat-env', variable: 'ENV_FILE'),
                            usernamePassword(credentialsId: "${DOCKER_HUB_CREDS}", passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER')
                        ]) {
                            sh 'bash deploy/scripts/jenkins-remote-deploy.sh'
                        }
                    }
                }
            }
        }
    }
}
