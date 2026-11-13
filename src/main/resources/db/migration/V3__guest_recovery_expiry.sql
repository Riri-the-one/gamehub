-- Historical guests are not assigned a recovery capability or an expiry retroactively.
ALTER TABLE utilisateurs ADD guest_token_expires_at DATETIME(6);
