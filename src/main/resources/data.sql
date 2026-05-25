-- ══════════════════════════════════════════════════════════════
--  CivicCMS — Seed Data (runs on every startup via spring.sql.init)
--  Uses INSERT IGNORE so existing rows are never overwritten
-- ══════════════════════════════════════════════════════════════

-- ── Project departments (only these 6 appear in the Raise Complaint form) ──
INSERT IGNORE INTO departments (name, code, keywords_csv, head_email) VALUES
  ('Roads',
   'ROAD',
   'road,pothole,street,pavement,footpath,traffic,signal,speed,bump,accident,crack,construction',
   'roads@civic.gov.in'),

  ('Sanitation',
   'GARBAGE',
   'garbage,waste,trash,litter,dump,bin,clean,hygiene,smell,stink,overflowing,sweeping,drain,flood,waterlog,overflow,stagnant,gutter,block,clog,canal,storm',
   'sanitation@civic.gov.in'),

  ('Electricity',
   'ELECTRICITY',
   'power,electric,electricity,light,voltage,wire,transformer,outage,current,meter,shock,fault,streetlight,lamp,dark,bulb,lighting,pole,night,visibility',
   'electricity@civic.gov.in'),

  ('Water',
   'WATER',
   'water,pipe,leak,burst,supply,tap,drinking,sewage,sewer,contamination,overflow',
   'water@civic.gov.in'),

  ('Public Safety',
   'ANIMAL',
   'dog,stray,animal,cattle,mosquito,pest,rat,snake,bird,bite,attack,noise,sound,loud,music,horn,encroach,illegal,unauthorized,building,occupy,permit',
   'safety@civic.gov.in'),

  ('General',
   'OTHER',
   'general,other,misc,complaint,request,suggest,feedback,park,garden,tree,plant,bench,playground,grass',
   'general@civic.gov.in');

-- NOTE: Admin user is seeded automatically by DataInitializer.java on first startup.

-- Schema migrations for ratings table are handled by DataInitializer.java
-- (see migrateRatingsTable method) to avoid MySQL DELIMITER issues.

-- ── CMS Site Content — default editable content ──────────────────
-- Uses INSERT IGNORE so manual edits in the admin are never overwritten on restart.

INSERT IGNORE INTO site_content (page, content_key, content_type, value, label) VALUES
  -- Global (shared across all pages)
  ('global', 'logo.icon',           'TEXT',  '🏛️',                                         'Logo Icon (emoji)'),
  ('global', 'navbar.brand.name',   'TEXT',  'Civic',                                        'Brand Name (accent part)'),
  ('global', 'navbar.brand.suffix', 'TEXT',  'CMS',                                          'Brand Suffix'),
  ('global', 'footer.brand',        'TEXT',  'CivicCMS',                                     'Footer Brand Name'),
  ('global', 'footer.tagline',      'TEXT',  'AI-Powered Smart Civic Issue Detection and Resolution Platform', 'Footer Tagline'),

  -- Index / Home page
  ('index',  'hero.eyebrow',        'TEXT',  'AI-Powered Platform — Live',                   'Hero Eyebrow Label'),
  ('index',  'hero.headline',       'TEXT',  'Smart Civic Issue<br>Reporting &amp; Resolution','Hero Main Headline'),
  ('index',  'hero.description',    'TEXT',  'Report road damage, water leaks, electricity faults and more. Our AI instantly classifies, prioritises and routes your complaint to the right department.', 'Hero Description'),
  ('index',  'hero.image',          'IMAGE', '',                                              'Hero Background Image'),
  ('index',  'hero.feature1',       'TEXT',  'AI Auto-Classification',                        'Hero Feature Pill 1'),
  ('index',  'hero.feature2',       'TEXT',  'Real-Time Status',                              'Hero Feature Pill 2'),
  ('index',  'hero.feature3',       'TEXT',  'Duplicate Detection',                           'Hero Feature Pill 3'),
  ('index',  'hero.feature4',       'TEXT',  'Risk Scoring Engine',                           'Hero Feature Pill 4'),
  ('index',  'howitworks.title',    'TEXT',  'How It Works',                                  '"How It Works" Section Title'),
  ('index',  'howitworks.subtitle', 'TEXT',  'Three steps from report to resolution, powered by AI', '"How It Works" Subtitle'),
  ('index',  'step1.icon',          'TEXT',  '📨',                                            'Step 1 Icon'),
  ('index',  'step1.title',         'TEXT',  'Submit',                                        'Step 1 Title'),
  ('index',  'step1.description',   'TEXT',  'Describe the issue, pin your location on the interactive map, and optionally attach a photo.', 'Step 1 Description'),
  ('index',  'step2.icon',          'TEXT',  '🤖',                                            'Step 2 Icon'),
  ('index',  'step2.title',         'TEXT',  'AI Processes',                                  'Step 2 Title'),
  ('index',  'step2.description',   'TEXT',  'Our AI extracts keywords, detects duplicates, scores risk, and routes to the correct department.', 'Step 2 Description'),
  ('index',  'step3.icon',          'TEXT',  '✅',                                            'Step 3 Icon'),
  ('index',  'step3.title',         'TEXT',  'Get Resolved',                                  'Step 3 Title'),
  ('index',  'step3.description',   'TEXT',  'Track status in real time, receive email updates, and rate the resolution quality.', 'Step 3 Description'),

  -- Login page
  ('login',  'subtitle',            'TEXT',  'Sign in to your account',                       'Login Page Subtitle'),

  -- Submit page
  ('submit', 'page.title',          'TEXT',  '📨 Submit a Complaint',                         'Submit Page Title'),
  ('submit', 'page.subtitle',       'TEXT',  'Fill in the details below. Our AI will classify and route it automatically.', 'Submit Page Subtitle'),

  -- Track page
  ('track',  'page.title',          'TEXT',  '🔍 Track Your Complaint',                       'Track Page Title'),
  ('track',  'page.subtitle',       'TEXT',  'Enter your complaint ID to view real-time status and progress.', 'Track Page Subtitle');
