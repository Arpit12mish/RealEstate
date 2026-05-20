-- PROJECT_METER_COMPLIANCE
INSERT INTO dashboard_field_help
    (module, field_key, field_label, short_help, detailed_help, why_needed, source_hint, example_value, validation_hint, display_order)
VALUES
(
    'PROJECT_METER_COMPLIANCE',
    'itemGroup',
    'Group',
    'Category of the compliance document.',
    'Groups compliance items into RERA, environmental, fire safety, structural, legal title, utility, occupancy, and other types.',
    'Helps organize and filter compliance items by regulatory category.',
    'Use official RERA documents, government portals, builder approvals, or legal records.',
    'RERA',
    'Select the closest compliance group from the dropdown.',
    1
),
(
    'PROJECT_METER_COMPLIANCE',
    'itemKey',
    'Item Key',
    'Machine-readable identifier for this compliance document.',
    'Used internally to uniquely identify a compliance item within its group. Should be consistent across projects.',
    'Enables linking, filtering, and referencing compliance items programmatically.',
    'Use standard codes like RERA_REGISTRATION, FIRE_NOC, OC_CERTIFICATE from the internal code list.',
    'RERA_REGISTRATION',
    'Uppercase letters, numbers, and underscores only. Example: FIRE_NOC.',
    2
),
(
    'PROJECT_METER_COMPLIANCE',
    'itemLabel',
    'Item Label',
    'Human-readable name of this compliance document.',
    'Shown to admins, reviewers, and app users in the compliance section.',
    'Makes the compliance document understandable for non-technical users.',
    'Use the official document name from the certificate or approval letter.',
    'RERA Registration Certificate',
    'Keep it short and clear. Match the official document name.',
    3
),
(
    'PROJECT_METER_COMPLIANCE',
    'status',
    'Status',
    'Current status of this compliance item.',
    'Indicates whether the approval has been obtained, is pending, is not applicable for this project, or has expired.',
    'Helps reviewers assess the regulatory standing of the project.',
    'Check the actual certificate or builder communication for current status.',
    'OBTAINED',
    'Select the most accurate status from the dropdown.',
    4
),
(
    'PROJECT_METER_COMPLIANCE',
    'displayOrder',
    'Display Order',
    'Controls the order in which this item appears in the compliance list.',
    'Lower numbers appear first. RERA and occupancy items are typically shown at the top.',
    'Ensures the most important compliance items are seen first.',
    'Use logical regulatory priority — RERA first, then environmental, then fire, etc.',
    '1',
    'Enter a non-negative number.',
    5
),
(
    'PROJECT_METER_COMPLIANCE',
    'valueText',
    'Value / Reference',
    'The actual certificate number, registration ID, or reference text.',
    'Stores the specific value associated with this compliance item, such as a RERA number, NOC number, or approval reference.',
    'Provides the verifiable reference that reviewers and users can cross-check.',
    'Copy from the official certificate, approval letter, or government portal.',
    'PRM/KA/RERA/1251/309/PR/171017/002270',
    'Enter exactly as it appears on the official document.',
    6
),
(
    'PROJECT_METER_COMPLIANCE',
    'documentUrl',
    'Document URL',
    'Link to the scanned document or official certificate.',
    'A URL pointing to the uploaded document in S3 or an external government portal.',
    'Allows reviewers to verify the compliance item against the source document.',
    'Upload the document via the media presign endpoint and paste the resulting URL.',
    'https://cdn.yourdomain.com/docs/rera-cert.pdf',
    'Must be a valid URL. Use the dashboard upload flow for S3 documents.',
    7
),
(
    'PROJECT_METER_COMPLIANCE',
    'remarks',
    'Remarks',
    'Optional notes about this compliance item.',
    'Internal notes visible to admins and reviewers. Not shown to app users.',
    'Provides context about pending issues, renewal dates, or collection notes.',
    'Add notes like expiry date, pending renewal, or data collection status.',
    'Certificate valid until December 2026. Renewal in progress.',
    'Keep brief and factual.',
    8
),
(
    'PROJECT_METER_COMPLIANCE',
    'verified',
    'Verified',
    'Marks whether this compliance item has been verified by a reviewer.',
    'Verified means the document or reference has been cross-checked against an official source.',
    'Improves trust score and signals data quality to app users.',
    'Reviewer should mark verified only after checking the official document or government portal.',
    'true',
    'Only mark verified when the document has been physically checked.',
    9
);

