-- V2 inserted the admin with a hand-picked id and left hibernate_sequence at its start,
-- so the first registered user got id 1 and clashed with it.
-- Move the sequence past every existing id, never backwards.
select setval('hibernate_sequence', greatest(
        (select last_value from hibernate_sequence),
        (select coalesce(max(id), 0) from m_user),
        (select coalesce(max(id), 0) from message),
        1));
