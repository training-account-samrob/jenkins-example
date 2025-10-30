CREATE OR REPLACE FUNCTION trigger_set_last_update()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_update = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- DDL for TABLE Person

CREATE TABLE Person(
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    date_of_birth DATE NOT NULL,
    address_line_1 VARCHAR(75) NOT NULL,
    postcode VARCHAR(10) NOT NULL,
    email VARCHAR(255) NOT NULL,
    last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER update_last_update
BEFORE UPDATE ON Person
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_last_update();


-- DDL for TABLE Visa

CREATE TYPE visa_types_enum AS ENUM ('Visit', 'Study', 'Work', 'Family', 'Business', 'Transit');

CREATE TABLE Visa(
    id VARCHAR(16) PRIMARY KEY,
    person_id BIGINT NOT NULL REFERENCES Person (id),
    country VARCHAR(256) NOT NULL,
    visa_type visa_types_enum NOT NULL,
    visa_start_date DATE NOT NULL,
    visa_expiry_date DATE NOT NULL,
    last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER update_last_update
BEFORE UPDATE ON Visa
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_last_update();

-- DDL for TABLE Travel History

CREATE TABLE Travel_History(
    id BIGSERIAL PRIMARY KEY,
    person_id BIGINT NOT NULL REFERENCES Person (id) ON DELETE CASCADE,
    country VARCHAR(256) NOT NULL,
    entry_date DATE NOT NULL,
    exit_date DATE,
    last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER update_last_update
BEFORE UPDATE ON Travel_History
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_last_update();