-- PROJECT_METER_AMENITY
INSERT INTO dashboard_field_help
    (module, field_key, field_label, short_help, detailed_help, why_needed, source_hint, example_value, validation_hint, display_order)
VALUES
(
    'PROJECT_METER_AMENITY',
    'amenityCode',
    'Amenity Code',
    'Machine-readable identifier for this amenity.',
    'Used internally to uniquely identify an amenity. Should be consistent across projects.',
    'Enables filtering, grouping, and referencing amenities programmatically.',
    'Use standard codes like SWIMMING_POOL, CLUBHOUSE, GYM from the internal list.',
    'SWIMMING_POOL',
    'Uppercase letters, numbers, and underscores only. Example: CLUBHOUSE.',
    1
),
(
    'PROJECT_METER_AMENITY',
    'amenityLabel',
    'Amenity Label',
    'Display name of the amenity shown to app users.',
    'This is the human-readable name shown in the amenities list in the app.',
    'Makes the amenity clear and recognizable to buyers.',
    'Use the builder brochure, site plan, or project marketing materials.',
    'Olympic-size Swimming Pool',
    'Keep it descriptive but concise.',
    2
),
(
    'PROJECT_METER_AMENITY',
    'status',
    'Status',
    'Current construction status of this amenity.',
    'Indicates whether the amenity is planned, under construction, fully completed, or no longer available.',
    'Helps buyers understand which amenities are ready and which are still being built.',
    'Use site inspection report, builder update, or engineer confirmation.',
    'UNDER_CONSTRUCTION',
    'Select the most accurate status from the dropdown.',
    3
),
(
    'PROJECT_METER_AMENITY',
    'progressPercent',
    'Progress %',
    'Completion percentage of this amenity.',
    'Shows how much of the amenity construction is finished. Relevant when status is Under Construction.',
    'Used to show buyers a progress bar for each amenity.',
    'Use site inspection report or engineer estimate.',
    '60',
    'Enter value between 0 and 100.',
    4
),
(
    'PROJECT_METER_AMENITY',
    'weightPercent',
    'Weight %',
    'Importance of this amenity in the overall amenity score.',
    'Defines how much this amenity contributes to the total amenity score shown on the project meter.',
    'Used to calculate the amenity score in the Project Meter.',
    'Assign higher weight to premium or flagship amenities like clubhouse or pool.',
    '15',
    'Enter value between 0 and 100. Total weights for all amenities should ideally equal 100.',
    5
),
(
    'PROJECT_METER_AMENITY',
    'displayOrder',
    'Order',
    'Controls the order in which this amenity appears in the list.',
    'Lower numbers appear first. Show flagship amenities (pool, clubhouse) near the top.',
    'Ensures the most important amenities are seen first by buyers.',
    'Use builder priority or sales team preference.',
    '1',
    'Enter a non-negative number.',
    6
),
(
    'PROJECT_METER_AMENITY',
    'remarks',
    'Remarks',
    'Optional internal notes about this amenity.',
    'Notes visible only to admins and reviewers. Not shown to app users.',
    'Provides context about collection source or pending verification.',
    'Add notes like expected completion date or data source.',
    'Expected completion by Q3 2025 per site engineer.',
    'Keep brief and factual.',
    7
),
(
    'PROJECT_METER_AMENITY',
    'verified',
    'Verified',
    'Marks whether this amenity data has been verified by a reviewer.',
    'Verified means the amenity status has been cross-checked against a site inspection or builder report.',
    'Improves the project trust score and data quality.',
    'Reviewer should mark verified only after site visit or confirmed builder update.',
    'true',
    'Only mark verified when the information has been confirmed from a reliable source.',
    8
);

