FROM nginx:1.25-alpine

# Remove config padrão
RUN rm /etc/nginx/conf.d/default.conf

# Copia config customizada
COPY nginx.conf /etc/nginx/conf.d/dentibot.conf

# Copia todos os arquivos estáticos do projeto
COPY ../index.html /usr/share/nginx/html/
COPY ../style.css /usr/share/nginx/html/
COPY ../script.js /usr/share/nginx/html/
COPY ../src /usr/share/nginx/html/src

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
