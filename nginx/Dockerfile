# Nginx reverse proxy with the config baked into the image.
# (Baking it in, instead of bind-mounting nginx.conf, is what makes the
# deployment work when Jenkins itself runs inside a container: a bind-mount
# path from the Jenkins workspace does not exist on the Docker host.)
FROM nginx:1.28-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1/nginx-health || exit 1
