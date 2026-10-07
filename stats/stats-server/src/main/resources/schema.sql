DROP TABLE IF EXISTS hits CASCADE;

CREATE TABLE hits (
                      id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                      app       VARCHAR(255) NOT NULL,
                      uri       VARCHAR(255) NOT NULL,
                      ip        VARCHAR(45)  NOT NULL,
                      timestamp TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_hits_timestamp ON hits(timestamp);
CREATE INDEX IF NOT EXISTS idx_hits_app_uri ON hits(app, uri);