-- PROJECT_METER_PRICE_HISTORY
INSERT INTO dashboard_field_help
    (module, field_key, field_label, short_help, detailed_help, why_needed, source_hint, example_value, validation_hint, display_order)
VALUES
(
    'PROJECT_METER_PRICE_HISTORY',
    'yearLabel',
    'Year / Month',
    'The time period this price point represents.',
    'Use YYYY for annual data or YYYY-MM for monthly precision. Displayed as the X-axis label in price charts.',
    'Defines the timeline of price changes shown in the price history chart.',
    'Use project launch records, broker data, or builder pricing updates.',
    '2024 or 2024-06',
    'Use YYYY or YYYY-MM format only. Example: 2024 or 2024-06.',
    1
),
(
    'PROJECT_METER_PRICE_HISTORY',
    'projectPrice',
    'Project Price',
    'Project price per sq ft at this point in time (INR).',
    'The actual price per square foot at which the project was being sold or offered during this period.',
    'Powers the price history chart showing how the project price has changed over time.',
    'Collect from builder price list, registration data, or broker records.',
    '7200',
    'Enter price per sq ft in INR. No decimals needed.',
    2
),
(
    'PROJECT_METER_PRICE_HISTORY',
    'averageAreaPrice',
    'Area Avg Price',
    'Average locality price per sq ft for comparison (optional).',
    'The average market price per sq ft for similar properties in the same locality during this period.',
    'Allows buyers to compare the project price against the local market average.',
    'Collect from property portals, registrar data, or market research reports.',
    '6800',
    'Optional. Enter price per sq ft in INR if available.',
    3
),
(
    'PROJECT_METER_PRICE_HISTORY',
    'displayOrder',
    'Display Order',
    'Controls the order of this point in the price history chart.',
    'Lower numbers appear first (left side of chart). Use chronological order with oldest first.',
    'Ensures the price history chart shows data in correct time sequence.',
    'Use chronological sequence starting from 1 for the oldest data point.',
    '1',
    'Enter a non-negative number. Use chronological order.',
    4
),
(
    'PROJECT_METER_PRICE_HISTORY',
    'verified',
    'Verified',
    'Marks whether this price data has been verified by a reviewer.',
    'Verified means the price was cross-checked against an official or reliable source.',
    'Improves trust score and data quality for price history.',
    'Reviewer should verify against registration data, broker records, or official price list.',
    'true',
    'Only mark verified when the data source has been confirmed.',
    5
);

-- PROJECT_METER_PAYMENT_MILESTONE
INSERT INTO dashboard_field_help
    (module, field_key, field_label, short_help, detailed_help, why_needed, source_hint, example_value, validation_hint, display_order)
