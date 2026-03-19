FROM node:24-alpine AS build

ARG NUXT_PUBLIC_API_BASE_URL=/
ENV NUXT_PUBLIC_API_BASE_URL=${NUXT_PUBLIC_API_BASE_URL}

WORKDIR /app

RUN corepack enable

COPY frontend/package.json /app/package.json
COPY frontend/yarn.lock /app/yarn.lock
COPY frontend/.yarnrc.yml /app/.yarnrc.yml
COPY frontend/.yarn /app/.yarn

RUN yarn install --immutable

COPY frontend/ /app/

RUN yarn generate

FROM nginx:1.29-alpine

COPY frontend/docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/.output/public /usr/share/nginx/html

EXPOSE 80
