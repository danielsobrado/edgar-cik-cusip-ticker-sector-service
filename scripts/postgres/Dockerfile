FROM postgres:latest

ENV POSTGRES_USER edgar_user
ENV POSTGRES_PASSWORD password
ENV POSTGRES_DB edgar

COPY init.sql /docker-entrypoint-initdb.d/

EXPOSE 5432
