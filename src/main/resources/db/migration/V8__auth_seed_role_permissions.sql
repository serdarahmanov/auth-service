-- ARTIST permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.name IN (
                                          'TRACK_CREATE',
                                          'TRACK_EDIT_OWN',
                                          'TRACK_DELETE_OWN',
                                          'ALBUM_CREATE',
                                          'ALBUM_EDIT_OWN',
                                          'ALBUM_DELETE_OWN'
    )
WHERE r.name = 'ROLE_ARTIST';


-- EDITOR permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.name IN (
                                          'TRACK_EDIT_ALL',
                                          'TRACK_DELETE_ALL',
                                          'ALBUM_EDIT_ALL',
                                          'ALBUM_DELETE_ALL'
    )
WHERE r.name = 'ROLE_EDITOR';

-- ADMIN permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.name IN (
                                          'TRACK_EDIT_ALL',
                                          'TRACK_DELETE_ALL',
                                          'ALBUM_EDIT_ALL',
                                          'ALBUM_DELETE_ALL',
                                          'USER_ASSIGN_ROLES',
                                          'USER_REVOKE_ROLES'
    )
WHERE r.name = 'ROLE_ADMIN';

