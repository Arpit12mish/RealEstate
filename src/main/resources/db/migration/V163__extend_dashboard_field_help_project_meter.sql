-- Extends dashboard_field_help coverage for the Project Meter tab.
--
-- Context (verified against current code, not the original V71/V72 seed intent):
--   * PROJECT_METER_CONSTRUCTION_STAGE was missing a "remarks" entry and had
--     vague copy for weightPercent/progressPercent/status/verified.
--   * PROJECT_METER_AMENITY was seeded before the "Intelligence v1" fields
--     (category, categoryLabel, iconKey, rare, available, publicVisible,
--     active, categoryDisplayOrder) existed on ProjectAmenityProgressEntity.
--   * PROJECT_METER_LAND_UTILIZATION and PROJECT_METER_LOCATION_SCORE were
--     seeded with field_key values (totalLandArea, builtUpArea, openArea,
--     amenitiesArea, connectivityScore, infrastructureScore, socialInfraScore,
--     appreciationScore, safetyScore, greeneryScore, overallScore) that do not
--     match any current form field. Those rows are left in place (no
--     field_key ever matches them, so they are inert) rather than deleted --
--     this migration only adds rows for the field_key values the current
--     forms actually request (LandUtilizationForm / LocationScoreForm).
--   * There was no help module at all for the RERA Timeline panel, the
--     project-level Snapshot summary/actions, or the Price Insights override
--     form -- three new modules are added for these.
--
-- Nothing here changes calculation formulas, validation, permissions, or
-- publication behaviour -- this migration only adds/refreshes help text rows.

-- ── PROJECT_METER_CONSTRUCTION_STAGE -----------------------------------------

UPDATE dashboard_field_help SET
    short_help = 'Controls how much this stage contributes to overall construction progress.',
    detailed_help = 'Overall Construction Progress is a weighted average of every stage''s Progress %, using each stage''s Weight % as its share of the total. Example: if all stage weights add up to 100 and this stage is weighted 20 with Progress % at 50, it contributes 10 percentage points to the overall total (20 x 0.50). Raising this stage''s Progress % from 50 to 60, with everything else unchanged, adds 2 more percentage points to the overall total (20 x 0.10 = 2). A stage''s Weight % never represents the final project percentage by itself -- it only sets that one stage''s share.',
    why_needed = 'Used to calculate the single Overall Construction Progress % shown on this project''s Meter tab, on public project cards, and on the project page in the app and website.',
    validation_hint = 'Enter 0-100. There is no system rule forcing all stage weights to add up to 100, but keeping the total at 100 across every stage is strongly recommended -- it is what makes Overall Construction Progress read as a true percentage.'
WHERE module = 'PROJECT_METER_CONSTRUCTION_STAGE' AND field_key = 'weightPercent';

UPDATE dashboard_field_help SET
    short_help = 'Records how much work has been completed for this construction stage.',
    detailed_help = 'This value is combined with Weight % to calculate Overall Construction Progress -- see the Weight % help for the exact formula and a worked example. Progress % and Status are separate fields: the system does not keep them in sync automatically. Setting Progress % to 100 does not change Status to Completed, and choosing Status = Completed does not set Progress % to 100 -- set both yourself.',
    why_needed = 'Drives Overall Construction Progress together with Weight %.'
WHERE module = 'PROJECT_METER_CONSTRUCTION_STAGE' AND field_key = 'progressPercent';

UPDATE dashboard_field_help SET
    short_help = 'Where this stage currently stands, for reviewers and the completed/delayed counts above.',
    detailed_help = 'Status does not feed into the Overall Construction Progress % number -- only Weight % and Progress % do that. Status instead drives the Completed and Delayed counts in the summary boxes above the stage list, and (together with the RERA Timeline and compliance verification) feeds the timeline/discipline part of this builder''s Promise Fulfilment score on the Builder Credibility screen. Use ON_HOLD when work has paused for a non-schedule reason such as pending approvals, and DELAYED specifically when the stage is now running behind schedule.',
    why_needed = 'Feeds the Completed/Delayed summary counts and part of the builder''s Promise Fulfilment credibility score; does not itself change the progress percentage.'
WHERE module = 'PROJECT_METER_CONSTRUCTION_STAGE' AND field_key = 'status';

