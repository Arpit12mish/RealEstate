#!/usr/bin/env node
/**
 * SFS Dashboard — Full Review/Publish Workflow Test
 *
 * This wrapper runs the dashboard state-machine test, including:
 * DRAFT submit, review queue visibility, reviewer reject/approve, publish guard,
 * public visibility, and role-negative checks.
 *
 * Required env:
 *   BASE_URL
 *   ADMIN_TOKEN or ADMIN_EMAIL + ADMIN_PASSWORD
 *   DATA_ENTRY_TOKEN or DATA_ENTRY_EMAIL + DATA_ENTRY_PASSWORD
 *   REVIEWER_TOKEN or REVIEWER_EMAIL + REVIEWER_PASSWORD
 */

require('./dashboard-state-machine-test');
