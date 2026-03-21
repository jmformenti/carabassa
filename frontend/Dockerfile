FROM node:24-alpine AS build

# API base URL is intentionally empty so the frontend uses relative paths (/api/...).
# nginx proxies /api requests to the backend container (see docker/nginx.conf).
ARG NUXT_PUBLIC_API_BASE_URL=
ENV NUXT_PUBLIC_API_BASE_URL=${NUXT_PUBLIC_API_BASE_URL}
ARG APP_VERSION=dev
ENV NUXT_PUBLIC_APP_VERSION=${APP_VERSION}

WORKDIR /app

RUN corepack enable

COPY package.json /app/package.json
COPY yarn.lock /app/yarn.lock
COPY .yarnrc.yml /app/.yarnrc.yml
COPY .yarn /app/.yarn

RUN yarn install --immutable

COPY / /app/

RUN yarn generate

FROM nginx:1.29-alpine

COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/.output/public /usr/share/nginx/html

EXPOSE 80