UPDATE dashboard_field_help SET
    short_help = 'Marks that a reviewer or admin has checked this stage''s Progress % and dates against real evidence.',
    detailed_help = 'Any dashboard editor with access to this project can toggle this field -- it is not restricted to Admin or Reviewer roles here (unlike the project-wide Snapshot Verify action, which is Admin/Reviewer only). Confirmed effect: the share of a builder''s construction stages marked Verified feeds part of that builder''s "Verification" credibility component on the Builder Credibility screen, alongside compliance-item verification and the project snapshot''s own Verified flag. Verified does not mean the stage is complete -- a stage can be accurately verified as "40% in progress."',
    why_needed = 'Signals the data has been checked, and feeds part of the builder''s Verification credibility component.',
    validation_hint = 'Only mark verified after checking the evidence -- nothing is checked automatically.'
WHERE module = 'PROJECT_METER_CONSTRUCTION_STAGE' AND field_key = 'verified';

INSERT INTO dashboard_field_help
    (module, field_key, field_label, short_help, detailed_help, why_needed, source_hint, example_value, validation_hint, display_order)
VALUES
(
    'PROJECT_METER_CONSTRUCTION_STAGE',
    'remarks',
    'Remarks',
    'Optional internal note about this stage.',
    'Shown to dashboard staff only -- never shown on the public app or website. Use it to record context a future editor or reviewer would need, such as why progress is stalled or what evidence was checked.',
    'Keeps a record of context behind the numbers, for later reviewers.',
    'Site visit notes, builder correspondence, or your own observation.',
    'Site visit confirmed slab casting complete on 3rd floor.',
    'Optional. Max 500 characters.',
    13
);

-- ── PROJECT_METER_COMPLIANCE --------------------------------------------------

UPDATE dashboard_field_help SET
    short_help = 'Marks that a reviewer or admin has checked this compliance item against the actual document.',
    detailed_help = 'Verified does not change this project''s Compliance Score (that score is based on Status alone -- Obtained/Approved items count in full, Pending or Expired items count at half weight, Not Applicable items are excluded). Verified instead feeds two builder-level credibility numbers: the ratio of verified compliance items is part of the builder''s "Verification" component, and Status combined with Verified together drive the builder''s "Compliance Strength" component.',
    why_needed = 'Confirms the document was actually checked, and feeds the builder''s Verification and Compliance Strength credibility components.',
    validation_hint = 'Only mark verified after checking the underlying document or reference value -- this is a manual attestation.'
WHERE module = 'PROJECT_METER_COMPLIANCE' AND field_key = 'verified';

UPDATE dashboard_field_help SET
    short_help = 'Whether this item has been obtained/approved, is pending, or does not apply to this project.',
    detailed_help = 'This is the field that drives Compliance Score: Obtained/Approved items count in full, Pending or Expired items count at half weight, and items marked Not Applicable are removed from the calculation entirely (they neither help nor hurt the score). Choose Not Applicable for a document type that genuinely does not apply to this project, rather than leaving it Pending indefinitely.',
    why_needed = 'Directly determines this project''s Compliance Score.'
WHERE module = 'PROJECT_METER_COMPLIANCE' AND field_key = 'status';

-- ── PROJECT_METER_AMENITY -----------------------------------------------------
-- "Intelligence v1" fields added to ProjectAmenityProgressEntity after the
-- original seed -- category, categoryLabel, iconKey, rare, available,
-- publicVisible, active, categoryDisplayOrder had no help rows at all.

UPDATE dashboard_field_help SET
    short_help = 'Controls how much this amenity contributes to overall Amenity Progress.',
    detailed_help = 'Amenity Progress is a weighted average of every eligible amenity''s Progress %, using each amenity''s Weight % as its share -- the same style of calculation as Construction Progress. An amenity is only counted if Available is checked and Status is not "Not Available"; excluded amenities are left out of both sides of the average rather than counting as zero.',
    why_needed = 'Used to calculate Amenity Progress %, shown on this project''s Meter tab.',
    validation_hint = 'Enter 0-100. There is no system rule forcing amenity weights to add up to 100, but keeping the total at 100 across all eligible amenities is recommended.'
WHERE module = 'PROJECT_METER_AMENITY' AND field_key = 'weightPercent';

