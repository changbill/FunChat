pipeline {
    agent any
    
    environment {
        BACKEND_IMAGE = "changbill/funchat-backend"
        FRONTEND_IMAGE = "changbill/funchat-frontend"
        // 배포 대상(미니 PC)로 바꾸세요.
        // 예) DEPLOY_HOST = "funchat.changee.cloud" 또는 내부/공인 IP
        DEPLOY_HOST = "192.168.45.40"
        DEPLOY_USER = "changbill"

        // 백엔드 수평확장 개수 (docker compose up --scale)
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
                        sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'
                        sh 'docker push ${BACKEND_IMAGE}:latest'
                        sh 'docker push ${FRONTEND_IMAGE}:latest'
                    }
                }
            }
        }

        stage('6. Deploy') {
            steps {
                sshagent(credentials: ["${MINI_PC_CREDS}"]) {
                    script {
                        sh "scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -r deploy ${DEPLOY_USER}@${DEPLOY_HOST}:~/funchat/deploy"
                        
                        withCredentials([
                            file(credentialsId: 'funchat-env', variable: 'ENV_FILE'),
                            usernamePassword(credentialsId: "${DOCKER_HUB_CREDS}", passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER')
                        ]) {
                            sh '''
                                set -e

                                # Jenkins credential 파일 내용을 SSH stdin으로 원격 /tmp에 생성
                                cat "$ENV_FILE" | ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null $DEPLOY_USER@$DEPLOY_HOST 'cat > /tmp/.funchat.env'

                                # 원격 배포 스크립트 실행(젠킨스는 전송+실행만 담당)
                                ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null $DEPLOY_USER@$DEPLOY_HOST \
                                  "chmod +x ~/funchat/deploy/deploy.sh && ENV_FILE=/tmp/.funchat.env DOCKER_USER='$DOCKER_USER' DOCKER_PASS='$DOCKER_PASS' APP_REPLICAS='$APP_REPLICAS' ~/funchat/deploy/deploy.sh"
                            '''
                        }
                    }
                }
            }
        }
    }
}