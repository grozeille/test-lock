--liquibase formatted sql
--changeset mathias.kluba:v1 splitStatements:true endDelimiter:;
CREATE TABLE IF NOT EXISTS CONTAINER (
  containerId VARCHAR(250) PRIMARY KEY,
  lambdaId VARCHAR(250) NOT NULL
);

CREATE TABLE IF NOT EXISTS LAMBDA  (
  id VARCHAR(250) PRIMARY KEY,
  containerId VARCHAR(250),
  lastUsedTimestamp bigint
);

CREATE TABLE IF NOT EXISTS JOB  (
  id VARCHAR(250) PRIMARY KEY,
  lambdaId VARCHAR(250),
  submitDateTimestamp bigint,
  startDateTimestamp bigint,
  endDateTimestamp bigint,
  status VARCHAR(250),
  tokenHash VARCHAR(250),
  result VARCHAR(250)
);