UPDATE dashboard_field_help SET
    short_help = 'Completion percentage for this specific amenity.',
    detailed_help = 'Combined with Weight % to calculate Amenity Progress -- see the Weight % help for the exact rule. Only counted toward that score when Available is checked and Status is not "Not Available".'
WHERE module = 'PROJECT_METER_AMENITY' AND field_key = 'progressPercent';

UPDATE dashboard_field_help SET
    short_help = 'Current build status of this amenity, from not started to completed -- or not available.',
    detailed_help = 'Choosing "Not Available" removes this amenity from the Amenity Progress % calculation entirely, the same as unchecking Available below. Use whichever of the two the rest of this project already uses consistently, since either one alone is enough to exclude the amenity from scoring.',
    why_needed = 'Feeds Amenity Progress, and "Not Available" excludes the amenity from that score.'
WHERE module = 'PROJECT_METER_AMENITY' AND field_key = 'status';

UPDATE dashboard_field_help SET
    short_help = 'Marks that a reviewer or admin has checked this amenity''s status and progress.',
    detailed_help = 'A per-amenity attestation, separate from the project snapshot''s own Verified flag and from construction-stage/compliance verification -- it does not feed those other calculations.',
    why_needed = 'Signals the amenity''s status has been checked against the actual site or plans.',
    validation_hint = 'Only mark verified after checking -- this is a manual attestation.'
WHERE module = 'PROJECT_METER_AMENITY' AND field_key = 'verified';

INSERT INTO dashboard_field_help
    (module, field_key, field_label, short_help, detailed_help, why_needed, source_hint, example_value, validation_hint, display_order)
VALUES
(
    'PROJECT_METER_AMENITY',
    'category',
    'Category',
    'Which amenity group this belongs to, for example Sports, Wellness, or Security.',
    'Amenities are grouped by this value on the public project page and in the app''s amenity list. Leaving it blank, or choosing OTHER, places the amenity in a generic "Other Amenities" group.',
    'Groups amenities into readable sections for buyers instead of one long flat list.',
    'Judgment call based on what the amenity actually is.',
    'SPORTS',
    'Pick the closest match from the list.',
    20
),
(
    'PROJECT_METER_AMENITY',
    'categoryLabel',
    'Category Label',
    'Optional override for the heading shown above this amenity''s category group.',
    'Leave blank to use the category''s standard heading (for example "Sports Amenities"). Only the first non-blank Category Label found among a category''s amenities is actually used -- so this overrides the whole group''s heading project-wide, not just this one amenity''s display.',
    'Lets you customize a group heading without renaming the category itself.',
    NULL,
    'Recreation & Sports',
    'Optional. Max 100 characters.',
    21
),
(
    'PROJECT_METER_AMENITY',
    'iconKey',
    'Icon Key',
    'Which icon to display next to this amenity.',
    'Should match a key from the amenity icon catalog used by the app and website. A key that does not match anything in the catalog falls back to a generic icon rather than causing an error.',
    'Controls the visual icon shown for this amenity on the public page and app.',
    'Pick from the amenity icon suggestions shown when adding the amenity.',
    'pool',
    'Use a known icon key from the suggestion list.',
    22
),
(
    'PROJECT_METER_AMENITY',
    'rare',
    'Rare',
    'Shows a "Rare" badge on this amenity to call out something unusual.',
    'Purely a display badge -- it does not affect Amenity Progress or any other calculation.',
    'Highlights standout amenities that make this project distinctive.',
    NULL,
    'true',
    'Check only for genuinely uncommon amenities, so the badge stays meaningful.',
    23
),
(
    'PROJECT_METER_AMENITY',
    'available',
    'Available',
    'Whether this amenity type actually exists (or is planned) at this project.',
    'Uncheck for an amenity that this project simply does not offer, for example a project with no Clubhouse. An unavailable amenity is excluded from the Amenity Progress % calculation entirely -- it neither helps nor hurts the score, the same effect as Status = "Not Available".',
    'Keeps Amenity Progress % fair by excluding amenities that were never promised at this project.',
    NULL,
    'true',
    'Uncheck only when the amenity genuinely is not part of this project.',
    24
),
(
    'PROJECT_METER_AMENITY',
    'publicVisible',
    'Public Visible',
    'Whether this amenity is shown on the public project page and app.',
    'Unchecking this hides the amenity from buyers while keeping the record for internal dashboard tracking. An amenity only appears publicly, and only counts toward the public Amenity Progress %, when both Public Visible and Active are checked.',
    'Lets staff track an amenity internally before it is ready to announce to buyers.',
    NULL,
    'true',
    'Uncheck to hide from buyers while keeping the record for internal use.',
    25
),
(
    'PROJECT_METER_AMENITY',
    'active',
    'Active',
    'Whether this amenity record is currently in use.',
    'Works together with Public Visible -- both must be checked for the amenity to appear anywhere public. Uncheck instead of deleting when an amenity was added by mistake or is being retired, so the history is not lost.',
    'Soft-disables an amenity record without permanently deleting it.',
    NULL,
    'true',
    'Uncheck to retire a record; it stays in the database for reference.',
    26
),
(
    'PROJECT_METER_AMENITY',
    'categoryDisplayOrder',
    'Category Order',
    'Controls which amenity category/group appears first.',
    'Lower numbers appear first -- the same idea as Display Order, but for the whole category group rather than one amenity. The lowest Category Order value among a category''s amenities decides where that entire group is positioned.',
    'Lets you control the order amenity groups (for example Sports before Wellness) appear in.',
    NULL,
    '1',
    '0 or a positive whole number; 0 appears first.',
    27
);

