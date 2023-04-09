FROM postgres:latest

ENV POSTGRES_USER edgar_user
ENV POSTGRES_PASSWORD password
ENV POSTGRES_DB edgar

COPY initPostgres.sql /docker-entrypoint-initdb.d/

VOLUME /var/lib/postgresql/data

EXPOSE 5432
