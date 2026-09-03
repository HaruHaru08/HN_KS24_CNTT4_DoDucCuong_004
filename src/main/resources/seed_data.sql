INSERT INTO app_users (user_id, full_name, department) VALUES
('USR-001', 'Nguyen Minh Anh', 'Operations'), ('USR-002', 'Tran Quoc Bao', 'Finance')
ON CONFLICT (user_id) DO UPDATE SET full_name = EXCLUDED.full_name;
INSERT INTO resource_types (resource_code, display_name, max_participants, active) VALUES
('STD', 'Standard thiết bị CNTT', 2, TRUE), ('PRM', 'Premium thiết bị CNTT', 4, TRUE)
ON CONFLICT (resource_code) DO UPDATE SET display_name = EXCLUDED.display_name;
INSERT INTO resource_inventory (resource_code, available_date, available_slots) VALUES
('STD', DATE '2026-09-15', 8), ('STD', DATE '2026-09-16', 8), ('PRM', DATE '2026-09-15', 3), ('PRM', DATE '2026-09-16', 2)
ON CONFLICT (resource_code, available_date) DO UPDATE SET available_slots = EXCLUDED.available_slots;
