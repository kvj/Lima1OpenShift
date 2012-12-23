-- DROP SEQUENCE ids;

CREATE SEQUENCE ids
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;
  
 -- DROP TABLE users;

CREATE TABLE users
(
  id bigint NOT NULL,
  username character varying(255),
  password character varying(255),
  created bigint,
  CONSTRAINT pk_users PRIMARY KEY (id )
)
WITH (
  OIDS=FALSE
);

-- DROP INDEX idx_users_username;

CREATE UNIQUE INDEX idx_users_username
  ON users
  USING btree
  (username COLLATE pg_catalog."default" );

-- DROP TABLE tokens;

CREATE TABLE tokens
(
  id bigint NOT NULL,
  user_id bigint,
  token character varying(255),
  client character varying(255),
  issued bigint,
  accessed bigint,
  CONSTRAINT pk_tokens PRIMARY KEY (id ),
  CONSTRAINT fk_tokens_users FOREIGN KEY (user_id)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);

-- DROP TABLE data;

CREATE TABLE data
(
  id bigint NOT NULL,
  app character varying(255),
  stream character varying(255),
  user_id bigint,
  status integer,
  updated bigint,
  token character varying(255),
  object text,
  object_id bigint,
  CONSTRAINT pk_data PRIMARY KEY (id ),
  CONSTRAINT fk_data_users FOREIGN KEY (user_id)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);

-- DROP TABLE files;

CREATE TABLE files
(
  id bigint NOT NULL,
  app character varying(255),
  user_id bigint,
  created bigint,
  name character varying(1023),
  CONSTRAINT pk_files PRIMARY KEY (id ),
  CONSTRAINT fk_files_users FOREIGN KEY (user_id)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);

ALTER TABLE users ADD COLUMN email character varying(255);
ALTER TABLE users ADD COLUMN "name" character varying(255);
ALTER TABLE users ADD COLUMN rights integer DEFAULT 0;

CREATE TABLE apps
(
   id bigint NOT NULL, 
   app character varying(255), 
   "name" character varying(512), 
   description text, 
   CONSTRAINT pk_apps PRIMARY KEY (id)
) WITH (OIDS=FALSE)
;

CREATE TABLE shemas
(
   id bigint NOT NULL, 
   rev integer DEFAULT 0, 
   app_id bigint, 
   created bigint DEFAULT 0, 
   "schema" text, 
   CONSTRAINT pk_schemas PRIMARY KEY (id), 
   CONSTRAINT fk_shemas_apps FOREIGN KEY (app_id) REFERENCES apps (id)    ON UPDATE NO ACTION ON DELETE NO ACTION
) WITH (OIDS=FALSE)
;

ALTER TABLE files ADD COLUMN status integer DEFAULT 1;
ALTER TABLE files ADD COLUMN updated bigint DEFAULT 0;
ALTER TABLE files ADD COLUMN token character varying(255);

CREATE TABLE settings
(
   id bigint NOT NULL, 
   "name" character varying(255), 
   "value" character varying(512), 
   description character varying(512), 
   CONSTRAINT pk_settings PRIMARY KEY (id)
) WITH (OIDS=FALSE)
;