VALUES
(
    'PROJECT_METER_PAYMENT_MILESTONE',
    'milestoneCode',
    'Milestone Code',
    'Machine-readable identifier for this payment milestone.',
    'Used internally to link milestones to construction stages and identify them programmatically.',
    'Enables consistent referencing and linking of payment milestones to construction progress.',
    'Use standard codes like ON_BOOKING, ON_FOUNDATION, ON_HANDOVER.',
    'ON_FOUNDATION',
    'Uppercase letters, numbers, and underscores only. Example: ON_BOOKING.',
    1
),
(
    'PROJECT_METER_PAYMENT_MILESTONE',
    'milestoneLabel',
    'Milestone Label',
    'Human-readable name of this payment milestone.',
    'Shown to buyers in the payment schedule section of the project detail.',
    'Makes the payment trigger clear and understandable to buyers.',
    'Use the builder payment schedule or demand letter headings.',
    'On Foundation Completion',
    'Keep it short and descriptive.',
    2
),
(
    'PROJECT_METER_PAYMENT_MILESTONE',
    'percentageValue',
    'Percentage %',
    'Percentage of total property price due at this milestone.',
    'The portion of the total payment that must be paid when this construction milestone is reached.',
    'Powers the payment schedule shown to buyers and helps them plan their finances.',
    'Collect from the builder payment plan, demand letter, or sale agreement.',
    '15',
    'Enter value between 0 and 100. All milestone percentages should total 100.',
    3
),
(
    'PROJECT_METER_PAYMENT_MILESTONE',
    'displayOrder',
    'Display Order',
    'Controls the sequence of this milestone in the payment schedule.',
    'Lower numbers appear first. Use chronological payment sequence.',
    'Ensures buyers see milestones in the correct payment order.',
    'Use chronological order from booking to handover.',
    '2',
    'Enter a non-negative number.',
    4
),
(
    'PROJECT_METER_PAYMENT_MILESTONE',
    'linkedStageCode',
    'Linked Stage Code',
    'Links this payment to a specific construction stage.',
    'When set, the payment milestone is visually connected to the corresponding construction stage in the app.',
    'Helps buyers understand which construction event triggers each payment.',
    'Use the stageCode value from the Construction Stages section (e.g. FOUNDATION, STRUCTURE).',
    'FOUNDATION',
    'Optional. Must match an existing stage code exactly.',
    5
),
(
    'PROJECT_METER_PAYMENT_MILESTONE',
    'description',
    'Description',
    'Detailed explanation of what triggers this payment.',
    'Provides additional context about the payment milestone conditions, grace periods, or legal terms.',
    'Helps buyers and reviewers understand the exact payment trigger conditions.',
    'Refer to the sale agreement, builder payment schedule, or demand letter terms.',
    'Payment due within 30 days of foundation completion certificate issued.',
    'Optional. Keep it factual and clear.',
    6
),
(
    'PROJECT_METER_PAYMENT_MILESTONE',
    'active',
    'Active',
    'Whether this milestone is currently shown in the payment schedule.',
    'Inactive milestones are hidden from app users but retained for historical reference.',
    'Allows hiding cancelled or superseded milestones without deleting them.',
    'Set to active for all current payment milestones. Deactivate outdated ones.',
    'true',
    'Deactivate rather than delete milestones that are no longer relevant.',
    7
);

-- PROJECT_METER_COST_BREAKDOWN
INSERT INTO dashboard_field_help
    (module, field_key, field_label, short_help, detailed_help, why_needed, source_hint, example_value, validation_hint, display_order)
