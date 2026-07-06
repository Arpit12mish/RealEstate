-- Extends brand_collaboration (created in V118) with the richer display
-- fields the Phase 1B admin API needs: how the brand relates to the target
-- (relation_type, source_type), what it's showcasing (usage_category, title,
-- description), and moderation/curation flags (verified, public_visible,
-- featured). Purely additive - the existing target_type/FK columns and their
-- CHECK/unique constraints from V118 are untouched.
--
-- relation_type and source_type are free-text tags (like brand_media's
-- existing action_type column), not a closed enum, because the full set of
-- valid values wasn't specified yet - see the Phase 1B report follow-ups.

ALTER TABLE brand_collaboration
  ADD COLUMN IF NOT EXISTS relation_type VARCHAR(50),
  ADD COLUMN IF NOT EXISTS source_type VARCHAR(50),
  ADD COLUMN IF NOT EXISTS usage_category VARCHAR(150),
  ADD COLUMN IF NOT EXISTS title VARCHAR(255),
  ADD COLUMN IF NOT EXISTS description TEXT,
  ADD COLUMN IF NOT EXISTS verified BOOLEAN NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS public_visible BOOLEAN NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS featured BOOLEAN NOT NULL DEFAULT false;
