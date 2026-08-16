-- V1__baseline.sql
-- Generated from the live schema Hibernate's ddl-auto produced, so this is exactly what already
-- exists rather than a hand-written approximation of it.
--
-- Existing databases are baselined at V1 and skip this file (spring.flyway.baseline-version=1).
-- A fresh database gets its whole schema from here - which is the point: the schema becomes a
-- reviewed artefact in git rather than a side effect of whatever the entity classes happened to
-- look like the last time the application started.


CREATE SEQUENCE notification_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE notifications (
    id bigint NOT NULL,
    created_date timestamp(6) with time zone NOT NULL,
    channel character varying(10) NOT NULL,
    correlation_id character varying(36) NOT NULL,
    notification_type character varying(20) NOT NULL,
    recipient character varying(254) NOT NULL,
    status character varying(10) NOT NULL,
    CONSTRAINT notifications_channel_check CHECK (((channel)::text = ANY ((ARRAY['SMS'::character varying, 'EMAIL'::character varying])::text[]))),
    CONSTRAINT notifications_notification_type_check CHECK (((notification_type)::text = ANY ((ARRAY['PASSWORD_RESET'::character varying, 'OTP'::character varying])::text[]))),
    CONSTRAINT notifications_status_check CHECK (((status)::text = ANY ((ARRAY['SENT'::character varying, 'FAILED'::character varying])::text[])))
);

ALTER TABLE ONLY notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);

CREATE INDEX idx_notifications_correlation_id ON notifications USING btree (correlation_id);

CREATE INDEX idx_notifications_created_date ON notifications USING btree (created_date);

