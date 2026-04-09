pipeline {
    agent any
    
    environment {
        BACKEND_IMAGE = "changbill/funchat-backend"
        FRONTEND_IMAGE = "changbill/funchat-frontend"
        AWS_IP = "15.164.233.8"
        DOCKER_HUB_CREDS = 'docker-hub-credentials'   // 젠킨스에 등록한 Docker ID
        AWS_CREDS = 'funchat-ec2-credentials'         // 젠킨스에 등록한 AWS .pem 키 ID
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

        stage('6. EC2 Deploy') {
            steps {
                sshagent(credentials: ["${AWS_CREDS}"]) {
                    script {
                        sh "scp -o StrictHostKeyChecking=no docker-compose.yml ec2-user@${AWS_IP}:~/funchat/docker-compose.yml"
                        
                        withCredentials([usernamePassword(credentialsId: "${DOCKER_HUB_CREDS}", passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER')]) {
                            sh '''
                                ssh -o StrictHostKeyChecking=no ec2-user@$AWS_IP "
                                    echo '$DOCKER_PASS' | sudo docker login -u '$DOCKER_USER' --password-stdin

                                    cd ~/funchat
                                    sudo docker compose -f docker-compose.yml pull
                                    sudo docker compose -f docker-compose.yml up -d --remove-orphans
                                    sudo docker logout
                                    sudo docker image prune -f
                                "
                            '''
                        }
                    }
                }
            }
        }
    }
}