-- ── PROJECT_METER_LAND_UTILIZATION --------------------------------------------
-- Current form fields: areaUnit, totalLandAreaSqm, residentialAreaSqm,
-- commercialAreaSqm, parksAreaSqm, openAreaSqm, parkingAreaSqm,
-- utilityAreaSqm (LandUtilizationForm.tsx / landUtilizationSchema). There is
-- no sourceLabel/remarks/verified field on this tab in the current model.

UPDATE dashboard_field_help SET
    short_help = 'Unit all area figures on this tab are measured in.',
    detailed_help = 'Changing this only changes the unit label shown next to the numbers -- it does not convert already-entered values. If you switch units, re-enter the area figures yourself.',
    why_needed = 'Tells reviewers and buyers whether the area figures are square meters or square feet.',
    validation_hint = 'Pick one unit and keep it consistent for this project.'
WHERE module = 'PROJECT_METER_LAND_UTILIZATION' AND field_key = 'areaUnit';

INSERT INTO dashboard_field_help
    (module, field_key, field_label, short_help, detailed_help, why_needed, source_hint, example_value, validation_hint, display_order)
VALUES
(
    'PROJECT_METER_LAND_UTILIZATION',
    'totalLandAreaSqm',
    'Total Land Area',
    'Total plot/land area for this project.',
    'This is the ceiling the other six area fields on this tab are checked against when you save: Residential + Commercial + Parks + Open + Parking + Utility area must not exceed this total.',
    'Used as the base of the area-distribution breakdown shown on this tab.',
    'Project''s approved layout plan or RERA-sanctioned plan.',
    '50000',
    'Cannot be negative. The other six area fields combined cannot exceed this value -- saving is blocked if they do.',
    1
),
(
    'PROJECT_METER_LAND_UTILIZATION',
    'residentialAreaSqm',
    'Residential Area',
    'Area used for residential towers or units.',
    NULL,
    'Shown in the area-distribution breakdown on this project''s Meter tab.',
    'Project''s approved layout plan or RERA-sanctioned plan.',
    '25000',
    'Cannot be negative; counts toward the Total Land Area allocation limit.',
    2
),
(
    'PROJECT_METER_LAND_UTILIZATION',
    'commercialAreaSqm',
    'Commercial Area',
    'Area used for commercial or retail space.',
    NULL,
    'Shown in the area-distribution breakdown on this project''s Meter tab.',
    'Project''s approved layout plan or RERA-sanctioned plan.',
    '3000',
    'Cannot be negative; counts toward the Total Land Area allocation limit.',
    3
),
(
    'PROJECT_METER_LAND_UTILIZATION',
    'parksAreaSqm',
    'Parks & Gardens',
    'Area used for parks and landscaped gardens.',
    NULL,
    'Shown in the area-distribution breakdown on this project''s Meter tab.',
    'Project''s approved layout plan or RERA-sanctioned plan.',
    '5000',
    'Cannot be negative; counts toward the Total Land Area allocation limit.',
    4
),
(
    'PROJECT_METER_LAND_UTILIZATION',
    'openAreaSqm',
    'Open / Green Area',
    'Open or green area that is not a dedicated park.',
    NULL,
    'Shown in the area-distribution breakdown on this project''s Meter tab.',
    'Project''s approved layout plan or RERA-sanctioned plan.',
    '4000',
    'Cannot be negative; counts toward the Total Land Area allocation limit.',
    5
),
(
    'PROJECT_METER_LAND_UTILIZATION',
    'parkingAreaSqm',
    'Parking Area',
    'Area used for parking.',
    NULL,
    'Shown in the area-distribution breakdown on this project''s Meter tab.',
    'Project''s approved layout plan or RERA-sanctioned plan.',
    '6000',
    'Cannot be negative; counts toward the Total Land Area allocation limit.',
    6
),
(
    'PROJECT_METER_LAND_UTILIZATION',
    'utilityAreaSqm',
    'Utility / Service Area',
    'Area used for utilities and services, such as the STP or substation.',
    NULL,
    'Shown in the area-distribution breakdown on this project''s Meter tab.',
    'Project''s approved layout plan or RERA-sanctioned plan.',
    '1500',
    'Cannot be negative; counts toward the Total Land Area allocation limit.',
    7
);

