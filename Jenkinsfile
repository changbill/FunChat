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
                        sh "scp -o StrictHostKeyChecking=no -r deploy ${DEPLOY_USER}@${DEPLOY_HOST}:~/funchat/deploy"
                        
                        withCredentials([usernamePassword(credentialsId: "${DOCKER_HUB_CREDS}", passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER')]) {
                            sh '''
                                ssh -o StrictHostKeyChecking=no $DEPLOY_USER@$DEPLOY_HOST "
                                    echo '$DOCKER_PASS' | sudo docker login -u '$DOCKER_USER' --password-stdin

                                    cd ~/funchat

                                    # 0) 기본 폴더/상태 파일 준비
                                    mkdir -p deploy/nginx
                                    if [ ! -f deploy/nginx/upstream.conf ]; then
                                      cp deploy/nginx/upstream.blue.conf deploy/nginx/upstream.conf
                                      echo blue > deploy/active_color
                                    fi

                                    ACTIVE=\$(cat deploy/active_color 2>/dev/null || echo blue)
                                    if [ \"\$ACTIVE\" = \"blue\" ]; then INACTIVE=green; else INACTIVE=blue; fi

                                    # 1) infra는 항상 유지(처음만 띄우고 이후엔 변경 최소화)
                                    sudo docker compose -f deploy/docker-compose.infra.yml up -d

                                    # 2) 새 이미지 pull
                                    sudo docker compose -p funchat_\$INACTIVE -f deploy/docker-compose.\$INACTIVE.yml pull

                                    # 3) 비활성(=새) 스택 기동
                                    sudo docker compose -p funchat_\$INACTIVE -f deploy/docker-compose.\$INACTIVE.yml up -d --remove-orphans --scale app=${APP_REPLICAS}

                                    # 4) 헬스체크 대기(app 컨테이너가 여러 개여도 동작)
                                    echo 'Waiting for new stack health...'
                                    APP_IDS=\$(sudo docker compose -p funchat_\$INACTIVE -f deploy/docker-compose.\$INACTIVE.yml ps -q app)
                                    if [ -z \"\$APP_IDS\" ]; then
                                      echo 'No app containers found.'
                                      sudo docker compose -p funchat_\$INACTIVE -f deploy/docker-compose.\$INACTIVE.yml ps
                                      exit 1
                                    fi

                                    OK=0
                                    for i in \$(seq 1 60); do
                                      ALL_HEALTHY=1
                                      for id in \$APP_IDS; do
                                        STATUS=\$(sudo docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}nohealth{{end}}' \$id 2>/dev/null || echo unknown)
                                        if [ \"\$STATUS\" != \"healthy\" ]; then
                                          ALL_HEALTHY=0
                                          break
                                        fi
                                      done

                                      if [ \"\$ALL_HEALTHY\" = \"1\" ]; then
                                        OK=1
                                        echo 'New stack healthy.'
                                        break
                                      fi
                                      sleep 2
                                    done

                                    if [ \"\$OK\" != \"1\" ]; then
                                      echo 'New stack did not become healthy.'
                                      sudo docker compose -p funchat_\$INACTIVE -f deploy/docker-compose.\$INACTIVE.yml logs --no-color --tail=200
                                      exit 1
                                    fi

                                    # 5) 라우터(Nginx) upstream 전환 + reload (무중단 스위치)
                                    if [ \"\$INACTIVE\" = \"blue\" ]; then
                                      cp deploy/nginx/upstream.blue.conf deploy/nginx/upstream.conf
                                    else
                                      cp deploy/nginx/upstream.green.conf deploy/nginx/upstream.conf
                                    fi

                                    sudo docker compose -f deploy/docker-compose.router.yml up -d
                                    sudo docker exec funchat-router nginx -s reload
                                    echo \$INACTIVE > deploy/active_color

                                    # 6) 구 스택 정리(원하면 주석 처리 가능)
                                    sudo docker compose -p funchat_\$ACTIVE -f deploy/docker-compose.\$ACTIVE.yml down

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