VALUES
(
    'PROJECT_METER_COST_BREAKDOWN',
    'landCost',
    'Land Cost',
    'Cost of acquiring the land for this project (INR).',
    'The total amount paid or estimated for land acquisition, including any legal or registration costs.',
    'Powers the cost breakdown chart showing how the total project cost is distributed.',
    'Collect from builder prospectus, annual report, or project cost disclosure.',
    '500000000',
    'Enter amount in INR. Use full numbers without commas.',
    1
),
(
    'PROJECT_METER_COST_BREAKDOWN',
    'constructionCost',
    'Construction Cost',
    'Cost of building construction (INR).',
    'Total cost of civil construction including structure, finishing, MEP, and contractor fees.',
    'Largest component of project cost — essential for the cost breakdown chart.',
    'Collect from builder cost estimate, contractor agreement, or project prospectus.',
    '1200000000',
    'Enter amount in INR.',
    2
),
(
    'PROJECT_METER_COST_BREAKDOWN',
    'infrastructureCost',
    'Infrastructure Cost',
    'Cost of developing roads, utilities, and common infrastructure (INR).',
    'Includes internal roads, drainage, water supply, electrical infrastructure, landscaping, and clubhouse construction.',
    'Helps buyers understand the investment in common infrastructure.',
    'Collect from builder cost estimate or project prospectus infrastructure section.',
    '200000000',
    'Enter amount in INR.',
    3
),
(
    'PROJECT_METER_COST_BREAKDOWN',
    'otherCost',
    'Other Cost',
    'All other project costs not covered above (INR).',
    'Includes marketing, legal fees, approvals, consultancy, and miscellaneous project costs.',
    'Ensures the total cost breakdown is complete.',
    'Collect from builder cost disclosure or project financial summary.',
    '100000000',
    'Enter amount in INR.',
    4
),
(
    'PROJECT_METER_COST_BREAKDOWN',
    'totalCost',
    'Total Cost (manual override)',
    'Total project cost if different from the calculated sum (INR).',
    'Leave blank to use the auto-calculated sum of the four cost fields above. Enter a value only if the builder-disclosed total differs.',
    'Allows capturing the builder-stated total when it does not match the sum of individual components.',
    'Use the total project cost figure from builder prospectus, annual report, or official disclosure.',
    '2000000000',
    'Optional. Leave blank to use the calculated total.',
    5
),
(
    'PROJECT_METER_COST_BREAKDOWN',
    'sourceLabel',
    'Source Label',
    'Where this cost data comes from.',
    'A short label identifying the data source, such as a specific report, prospectus edition, or financial year.',
    'Helps reviewers assess data reliability and track when the data was last updated.',
    'Use the report name, prospectus edition, or data collection date.',
    'Builder Prospectus Q1 2024',
    'Keep it short and identifiable.',
    6
),
(
    'PROJECT_METER_COST_BREAKDOWN',
    'remarks',
    'Remarks',
    'Optional notes about this cost breakdown.',
    'Internal notes visible to admins and reviewers only.',
    'Provides context about data quality, assumptions, or pending verification.',
    'Add notes about data limitations, estimation methodology, or pending updates.',
    'Cost estimates from builder; actual registration cost pending.',
    'Keep brief and factual.',
    7
),
(
    'PROJECT_METER_COST_BREAKDOWN',
    'verified',
    'Verified',
    'Marks whether this cost breakdown has been verified by a reviewer.',
    'Verified means the cost figures have been cross-checked against an official source document.',
    'Improves project trust score and data quality.',
    'Reviewer should verify against builder prospectus, annual report, or RERA cost disclosure.',
    'true',
    'Only mark verified when figures have been checked against a source document.',
    8
);

-- PROJECT_METER_LAND_UTILIZATION
INSERT INTO dashboard_field_help
    (module, field_key, field_label, short_help, detailed_help, why_needed, source_hint, example_value, validation_hint, display_order)
