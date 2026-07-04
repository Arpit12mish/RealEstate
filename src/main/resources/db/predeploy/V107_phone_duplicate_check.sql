-- Pre-deploy duplicate report for V107__align_mobile_auth_tables.sql.
--
-- Run this query against production before deploying V107.
-- If it returns any rows, stop deployment and manually merge/cleanup the listed
-- users. Deploy only after this query returns zero rows.
--
-- Indian phone variants such as 9876543210, 09876543210, 919876543210, and
-- +919876543210 are all reported under the same canonical phone.

WITH normalized AS (
    SELECT
        id,
        email,
        phone_number,
        created_at,
        last_login_at,
        role,
        onboarding_status,
        regexp_replace(phone_number, '\D', '', 'g') AS digits
    FROM users
    WHERE phone_number IS NOT NULL
),
canonical AS (
    SELECT
        id,
        email,
        phone_number,
        created_at,
        last_login_at,
        role,
        onboarding_status,
        CASE
            WHEN length(digits) = 10 AND digits ~ '^[6-9][0-9]{9}$'
                THEN '+91' || digits
            WHEN length(digits) = 11 AND digits ~ '^0[6-9][0-9]{9}$'
                THEN '+91' || substring(digits FROM 2)
            WHEN length(digits) = 12 AND digits ~ '^91[6-9][0-9]{9}$'
                THEN '+91' || substring(digits FROM 3)
            ELSE phone_number
        END AS canonical_phone
    FROM normalized
)
SELECT
    canonical_phone,
    count(*) AS row_count,
    array_agg(id ORDER BY id) AS user_ids,
    array_agg(phone_number ORDER BY id) AS stored_phone_numbers,
    array_agg(email ORDER BY id) AS emails,
    array_agg(created_at ORDER BY id) AS created_timestamps,
    array_agg(last_login_at ORDER BY id) AS last_login_timestamps,
    array_agg(role ORDER BY id) AS roles,
    array_agg(onboarding_status ORDER BY id) AS onboarding_statuses
FROM canonical
GROUP BY canonical_phone
HAVING count(*) > 1
ORDER BY canonical_phone;
