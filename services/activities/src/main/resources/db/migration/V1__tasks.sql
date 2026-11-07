-- Kraal tasks module. Runs as the schema owner; the runtime role is ${appRole}.
-- Every table carries group_id (tenant) and is protected by row-level security:
-- the application sets app.current_group per transaction (SET LOCAL semantics).

CREATE FUNCTION current_group() RETURNS uuid
    LANGUAGE sql STABLE
AS $$ SELECT nullif(current_setting('app.current_group', true), '')::uuid $$;

CREATE TABLE recurrence_series (
    id              uuid PRIMARY KEY,
    group_id        uuid        NOT NULL,
    period          text        NOT NULL CHECK (period IN ('weekly', 'monthly', 'round')),
    anchor_due_date date        NOT NULL,
    last_due_date   date        NOT NULL,
    active          boolean     NOT NULL DEFAULT true,
    created_by      uuid        NOT NULL,
    created_at      timestamptz NOT NULL,
    ended_at        timestamptz,
    ended_by        uuid,
    UNIQUE (group_id, id)
);

CREATE TABLE task (
    id                   uuid PRIMARY KEY,
    group_id             uuid        NOT NULL,
    title                text        NOT NULL,
    description          text,
    status               text        NOT NULL CHECK (status IN ('pending', 'in_progress', 'paused', 'finished')),
    pause_reason         text,
    due_date             date,
    reminder_days_before smallint CHECK (reminder_days_before BETWEEN 0 AND 30),
    reminder_sent_for    date,
    series_id            uuid,
    created_by           uuid        NOT NULL,
    created_at           timestamptz NOT NULL,
    updated_at           timestamptz NOT NULL,
    version              integer     NOT NULL DEFAULT 1 CHECK (version >= 1),
    deleted_at           timestamptz,
    deleted_by           uuid,
    UNIQUE (group_id, id),
    FOREIGN KEY (group_id, series_id) REFERENCES recurrence_series (group_id, id),
    CHECK ((status = 'paused') = (pause_reason IS NOT NULL)),
    CHECK (reminder_days_before IS NULL OR due_date IS NOT NULL),
    CHECK (series_id IS NULL OR due_date IS NOT NULL)
);
CREATE INDEX task_list_order ON task (group_id, (coalesce(due_date, 'infinity'::date)), id) WHERE deleted_at IS NULL;
CREATE INDEX task_series ON task (series_id) WHERE series_id IS NOT NULL;

CREATE TABLE task_owner (
    group_id   uuid NOT NULL,
    task_id    uuid NOT NULL,
    owner_type text NOT NULL CHECK (owner_type IN ('person', 'position')),
    owner_id   uuid NOT NULL,
    PRIMARY KEY (task_id, owner_type, owner_id),
    FOREIGN KEY (group_id, task_id) REFERENCES task (group_id, id)
);
CREATE INDEX task_owner_by_owner ON task_owner (group_id, owner_type, owner_id);

CREATE TABLE task_link (
    group_id  uuid NOT NULL,
    task_id   uuid NOT NULL,
    link_type text NOT NULL CHECK (link_type IN ('council', 'event', 'position')),
    link_id   uuid NOT NULL,
    PRIMARY KEY (task_id, link_type, link_id),
    FOREIGN KEY (group_id, task_id) REFERENCES task (group_id, id)
);
CREATE INDEX task_link_by_item ON task_link (group_id, link_type, link_id);

CREATE TABLE tag (
    id         uuid PRIMARY KEY,
    group_id   uuid        NOT NULL,
    name       text        NOT NULL,
    created_at timestamptz NOT NULL,
    UNIQUE (group_id, id)
);
CREATE UNIQUE INDEX tag_name_per_group ON tag (group_id, lower(name));

CREATE TABLE task_tag (
    group_id uuid NOT NULL,
    task_id  uuid NOT NULL,
    tag_id   uuid NOT NULL,
    PRIMARY KEY (task_id, tag_id),
    FOREIGN KEY (group_id, task_id) REFERENCES task (group_id, id),
    FOREIGN KEY (group_id, tag_id) REFERENCES tag (group_id, id)
);
CREATE INDEX task_tag_by_tag ON task_tag (tag_id);

-- Append-only: who, on behalf of which position, what kind of change and which fields.
CREATE TABLE task_history (
    id                       uuid PRIMARY KEY,
    group_id                 uuid        NOT NULL,
    task_id                  uuid        NOT NULL,
    at                       timestamptz NOT NULL,
    actor_person_id          uuid        NOT NULL,
    on_behalf_of_position_id uuid,
    kind                     text        NOT NULL CHECK (kind IN ('created', 'edited', 'status_changed',
                                                                  'owner_removed_on_leave', 'recurrence_ended',
                                                                  'instance_created')),
    changes                  jsonb,
    FOREIGN KEY (group_id, task_id) REFERENCES task (group_id, id)
);
CREATE INDEX task_history_by_task ON task_history (task_id, at DESC, id DESC);

CREATE FUNCTION reject_history_rewrite() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'task_history is append-only';
END
$$;
CREATE TRIGGER task_history_append_only
    BEFORE UPDATE OR DELETE ON task_history
    FOR EACH STATEMENT EXECUTE FUNCTION reject_history_rewrite();

DO $$
DECLARE
    t text;
BEGIN
    FOREACH t IN ARRAY ARRAY['recurrence_series', 'task', 'task_owner', 'task_link', 'tag', 'task_tag', 'task_history']
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format('CREATE POLICY tenant_isolation ON %I USING (group_id = current_group()) '
                       'WITH CHECK (group_id = current_group())', t);
    END LOOP;
END
$$;

GRANT USAGE ON SCHEMA ${flyway:defaultSchema} TO ${appRole};
GRANT EXECUTE ON FUNCTION current_group() TO ${appRole};
GRANT SELECT, INSERT, UPDATE ON recurrence_series, task TO ${appRole};
GRANT SELECT, INSERT, DELETE ON task_owner, task_link, task_tag TO ${appRole};
GRANT SELECT, INSERT, UPDATE, DELETE ON tag TO ${appRole};
GRANT SELECT, INSERT ON task_history TO ${appRole};
REVOKE UPDATE, DELETE, TRUNCATE ON task_history FROM ${appRole};
