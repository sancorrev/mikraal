-- Runs once when the local Postgres volume is created. Development only.
CREATE ROLE openfga LOGIN PASSWORD 'openfga';
CREATE DATABASE openfga OWNER openfga;

-- Activities service: one database for the service, two roles.
-- activities_owner runs Flyway migrations and owns the schema.
-- activities_app is the runtime role: no BYPASSRLS, so row-level security always applies.
CREATE ROLE activities_owner LOGIN PASSWORD 'activities_owner';
CREATE ROLE activities_app LOGIN PASSWORD 'activities_app' NOSUPERUSER NOBYPASSRLS;
CREATE DATABASE activities OWNER activities_owner;