-- ── PROJECT_METER_LOCATION_SCORE ----------------------------------------------
-- Current form fields: metroScore, educationScore, healthcareScore,
-- retailScore, jobScore, leisureScore, currentStrengthScore,
-- futureGrowthScore, finalScore, appreciationPercent3Y, scoreSummary,
-- verified (LocationScoreForm.tsx / locationScoreSchema). Scores are 0-10.

UPDATE dashboard_field_help SET
    short_help = 'Marks that a reviewer has checked these location scores.',
    detailed_help = 'A location-specific verification flag, separate from the project snapshot''s own Verified flag and from construction-stage/compliance verification -- it does not feed those other calculations.',
    validation_hint = 'Mark only after checking the assessment.'
WHERE module = 'PROJECT_METER_LOCATION_SCORE' AND field_key = 'verified';

INSERT INTO dashboard_field_help
    (module, field_key, field_label, short_help, detailed_help, why_needed, source_hint, example_value, validation_hint, display_order)
VALUES
(
    'PROJECT_METER_LOCATION_SCORE',
    'metroScore',
    'Metro / Transit',
    'How convenient this location is to metro or rail transit, scored 0-10.',
    'One of six radar axes shown on this tab. The six axes are averaged on-screen as a "Radar Average" for reference, but the score actually published to buyers comes from the separate Final Score field below, not automatically from this average.',
    'One input a reviewer weighs when setting the overall Final Score.',
    'Distance/travel-time to the nearest metro or rail station.',
    '7',
    '0-10. Higher is better.',
    1
),
(
    'PROJECT_METER_LOCATION_SCORE',
    'educationScore',
    'Education',
    'Proximity to good schools and colleges, scored 0-10.',
    'One of six radar axes averaged on-screen for reference -- see Metro / Transit help for how these relate to Final Score.',
    'One input a reviewer weighs when setting the overall Final Score.',
    'Nearby schools/colleges and their reputation.',
    '7',
    '0-10. Higher is better.',
    2
),
(
    'PROJECT_METER_LOCATION_SCORE',
    'healthcareScore',
    'Healthcare',
    'Proximity to hospitals and clinics, scored 0-10.',
    'One of six radar axes averaged on-screen for reference -- see Metro / Transit help for how these relate to Final Score.',
    'One input a reviewer weighs when setting the overall Final Score.',
    'Nearby hospitals/clinics and their quality.',
    '7',
    '0-10. Higher is better.',
    3
),
(
    'PROJECT_METER_LOCATION_SCORE',
    'retailScore',
    'Retail & Shopping',
    'Proximity to malls, markets, and daily shopping, scored 0-10.',
    'One of six radar axes averaged on-screen for reference -- see Metro / Transit help for how these relate to Final Score.',
    'One input a reviewer weighs when setting the overall Final Score.',
    'Nearby malls/markets for daily and weekly shopping.',
    '6',
    '0-10. Higher is better.',
    4
),
(
    'PROJECT_METER_LOCATION_SCORE',
    'jobScore',
    'Job Hubs',
    'Proximity to job hubs and business districts, scored 0-10.',
    'One of six radar axes averaged on-screen for reference -- see Metro / Transit help for how these relate to Final Score.',
    'One input a reviewer weighs when setting the overall Final Score.',
    'Distance/travel-time to major employment hubs.',
    '8',
    '0-10. Higher is better.',
    5
),
(
    'PROJECT_METER_LOCATION_SCORE',
    'leisureScore',
    'Leisure & Dining',
    'Proximity to parks, entertainment, and dining, scored 0-10.',
    'One of six radar axes averaged on-screen for reference -- see Metro / Transit help for how these relate to Final Score.',
    'One input a reviewer weighs when setting the overall Final Score.',
    'Nearby parks, restaurants, and entertainment options.',
    '6',
    '0-10. Higher is better.',
    6
),
(
    'PROJECT_METER_LOCATION_SCORE',
    'currentStrengthScore',
    'Current Strength',
    'Composite score for how strong this location already is today, 0-10.',
    'A separate, manually-assessed composite -- not an automatic average of the six radar axes above. Enter the reviewer''s overall judgment of the location''s present-day strength.',
    'Feeds the reviewer''s overall assessment shown alongside Future Growth and Final Score.',
    'Reviewer''s own assessment, informed by the radar axes above.',
    '7',
    '0-10.',
    7
),
(
    'PROJECT_METER_LOCATION_SCORE',
    'futureGrowthScore',
    'Future Growth',
    'Composite score for the location''s expected future growth potential, 0-10.',
    'A separate, manually-assessed composite -- not an automatic average of the six radar axes above. This is a forecast, distinct from Current Strength, which reflects today.',
    'Feeds the reviewer''s overall assessment shown alongside Current Strength and Final Score.',
    'Reviewer''s own forecast, informed by planned infrastructure, upcoming transit, etc.',
    '8',
    '0-10.',
    8
),
(
    'PROJECT_METER_LOCATION_SCORE',
    'finalScore',
    'Final Score',
    'The single Location Score published to buyers, 0-10.',
    'This is the number actually shown on the public site and app -- it is not automatically calculated from the six radar axes. The form shows a "Radar Average" of those axes as a cross-check and warns when Final Score differs from it by more than 1 point, but saving does not force them to match; set Final Score to the reviewer''s overall judgment.',
    'This is what is published to buyers as this project''s Location Score.',
    'Reviewer''s overall judgment, informed by the radar axes, Current Strength, and Future Growth above.',
    '7.5',
    '0-10. If this differs a lot from the Radar Average, double check it is intentional.',
    9
),
(
    'PROJECT_METER_LOCATION_SCORE',
    'appreciationPercent3Y',
    '3-Year Appreciation',
    'Expected or observed 3-year price appreciation for this location, as a percentage.',
    'A location-level forecast or historical figure -- separate from this specific project''s own Price Appreciation (Launch Price to Current Price) shown on the Price Insights tab.',
    'Gives buyers a location-level growth expectation distinct from this project''s own pricing.',
    'Local market research or historical price trend data for the area.',
    '18',
    '-100 to 9999.',
    10
),
(
    'PROJECT_METER_LOCATION_SCORE',
    'scoreSummary',
    'Score Summary',
    'Short narrative explaining the location scores, shown alongside them.',
    NULL,
    'Gives buyers context behind the numeric scores.',
    'Reviewer''s own summary of the location assessment.',
    'Well connected to the upcoming metro corridor with strong social infrastructure nearby.',
    'Optional. Max 1000 characters.',
    11
);

