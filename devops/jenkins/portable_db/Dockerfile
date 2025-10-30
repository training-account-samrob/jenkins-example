# Use the same Postgres version as your K8s deployment
FROM postgres:17.5
# Set environment variables (same as K8s Secret values)
ENV POSTGRES_USER=postgres
ENV POSTGRES_PASSWORD=securepass
ENV POSTGRES_DB=visa_db
# Copy the schema.sql into the docker-entrypoint-initdb.d directory
# Postgres will automatically run it on first startup
COPY schema.sql /docker-entrypoint-initdb.d/schema.sql
# Expose Postgres port
EXPOSE 5432