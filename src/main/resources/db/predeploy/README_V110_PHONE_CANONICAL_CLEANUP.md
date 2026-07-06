# V110 Phone Canonicalization Predeploy Runbook

V110 (`V110__enforce_canonical_phone_number_format.sql`) adds a database-level
CHECK constraint requiring every non-null `users.phone_number` to match
`^\+91[6-9][0-9]{9}$`. The migration intentionally aborts before making any
schema change if existing rows are not already canonical, e.g.:

```
V110 aborted: N users.phone_number value(s) are not in canonical +91XXXXXXXXXX form.
```

This is not a bug in V110 - it is the migration doing its job. **Do not weaken
or remove the constraint.** Clean up the data first, then let V110 run.

> **Never bulk-generate fake phone numbers in production. The `+918...`
> generated dummy-number approach is local/dev only and must never be used on
> real users.**

Hard rules, always:
- Do not auto-merge or auto-delete users.
- Do not set `phone_number` to `NULL` automatically.
- Do not touch `email` unless a human has explicitly reviewed that specific row.
- Do not run any mass `UPDATE users SET phone_number = ...` against production
  outside of the guarded, collision-checked scripts below.

## Files

- `V110_phone_canonical_audit.sql` - read-only. Quick technical audit: lists
  every non-canonical row, extracted digits, proposed canonical form, and
  classifies `SAFE_TO_BACKFILL` / `MANUAL_REVIEW`.
- `V110_phone_canonical_safe_backfill.sql` - runs a collision check first and
  **aborts the whole transaction with zero rows changed** if anything would
  collide, then updates only rows with an unambiguous, collision-free
  canonical form. This is the only script in this directory that mutates data
  automatically, and only for clearly derivable, user-owned numbers - never
  fabricated ones.
- `V110_phone_canonical_production_review.sql` - read-only. The detailed,
  CSV-friendly report for production sign-off: same data as the audit, plus
  `is_admin`/`is_verified` flags, a synthetic-placeholder-email check, and a
  plain-English `recommended_action` per row (never executable SQL).
- `V110_phone_canonical_manual_resolution_template.sql` - **not executable by
  default**. Every mutating statement is commented out. Copy one case (A/B/C)
  at a time, fill in real values after review, run inside an explicit
  transaction.
- `V107_phone_duplicate_check.sql` / `V107_phone_duplicate_cleanup_template.sql`
  - the equivalent, earlier runbook pair for the V107 migration (duplicate
  phone detection before the unique index was added). Same non-destructive
  conventions.
- `V110_LOCAL_DEV_ONLY_synthetic_seed_phone_backfill.sql` - **LOCAL DEV ONLY -
  NEVER PRODUCTION.** For the case the safe-backfill script correctly refuses
  to touch: local seed rows created with fake sequential digit strings
  (`phone_<digits>@phone.local`) that were never real phone numbers to begin
  with. Only rewrites rows matching that exact synthetic email pattern, to a
  deterministic, collision-checked `+917` + zero-padded-id placeholder. Never
  run this against anything but a local database.

---

## 1. Local / dev cleanup

**LOCAL DEV ONLY - NEVER PRODUCTION.** A local/test database can contain
throwaway seed data that is fine to bulk-fix in ways that would be completely
unacceptable against real users (e.g. generating synthetic `+918XXXXXXXXX`
numbers for seed rows that have no real phone). If you ever write or see a
script that does this:

- It must be clearly labeled `LOCAL DEV ONLY - NEVER PRODUCTION` in a comment
  at the top of the file.
- It must never be pointed at a production `DATABASE_URL`.
- It must not live in this `predeploy/` directory unless that label is
  present and unmissable.

For local development, run the audit and safe-backfill scripts exactly as
described in the Production section below, just against your local database
(`localhost`, gitignored `application.yml` credentials). Nothing in this
directory currently performs bulk fake-number generation - this section exists
so that if such convenience tooling is ever added for local seeding, it is
never confused with the production-safe scripts.

```bash
export PGPASSWORD='<local-db-password-from-your-gitignored-application.yml>'
psql -h localhost -p 5432 -U sfs_user -d sfs_db \
  -f src/main/resources/db/predeploy/V110_phone_canonical_audit.sql

psql -h localhost -p 5432 -U sfs_user -d sfs_db \
  -f src/main/resources/db/predeploy/V110_phone_canonical_safe_backfill.sql

psql -h localhost -p 5432 -U sfs_user -d sfs_db \
  -c "SELECT COUNT(*) FROM users WHERE phone_number IS NOT NULL AND phone_number !~ '^\+91[6-9][0-9]{9}\$';"
```