-- ── PROJECT_METER_TIMELINE (new module) ---------------------------------------
-- RERA Timeline panel fields (ReraTimelinePanel.tsx, updateProjectTimeline).

INSERT INTO dashboard_field_help
    (module, field_key, field_label, short_help, detailed_help, why_needed, source_hint, example_value, validation_hint, display_order)
VALUES
(
    'PROJECT_METER_TIMELINE',
    'originalCompletionDate',
    'Original Completion Date',
    'The completion date originally promised for this project, before any RERA extension.',
    'This is the baseline every delay figure is measured against. If left blank, calculations fall back to Latest RERA Completion Date instead.',
    'Used to calculate how many days late the project is against its very first promise, and to decide the "Completed on time" badge.',
    'Original RERA registration certificate, or the project''s initial possession date.',
    '2025-06-30',
    'Use the date from the first RERA registration, not a later revised date.',
    1
),
(
    'PROJECT_METER_TIMELINE',
    'latestReraCompletionDate',
    'Latest RERA Completion Date',
    'The current, officially approved completion date, after any RERA-approved extensions.',
    'This is the date actually used to decide whether the project shows as Delayed today. If left blank, calculations fall back to Original Completion Date. Every time a builder gets a new RERA extension approved, update this field (and increase RERA Extension Count below) rather than editing Original Completion Date.',
    'Drives the Delayed badge and the days-delayed figure shown on the dashboard, public site, and app.',
    'Latest RERA extension order or certificate.',
    '2025-09-30',
    'Should be on or after Original Completion Date.',
    2
),
(
    'PROJECT_METER_TIMELINE',
    'actualCompletionDate',
    'Actual Completion Date',
    'The real date construction finished. Leave blank while the project is still ongoing.',
    'Filling this in freezes the timeline: the badge switches from an ongoing On Track / Delayed state to a completed state (Completed on time, Completed within revised RERA timeline, or Completed late), and the delay-day figures stop moving with today''s date. While this stays blank, delay is measured live against today''s date every time the snapshot is recalculated.',
    'Marks the project as finished for timeline purposes and freezes the delay calculation.',
    'Occupation Certificate date or builder handover confirmation.',
    '2025-10-05',
    'Leave blank until construction has actually finished.',
    3
),
(
    'PROJECT_METER_TIMELINE',
    'reraExtensionCount',
    'RERA Extension Count',
    'How many times RERA has approved a new completion date for this project.',
    'A manually maintained counter -- it does not increment on its own. It only changes the wording of the on-track badge (for example "RERA timeline extended 2 times"); the delay-day numbers come entirely from the three dates above, not from this count.',
    'Gives reviewers and buyers context on how many times the promised date has moved.',
    'Count of RERA extension orders on file for this project.',
    '1',
    '0 or a positive whole number.',
    4
);

