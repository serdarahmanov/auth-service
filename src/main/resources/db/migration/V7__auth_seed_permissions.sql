INSERT INTO permissions (name, description) VALUES

-- Track permissions
('TRACK_CREATE', 'Upload new tracks'),
('TRACK_EDIT_OWN', 'Edit own tracks'),
('TRACK_EDIT_ALL', 'Edit any track'),
('TRACK_DELETE_OWN', 'Delete own tracks'),
('TRACK_DELETE_ALL', 'Delete any track'),

-- Album permissions
('ALBUM_CREATE', 'Create albums'),
('ALBUM_EDIT_OWN', 'Edit own albums'),
('ALBUM_EDIT_ALL', 'Edit any album'),
('ALBUM_DELETE_OWN', 'Delete own albums'),
('ALBUM_DELETE_ALL', 'Delete any album'),

-- User & admin permissions
('USER_ASSIGN_ROLES', 'Assign roles to users'),
('USER_REVOKE_ROLES', 'Revoke roles from users');
