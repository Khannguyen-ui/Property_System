-- V13: Performance indexes for hot backend queries in property-service
-- Keep this migration additive and safe for online deploys.

-- =========================
-- PROPERTY
-- =========================

-- Public listing / expiring properties
CREATE INDEX IF NOT EXISTS idx_properties_status_expires_created
    ON properties (status, expires_at, created_at DESC);

-- Owner dashboard / my properties
CREATE INDEX IF NOT EXISTS idx_properties_owner_status_created
    ON properties (owner_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_properties_owner_status_property_type_created
    ON properties (owner_id, status, property_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_properties_owner_status_transaction_type_created
    ON properties (owner_id, status, transaction_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_properties_owner_status_expires_created
    ON properties (owner_id, status, expires_at, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_properties_owner_status_transaction_expires_created
    ON properties (owner_id, status, transaction_type, expires_at, created_at DESC);

-- Project feed / project detail listing
CREATE INDEX IF NOT EXISTS idx_properties_project_status_expires_created
    ON properties (project_id, status, expires_at, created_at DESC);

-- Promoted / reels / trending feeds
CREATE INDEX IF NOT EXISTS idx_properties_status_promoted_created_id
    ON properties (status, is_promoted, created_at DESC, id DESC);

-- Trash / deleted properties by owner
CREATE INDEX IF NOT EXISTS idx_properties_deleted_owner_created
    ON properties (owner_id, created_at DESC)
    WHERE status = 'DELETED';

-- =========================
-- PROJECT
-- =========================

CREATE INDEX IF NOT EXISTS idx_projects_status_created
    ON projects (status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_projects_deleted_created
    ON projects (created_at DESC)
    WHERE status = 'DELETED';

-- =========================
-- AMENITIES
-- =========================

-- Supports LOWER(name) = LOWER(:name)
CREATE INDEX IF NOT EXISTS idx_amenities_name_lower
    ON amenities (LOWER(name));

-- =========================
-- APPOINTMENTS
-- =========================

CREATE INDEX IF NOT EXISTS idx_appointments_user_time
    ON appointments (user_id, appointment_time ASC);

CREATE INDEX IF NOT EXISTS idx_appointments_owner_time
    ON appointments (owner_id, appointment_time ASC);

-- =========================
-- PROPERTY CONTACTS
-- =========================

CREATE INDEX IF NOT EXISTS idx_property_contacts_user_property
    ON property_contacts (user_id, property_id);

CREATE INDEX IF NOT EXISTS idx_property_contacts_user_owner
    ON property_contacts (user_id, owner_id);

-- =========================
-- OWNER FOLLOWS
-- =========================

CREATE INDEX IF NOT EXISTS idx_owner_follows_owner
    ON owner_follows (owner_id);

CREATE INDEX IF NOT EXISTS idx_owner_follows_follower
    ON owner_follows (follower_id);

CREATE INDEX IF NOT EXISTS idx_owner_follows_follower_owner
    ON owner_follows (follower_id, owner_id);

-- =========================
-- OWNER REVIEWS
-- =========================

CREATE INDEX IF NOT EXISTS idx_owner_reviews_owner_created
    ON owner_reviews (owner_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_owner_reviews_owner_reviewer_property
    ON owner_reviews (owner_id, reviewer_id, property_id);

CREATE INDEX IF NOT EXISTS idx_owner_reviews_owner_rating
    ON owner_reviews (owner_id, rating);

CREATE INDEX IF NOT EXISTS idx_owner_reviews_owner_verified
    ON owner_reviews (owner_id, verified);

-- =========================
-- PROMOTION QUEUE
-- =========================

CREATE INDEX IF NOT EXISTS idx_promotion_queue_property_status_priority_created
    ON promotion_queue (property_id, status, priority_level DESC, created_at ASC);

CREATE INDEX IF NOT EXISTS idx_promotion_queue_user_status_priority
    ON promotion_queue (user_id, status, priority_level DESC);

-- =========================
-- OUTBOX EVENTS
-- =========================

-- Relay task scans pending rows repeatedly
CREATE INDEX IF NOT EXISTS idx_outbox_events_pending_created
    ON outbox_events (created_at ASC)
    WHERE status = 'PENDING';

-- =========================
-- USER PROPERTY INTERACTIONS
-- =========================

CREATE INDEX IF NOT EXISTS idx_property_interactions_property_type_user
    ON user_property_interactions (property_id, interaction_type, user_id);

CREATE INDEX IF NOT EXISTS idx_property_interactions_property_type_guest
    ON user_property_interactions (property_id, interaction_type, guest_id);

CREATE INDEX IF NOT EXISTS idx_property_interactions_user_type_property
    ON user_property_interactions (user_id, interaction_type, property_id);

CREATE INDEX IF NOT EXISTS idx_property_interactions_guest_type_property
    ON user_property_interactions (guest_id, interaction_type, property_id);