-- ── PROJECT_METER_SNAPSHOT (new module) ---------------------------------------
-- Snapshot summary panel: Recalculate/Verify actions and the metric concepts
-- shown there (SnapshotPanel.tsx).

INSERT INTO dashboard_field_help
    (module, field_key, field_label, short_help, detailed_help, why_needed, source_hint, example_value, validation_hint, display_order)
VALUES
(
    'PROJECT_METER_SNAPSHOT',
    'recalculate',
    'Recalculate',
    'Recomputes this project''s saved progress, compliance, amenity, price and timeline numbers from the latest data you have entered.',
    'Editing a construction stage, compliance item, amenity, or price history row updates that row immediately, but the combined totals used everywhere else -- this project''s public Meter tab, its card in search results, the mobile app, and builder credibility -- are stored separately as a snapshot and are not updated automatically. Nothing outside the dashboard reflects your edits until someone clicks Recalculate (or an admin runs the platform-wide recalculation job). The RERA Timeline badge is refreshed at the same time.',
    'This is the action that actually publishes Meter-tab edits to the public site and app.',
    NULL,
    NULL,
    'Click after finishing a batch of edits. Safe to run repeatedly -- it fully replaces the stored numbers each time from current data.',
    1
),
(
    'PROJECT_METER_SNAPSHOT',
    'verify',
    'Verify / Unverify',
    'Marks this project''s whole snapshot as checked -- distinct from verifying individual stages, compliance items, or amenities.',
    'Restricted to Admin and Reviewer roles; Data Entry accounts can save data but cannot use this control. Marking the snapshot Verified sets the Verified badge shown here and feeds part of this builder''s overall "Verification" credibility component, alongside the share of individual construction stages and compliance items marked verified.',
    'Gives buyers and internal reviewers a top-level trust signal for this project''s numbers.',
    NULL,
    NULL,
    'Only Admin or Reviewer accounts can verify or unverify a snapshot.',
    2
),
(
    'PROJECT_METER_SNAPSHOT',
    'constructionProgress',
    'Construction Progress',
    'The saved Overall Construction Progress % from the last Recalculate.',
    'Same weighted-average figure explained on the Construction Stages tab''s Weight % help. This card shows the value exactly as it was last saved, which is what the public site and app currently display.',
    NULL,
    NULL,
    NULL,
    NULL,
    3
),
(
    'PROJECT_METER_SNAPSHOT',
    'complianceScore',
    'Compliance Score',
    'How much of this project''s compliance checklist is Obtained or Approved, weighted by status.',
    'Calculated as (fully Obtained/Approved items x 1, plus Pending or Expired items x 0.5) divided by total applicable items x 100. Items marked Not Applicable are excluded from the count entirely. This score is based on Status only -- it does not use the Verified flag on compliance items; Verified instead feeds a separate builder credibility component.',
    NULL,
    NULL,
    NULL,
    NULL,
    4
),
(
    'PROJECT_METER_SNAPSHOT',
    'amenityProgress',
    'Amenity Progress',
    'The weighted completion percentage across this project''s amenities.',
    'Uses the same weighted-average approach as Construction Progress, but only counts amenities where Available is checked and Status is not "Not Available". Excluded amenities are left out of both sides of the average, not counted as zero.',
    NULL,
    NULL,
    NULL,
    NULL,
    5
),
(
    'PROJECT_METER_SNAPSHOT',
    'locationScore',
    'Location Score',
    'This project''s Final Score from the Location Score tab, out of 10.',
    'Mirrors the Final Score field on the Location Score tab directly -- it is not an automatic average of that tab''s individual radar axes.',
    NULL,
    NULL,
    NULL,
    NULL,
    6
),
(
    'PROJECT_METER_SNAPSHOT',
    'priceAppreciation',
    'Price Appreciation',
    'Percentage change from Launch Price to Current Price, from Price Insights.',
    'Calculated as (Current Price minus Launch Price) divided by Launch Price x 100. Left blank if either price is missing, or if Launch Price is zero.',
    NULL,
    NULL,
    NULL,
    NULL,
    7
);

