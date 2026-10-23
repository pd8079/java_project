FROM docker.io/library/nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY index.html 404.html deploy-config.js /usr/share/nginx/html/
EXPOSE 80
