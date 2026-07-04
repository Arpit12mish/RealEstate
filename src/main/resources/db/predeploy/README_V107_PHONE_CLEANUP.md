# V107 Phone Duplicate Cleanup Runbook

V107 normalizes mobile auth phone numbers and creates a unique index on
`users.phone_number`. Equivalent Indian formats such as `9876543210`,
`09876543210`, `919876543210`, and `+919876543210` all map to the canonical
value `+919876543210`.

The migration intentionally fails before making changes if duplicate canonical
phone groups exist. Do not auto-merge or auto-delete users.

## Deployment Procedure

1. Take a production database backup.

2. Run the duplicate report:

   ```bash
   psql "$DATABASE_URL" -f src/main/resources/db/predeploy/V107_phone_duplicate_check.sql
   ```

3. If the duplicate report returns zero rows, proceed with deployment and Flyway.

4. If the duplicate report returns rows, stop deployment.

5. Export the duplicate report for review:

   ```bash
   psql "$DATABASE_URL" \
     -f src/main/resources/db/predeploy/V107_phone_duplicate_check.sql \
     --csv > v107_phone_duplicates.csv
   ```

6. Manually review each duplicate group. Use
   `V107_phone_duplicate_cleanup_template.sql` as a safe template only after a
   business-approved survivor user has been chosen.

7. If merge/delete is risky, do not clean the group blindly. Leave the backend
   fail-closed duplicate-login behavior in place and route the case to support.

8. Rerun the duplicate report after cleanup.

9. Deploy only when the duplicate report returns zero rows.

10. Run Flyway migration.

11. Verify V107 completed successfully.

## Verification Queries

Duplicate phone check after cleanup:

```sql
WITH normalized AS (
    SELECT
        id,
        phone_number,
        regexp_replace(phone_number, '\D', '', 'g') AS digits
    FROM users
    WHERE phone_number IS NOT NULL
),
canonical AS (
    SELECT
        id,
        phone_number,
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
SELECT canonical_phone, count(*) AS row_count, array_agg(id ORDER BY id) AS user_ids
FROM canonical
GROUP BY canonical_phone
HAVING count(*) > 1
ORDER BY canonical_phone;
```

Refresh token duplicate hash check:

```sql
SELECT token, count(*) AS row_count, array_agg(id ORDER BY id) AS token_ids
FROM refresh_tokens
GROUP BY token
HAVING count(*) > 1;
```

Flyway V107 status:

```sql
SELECT installed_rank, version, description, script, installed_on, success
FROM flyway_schema_history
WHERE version = '107';
```

Failed Flyway migrations:

```sql
SELECT installed_rank, version, description, script, installed_on, success
FROM flyway_schema_history
WHERE success = false
ORDER BY installed_rank;
```

Users phone unique index existence:

```sql
SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename = 'users'
  AND indexname = 'ux_users_phone_number_normalized';
```

Refresh token indexes:

```sql
SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename = 'refresh_tokens'
  AND indexname IN (
      'ux_refresh_tokens_token_hash',
      'idx_refresh_tokens_user_id',
      'idx_refresh_tokens_user_device_active'
  )
ORDER BY indexname;
```