VALUES
(
    'PROJECT_METER_LAND_UTILIZATION',
    'areaUnit',
    'Area Unit',
    'Unit of measurement used for all area fields.',
    'All area values in this form use the same unit. Select before filling any area values.',
    'Ensures consistent interpretation of all area figures.',
    'Use the unit from the approved layout plan or RERA filing.',
    'SQ_MT',
    'Select the unit first, then fill in areas.',
    1
),
(
    'PROJECT_METER_LAND_UTILIZATION',
    'totalLandArea',
    'Total Land Area',
    'Total land area of the project site.',
    'The gross land area as per approved layout, RERA filing, or land records.',
    'The reference total against which all allocated areas are checked.',
    'Collect from RERA certificate, layout plan, or land records.',
    '40000',
    'Enter in the selected area unit.',
    2
),
(
    'PROJECT_METER_LAND_UTILIZATION',
    'builtUpArea',
    'Built-up Area',
    'Area covered by residential and commercial buildings.',
    'Total footprint of all towers and commercial structures on the project site.',
    'Shows buyers how much land is used for actual construction.',
    'Collect from approved layout plan or RERA filing.',
    '18000',
    'Enter in the selected area unit. Must not exceed total land area.',
    3
),
(
    'PROJECT_METER_LAND_UTILIZATION',
    'openArea',
    'Open / Green Area',
    'Open landscaped and green spaces on the project site.',
    'Includes parks, gardens, walking tracks, and other soft landscaped areas.',
    'Shows buyers how much open and green space is available in the project.',
    'Collect from landscape plan or approved layout.',
    '8000',
    'Enter in the selected area unit.',
    4
),
(
    'PROJECT_METER_LAND_UTILIZATION',
    'parkingArea',
    'Parking Area',
    'Area allocated for vehicle parking.',
    'Includes open surface parking and basement/podium parking footprint.',
    'Helps buyers understand available parking coverage.',
    'Collect from parking layout or approved building plan.',
    '4000',
    'Enter in the selected area unit.',
    5
),
(
    'PROJECT_METER_LAND_UTILIZATION',
    'amenitiesArea',
    'Amenities Area',
    'Area covered by amenity structures.',
    'Includes clubhouse, sports facilities, gym, swimming pool, and other amenity buildings.',
    'Shows buyers how much space is dedicated to amenities.',
    'Collect from approved layout or amenity plan.',
    '3000',
    'Enter in the selected area unit.',
    6
),
(
    'PROJECT_METER_LAND_UTILIZATION',
    'otherArea',
    'Other Area',
    'Any remaining area not accounted for in the above categories.',
    'Includes utility zones, service corridors, entry/exit driveways, and miscellaneous areas.',
    'Ensures the total land area is fully accounted for.',
    'Calculate as: Total Land - (Built-up + Open + Parking + Amenities).',
    '2000',
    'Enter in the selected area unit.',
    7
),
(
    'PROJECT_METER_LAND_UTILIZATION',
    'sourceLabel',
    'Source Label',
    'Where this land utilization data comes from.',
    'Identifies the plan or document these figures are based on.',
    'Helps reviewers assess data reliability.',
    'Use the approved layout plan reference, RERA filing, or site survey report.',
    'Approved Layout Plan - BBMP 2024',
    'Keep it short and identifiable.',
    8
),
(
    'PROJECT_METER_LAND_UTILIZATION',
    'remarks',
    'Remarks',
    'Optional internal notes about this land utilization data.',
    'Notes visible to admins and reviewers only.',
    'Provides context about data assumptions or collection notes.',
    'Add notes about estimation methodology or pending plan approval.',
    'Based on preliminary layout. Final RERA plan pending.',
    'Keep brief and factual.',
    9
),
(
    'PROJECT_METER_LAND_UTILIZATION',
    'verified',
    'Verified',
    'Marks whether this land utilization data has been verified.',
    'Verified means the area figures have been cross-checked against the approved layout plan.',
    'Improves project trust score.',
    'Reviewer should verify against the approved layout plan or RERA filing.',
    'true',
    'Only mark verified when figures have been checked against the layout plan.',
    10
);

-- PROJECT_METER_LOCATION_SCORE
INSERT INTO dashboard_field_help
    (module, field_key, field_label, short_help, detailed_help, why_needed, source_hint, example_value, validation_hint, display_order)
