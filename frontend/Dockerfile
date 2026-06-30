# 빌드 스테이지
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# 실행 스테이지
FROM nginx:stable-alpine
# 빌드 결과물 복사
COPY --from=build /app/dist /usr/share/nginx/html
# [중요] 직접 만든 nginx 설정 파일 주입
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]