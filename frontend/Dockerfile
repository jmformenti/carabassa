FROM node:24-alpine AS build

ARG NUXT_PUBLIC_API_BASE_URL=/
ENV NUXT_PUBLIC_API_BASE_URL=${NUXT_PUBLIC_API_BASE_URL}

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