VALUES
(
    'PROJECT_METER_LOCATION_SCORE',
    'connectivityScore',
    'Connectivity Score',
    'Quality of transport connections from this location.',
    'Covers proximity and access to metro stations, bus stops, highways, and the airport.',
    'Helps buyers assess how easy it is to commute from this project.',
    'Use Google Maps travel time to nearest metro, highway, and airport. Score on 0-10 scale.',
    '8.5',
    'Enter value between 0.0 and 10.0.',
    1
),
(
    'PROJECT_METER_LOCATION_SCORE',
    'infrastructureScore',
    'Infrastructure Score',
    'Quality of roads, utilities, and civic infrastructure in this area.',
    'Covers road quality, power supply reliability, water supply, sewerage, and broadband availability.',
    'Helps buyers understand the quality of basic infrastructure in the location.',
    'Use site visit assessment, area development reports, or municipal records.',
    '7.5',
    'Enter value between 0.0 and 10.0.',
    2
),
(
    'PROJECT_METER_LOCATION_SCORE',
    'socialInfraScore',
    'Social Infrastructure Score',
    'Quality and proximity of schools, hospitals, and daily services.',
    'Covers access to reputed schools, colleges, hospitals, clinics, supermarkets, and restaurants.',
    'Key factor for family buyers evaluating a location.',
    'Map nearby schools, hospitals, and markets using Google Maps or field survey.',
    '7.0',
    'Enter value between 0.0 and 10.0.',
    3
),
(
    'PROJECT_METER_LOCATION_SCORE',
    'appreciationScore',
    'Appreciation Score',
    'Expected price appreciation potential of this location.',
    'Based on planned infrastructure, government investments, new employment hubs, and historical price growth.',
    'Helps investors evaluate the capital appreciation potential of the project.',
    'Use area development reports, government master plans, and historical price data from property portals.',
    '8.0',
    'Enter value between 0.0 and 10.0.',
    4
),
(
    'PROJECT_METER_LOCATION_SCORE',
    'safetyScore',
    'Safety Score',
    'Safety and security rating of the neighbourhood.',
    'Based on crime rate, street lighting, presence of CCTV, police station proximity, and gated community access.',
    'Important factor for buyers with families or senior residents.',
    'Use local crime data, police station proximity, and site visit assessment.',
    '8.0',
    'Enter value between 0.0 and 10.0.',
    5
),
(
    'PROJECT_METER_LOCATION_SCORE',
    'greeneryScore',
    'Greenery Score',
    'Amount of parks, lakes, and green spaces near the project.',
    'Covers proximity to public parks, lakes, forest reserves, and green corridors.',
    'Increasingly important to health-conscious buyers and families.',
    'Use Google Maps satellite view, area master plan, or site visit.',
    '6.5',
    'Enter value between 0.0 and 10.0.',
    6
),
(
    'PROJECT_METER_LOCATION_SCORE',
    'overallScore',
    'Overall Score (manual override)',
    'Final composite location score for this project.',
    'Leave blank to use the auto-calculated average of all category scores. Override only when you have a validated composite score from a third-party assessment.',
    'Shown as the headline location score in the Project Meter.',
    'Use the auto-calculated average unless you have a specific assessed composite score.',
    '7.8',
    'Optional. Leave blank to use calculated average. Enter between 0.0 and 10.0.',
    7
),
(
    'PROJECT_METER_LOCATION_SCORE',
    'sourceLabel',
    'Source Label',
    'Where this location assessment data comes from.',
    'Identifies the assessment report, data source, or methodology used.',
    'Helps reviewers assess reliability of the location scores.',
    'Use the assessment report name, team, and quarter/year.',
    'Internal assessment Q2 2024',
    'Keep it short and identifiable.',
    8
),
(
    'PROJECT_METER_LOCATION_SCORE',
    'remarks',
    'Remarks',
    'Optional notes about this location score.',
    'Internal notes about the scoring methodology, data sources, or limitations.',
    'Helps future reviewers understand how the scores were derived.',
    'Describe any scoring assumptions, limitations, or pending updates.',
    'Connectivity score will improve when metro line 4 opens in 2026.',
    'Keep brief and factual.',
    9
),
(
    'PROJECT_METER_LOCATION_SCORE',
    'verified',
    'Verified',
    'Marks whether this location score has been verified by a reviewer.',
    'Verified means the scores have been cross-checked against the stated source.',
    'Improves project trust score.',
    'Reviewer should verify category scores against the source assessment or field data.',
    'true',
    'Only mark verified when the scoring has been reviewed and confirmed.',
    10
);
