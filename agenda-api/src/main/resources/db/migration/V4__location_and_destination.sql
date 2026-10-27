ALTER TABLE appointments ADD COLUMN destination_latitude DOUBLE PRECISION;
ALTER TABLE appointments ADD COLUMN destination_longitude DOUBLE PRECISION;
ALTER TABLE appointments ADD CONSTRAINT appointment_destination_coordinates_pair
    CHECK ((destination_latitude IS NULL AND destination_longitude IS NULL)
        OR (destination_latitude BETWEEN -90 AND 90 AND destination_longitude BETWEEN -180 AND 180));

CREATE TABLE user_location_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    fallback_label VARCHAR(240),
    fallback_latitude DOUBLE PRECISION,
    fallback_longitude DOUBLE PRECISION,
    gps_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    last_gps_latitude DOUBLE PRECISION,
    last_gps_longitude DOUBLE PRECISION,
    last_gps_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fallback_coordinates_pair CHECK (
        (fallback_latitude IS NULL AND fallback_longitude IS NULL)
        OR (fallback_latitude BETWEEN -90 AND 90 AND fallback_longitude BETWEEN -180 AND 180)
    ),
    CONSTRAINT gps_coordinates_pair CHECK (
        (last_gps_latitude IS NULL AND last_gps_longitude IS NULL AND last_gps_at IS NULL)
        OR (last_gps_latitude BETWEEN -90 AND 90 AND last_gps_longitude BETWEEN -180 AND 180 AND last_gps_at IS NOT NULL)
    )
);
