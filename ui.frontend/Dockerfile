# Multi-stage Dockerfile para Vue 3 + Vite + AEM ClientLibs + Nginx
FROM node:20-alpine AS build
WORKDIR /app

COPY ui.frontend/package.json ./ui.frontend/
WORKDIR /app/ui.frontend
RUN npm install --no-audit --no-fund

COPY ui.frontend/ ./
RUN npm run build

FROM nginx:1.27-alpine
COPY ui.frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/ui.frontend/dist /usr/share/nginx/html
COPY --from=build /app/ui.apps /usr/share/nginx/html/aem-clientlibs

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