-- ── PROJECT_METER_PRICE_INSIGHTS (new module) ---------------------------------
-- Manual override form on the Price Insights tab (PriceInsightsPanel.tsx),
-- distinct from the Price History rows that normally drive these values.

INSERT INTO dashboard_field_help
    (module, field_key, field_label, short_help, detailed_help, why_needed, source_hint, example_value, validation_hint, display_order)
VALUES
(
    'PROJECT_METER_PRICE_INSIGHTS',
    'launchPrice',
    'Launch Price',
    'This project''s price per square foot at launch, in rupees.',
    'Normally filled in automatically from the earliest row in Price History every time someone clicks Recalculate. Saving a value here sets a manual override -- but if any Price History rows exist, the next Recalculate will overwrite it with the earliest Price History entry again. To make a manual value stick, either remove the conflicting Price History rows or re-enter it after each Recalculate.',
    'Used as the baseline for the Appreciation % shown on this project''s Meter tab.',
    'Builder''s original launch price list, or the earliest Price History entry.',
    '6500',
    'Rupees per square foot. Cannot be negative.',
    1
),
(
    'PROJECT_METER_PRICE_INSIGHTS',
    'currentPrice',
    'Current Price',
    'This project''s current price per square foot, in rupees.',
    'Same auto-fill-from-Price-History and manual-override behaviour as Launch Price, but sourced from the most recent Price History row instead of the earliest.',
    'The other half of the Appreciation % calculation.',
    'Builder''s current price list, or the latest Price History entry.',
    '8200',
    'Rupees per square foot. Cannot be negative.',
    2
),
(
    'PROJECT_METER_PRICE_INSIGHTS',
    'averageAreaPrice',
    'Area Average Price',
    'The average price per square foot for this locality, for comparison.',
    'Auto-filled from the most recent Price History row that has a locality price entered, falling back to the project''s own average price if none is available. Not part of the Appreciation % calculation -- shown purely as a market comparison.',
    'Lets a buyer compare this project''s pricing against the surrounding locality.',
    'Local market research, comparable project listings, or a Price History entry''s locality figure.',
    '7800',
    'Rupees per square foot. Cannot be negative.',
    3
);
