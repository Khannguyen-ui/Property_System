CREATE INDEX idx_comment_property_created
ON property_comments(property_id, status, created_at DESC);

CREATE INDEX idx_comment_parent_created
ON property_comments(parent_id, status, created_at ASC);

CREATE INDEX idx_comment_user
ON property_comments(user_id);