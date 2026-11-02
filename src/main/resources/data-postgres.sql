INSERT INTO training_type (training_type_name) VALUES ('Fitness') ON CONFLICT (training_type_name) DO NOTHING;
INSERT INTO training_type (training_type_name) VALUES ('Yoga') ON CONFLICT (training_type_name) DO NOTHING;
INSERT INTO training_type (training_type_name) VALUES ('Zumba') ON CONFLICT (training_type_name) DO NOTHING;
INSERT INTO training_type (training_type_name) VALUES ('Stretching') ON CONFLICT (training_type_name) DO NOTHING;
INSERT INTO training_type (training_type_name) VALUES ('Resistance') ON CONFLICT (training_type_name) DO NOTHING;