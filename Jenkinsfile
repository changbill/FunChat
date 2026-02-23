pipeline {
    agent any
    
    environment {
        DOCKER_IMAGE = "changbill/funchat"
        AWS_IP = "15.164.233.8"
        DOCKER_HUB_CREDS = 'docker-hub-credentials'   // 젠킨스에 등록한 Docker ID
        AWS_CREDS = 'funchat-ec2-credentials'         // 젠킨스에 등록한 AWS .pem 키 ID
    }

    stages {
        stage('1. Unit Test') {
            steps {
                dir('backend') {
                    sh "./gradlew test" 
                }
            }
        }
        
        stage('2. SonarQube Analysis') {
            steps {
                dir('backend') { // 소스 코드가 있는 디렉토리로 이동
                    withSonarQubeEnv('sonar-server') { 
                        sh "./gradlew sonar"
                    }
                }
            }
        }
        
        stage('3. Spring Boot Build') {
            steps {
                dir('backend') {
                    script {
                        sh "docker build -t ${DOCKER_IMAGE}:latest ."
                    }
                }
            }
        }
        
        stage('4. Docker Push') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: "${DOCKER_HUB_CREDS}", passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER')]) {
                        sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'
                        sh 'docker push ${DOCKER_IMAGE}:latest'
                    }
                }
            }
        }

        stage('5. EC2 Deploy') {
            steps {
                sshagent(credentials: ["${AWS_CREDS}"]) {
                    script {
                        sh "scp -o StrictHostKeyChecking=no backend/docker-compose.yml ec2-user@${AWS_IP}:~/funchat/docker-compose.yml"
                        
                        withCredentials([usernamePassword(credentialsId: "${DOCKER_HUB_CREDS}", passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER')]) {
                            sh '''
                                ssh -o StrictHostKeyChecking=no ec2-user@$AWS_IP "
                                    echo '$DOCKER_PASS' | sudo docker login -u '$DOCKER_USER' --password-stdin

                                    cd ~/funchat
                                    sudo docker compose pull
                                    sudo docker compose up -d
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