(URL-encode any `@`/`:`/`/` in the password if you prefer a single
`postgresql://` connection string instead of `-h`/`-U`/`-d` flags - a literal
`@` in the password breaks host parsing.)

---

## 2. Production cleanup

Production has a small number of users (roughly two dozen). Every invalid row
is resolved **one at a time, by a human**, using the manual resolution
template. Nothing in this section auto-generates data.

1. **Take a DB backup/snapshot.** Do not proceed without one you can restore from.

2. **Run the read-only production review script:**

   ```bash
   psql "$DATABASE_URL" -f src/main/resources/db/predeploy/V110_phone_canonical_production_review.sql
   ```

3. **Export it as CSV for offline review:**

   ```bash
   psql "$DATABASE_URL" \
     -f src/main/resources/db/predeploy/V110_phone_canonical_production_review.sql \
     --csv > v110_phone_production_review.csv
   ```

4. **Review each row manually.** The `classification` and `recommended_action`
   columns tell you which case applies:
   - `SAFE_CANONICALIZATION_CANDIDATE` -> Case A (single-row typo/format fix)
     after confirming ownership.
   - `DUPLICATE_COLLISION` -> Case B (survivor selection, dependent-row
     inventory, session revocation, business approval).
   - `INVALID_NON_MOBILE` -> contact the user/support for the correct number,
     then Case A.
   - `MANUAL_SUPPORT_REVIEW` -> escalate to business (includes any admin
     account, regardless of how derivable its number looks - handle admins
     with extra caution).

5. **For each invalid user, verify ownership/business meaning** before
   changing anything (support ticket, re-verification, account history).

6. **Apply one manual transaction per user or duplicate group** using
   `V110_phone_canonical_manual_resolution_template.sql`. No mass updates.

7. **Re-run the review script** to confirm progress:

   ```bash
   psql "$DATABASE_URL" -f src/main/resources/db/predeploy/V110_phone_canonical_production_review.sql
   ```

8. **Confirm this returns `0`:**

   ```sql
   SELECT COUNT(*)
   FROM users
   WHERE phone_number IS NOT NULL
     AND phone_number !~ '^\+91[6-9][0-9]{9}$';
   ```

9. **Only then** deploy the jar and allow Flyway to run V110 (see
   `docs/EC2_DEPLOYMENT.md` for the standard deploy steps).

10. **Verify V110 succeeded:**

    ```sql
    SELECT installed_rank, version, description, script, installed_on, success
    FROM flyway_schema_history
    WHERE version = '110';
    ```

### Exact production-safe psql commands

Use placeholders only - never put real production credentials in a tracked
file, shared shell history, or this repo:

```bash
export DATABASE_URL='postgresql://<user>:<password>@<host>:5432/<db>'

psql "$DATABASE_URL" \
  -f src/main/resources/db/predeploy/V110_phone_canonical_production_review.sql \
  --csv > v110_phone_production_review.csv
```

If the password contains reserved URL characters (`@`, `:`, `/`), either
URL-encode them or use `-h/-p/-U/-d` flags with `PGPASSWORD` set in the shell
environment instead of a single connection string - never hardcode the
decoded/plain password in a file.

To reach the production host itself (SSH access, key path), follow the
existing access procedure in `docs/EC2_DEPLOYMENT.md` rather than duplicating
infrastructure details here.

---

## Verification Queries

Remaining invalid count (must be `0` before V110 can succeed):

```sql
SELECT COUNT(*) FROM users
WHERE phone_number IS NOT NULL
  AND phone_number !~ '^\+91[6-9][0-9]{9}$';
```

Flyway V110 status:

```sql
SELECT installed_rank, version, description, script, installed_on, success
FROM flyway_schema_history
WHERE version = '110';
```

Failed Flyway migrations:

```sql
SELECT installed_rank, version, description, script, installed_on, success
FROM flyway_schema_history
WHERE success = false
ORDER BY installed_rank;
```

CHECK constraint existence after deploy:

```sql
SELECT conname, pg_get_constraintdef(oid)
FROM pg_constraint
WHERE conrelid = 'users'::regclass
  AND conname = 'ck_users_phone_number_canonical_e164';
```
