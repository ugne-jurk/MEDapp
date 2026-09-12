
-- psql -h pgsql3.mif -d studentu


SET search_path TO ugju1106, public;





CREATE TABLE Gydytojas (
    Spaudo_nr VARCHAR(8) PRIMARY KEY,
    Vardas VARCHAR(50) NOT NULL,
    Pavarde VARCHAR(50) NOT NULL,
    Specializacija VARCHAR(100) DEFAULT 'nenurodyta'
);

CREATE TABLE Pacientas (
    ID SERIAL PRIMARY KEY,
    AK NUMERIC(11) NOT NULL,
    Vardas VARCHAR(50) NOT NULL,
    Pavarde VARCHAR(50) NOT NULL,
    Gimimo_data DATE NOT NULL,
    El_pastas VARCHAR(100),
    Tel_nr VARCHAR(20) DEFAULT 'nenurodytas',
    CONSTRAINT check_email CHECK (El_pastas LIKE '%@%'),
    CONSTRAINT check_amzius CHECK (DATE_PART('year', AGE(Gimimo_data)) BETWEEN 0 AND 120)
);

CREATE TABLE Diagnoze (
    TLK VARCHAR(10) PRIMARY KEY,
    Pavadinimas VARCHAR(200) NOT NULL
);

CREATE TABLE Vizitas (
    Vizito_ID INT PRIMARY KEY,
    Vizito_data DATE NOT NULL,
    Gydytojo_Spaudas VARCHAR(10) NOT NULL REFERENCES Gydytojas (Spaudo_nr),
    Paciento_ID INT NOT NULL REFERENCES Pacientas (ID),
    CONSTRAINT check_vizito_data CHECK (Vizito_data <= '2030-01-01')
);

CREATE TABLE Vizito_diagnoze (
    Vizito_ID INT NOT NULL,
    TLK VARCHAR(10) NOT NULL,
    PRIMARY KEY (Vizito_ID, TLK),
    Vizito_ID REFERENCES Vizitas (Vizito_ID),
    TLK REFERENCES Diagnoze (TLK)
);


CREATE UNIQUE INDEX idx_pacientas_ak ON Pacientas(AK);

CREATE UNIQUE INDEX idx_unique_gydytojas_vizitas 
ON Vizitas (Gydytojo_Spaudas, Vizito_data);

CREATE UNIQUE INDEX idx_unique_pacientas_vizitas 
ON Vizitas (Paciento_ID, Vizito_data);

CREATE INDEX idx_pacientas_vardas_pavarde_gimimo_data
ON Pacientas (Vardas, Pavarde, Gimimo_data);


CREATE VIEW Visi_vizitai AS
SELECT v.Vizito_ID, v.Vizito_data, p.Vardas, p.Pavarde
FROM Vizitas v
JOIN Pacientas p ON v.Paciento_ID = p.ID;


CREATE MATERIALIZED VIEW Statistika AS
SELECT Paciento_ID, COUNT(*) AS vizitu_skaicius
FROM Vizitas
GROUP BY Paciento_ID;

REFRESH MATERIALIZED VIEW Statistika;

CREATE FUNCTION trg_check_gydytojas_max_vizitu()
RETURNS TRIGGER AS $$
DECLARE
    vizitu_sk INT;
BEGIN
    SELECT COUNT(*)
    INTO vizitu_sk
    FROM Vizitas
    WHERE Gydytojo_Spaudas = NEW.Gydytojo_Spaudas
      AND Vizito_data = NEW.Vizito_data;

    IF vizitu_sk >= 5 THEN
        RAISE EXCEPTION 'Gydytojas jau turi 5 vizitus siai dienai' ;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER Gydytojas_Max_5
BEFORE INSERT ON Vizitas
FOR EACH ROW
EXECUTE FUNCTION trg_check_gydytojas_max_vizitu();


CREATE OR REPLACE FUNCTION trg_no_delete_24h()
RETURNS trigger AS $$
BEGIN
    -- Tikriname ar vizitas yra ateityje IR ar liko mažiau nei 24 val
    IF OLD.Vizito_data > NOW() 
       AND (OLD.Vizito_data - NOW()) < INTERVAL '24 hours' THEN
        RAISE EXCEPTION
            'Negalima trinti vizito: iki vizito liko mažiau nei 24 valandos. Vizito data: %, Dabar: %',
            OLD.Vizito_data, NOW();
    END IF;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;



CREATE TRIGGER No_Delete_24_Hours
BEFORE DELETE ON Vizitas
FOR EACH ROW
EXECUTE FUNCTION trg_no_delete_24h();








INSERT INTO Gydytojas (Spaudo_nr, Vardas, Pavarde, Specializacija) VALUES
('G001', 'Jonas', 'Kazlauskas', 'Kardiologas'),
('G002', 'Agnė', 'Petraitienė', 'Neurologė'),
('G003', 'Tomas', 'Jankauskas', 'nenurodyta');

INSERT INTO Pacientas (AK, Vardas, Pavarde, Gimimo_data, El_pastas, Tel_nr) VALUES
('50001011234', 'Mantas', 'Barauskas', '1990-05-10', 'mantas@gmail.com', '860000001'),
('50002021234', 'Laura', 'Ciutaite', '1985-08-14', 'laura@mail.lt', '860000002'),
('50003031234', 'Rokas', 'Lapinskas', '2000-03-20', NULL, 'nenurodytas'),
('50004041234', 'Greta', 'Matulevičienė', '1975-11-30', 'greta@inbox.lt', '860000004');

INSERT INTO Diagnoze (TLK, Pavadinimas) VALUES
('I10', 'Esminė hipertenzija'),
('G44', 'Migrena'),
('J20', 'Ūminis bronchitas'),
('E11', 'Cukrinis diabetas, 2 tipo'),
('K21', 'Gastroezofaginio refliukso liga');

INSERT INTO Vizitas (Vizito_ID, Vizito_data, Gydytojo_Spaudas, Paciento_ID) 
VALUES 
    (1, '2024-01-10', 'G001', 1),
    (2, '2024-01-15', 'G002', 2),
    (3, '2024-01-20', 'G001', 3);

INSERT INTO Vizito_diagnoze (Vizito_ID, TLK) VALUES
(1, 'I10'),
(1, 'E11'),

(2, 'G44'),

(3, 'J20');

DROP TRIGGER IF EXISTS Gydytojas_Vienu_Metu ON Vizitas;
DROP FUNCTION IF EXISTS trg_check_gydytojas_vizitas();

DROP TRIGGER IF EXISTS Pacientas_Vienu_Metu ON Vizitas;
DROP FUNCTION IF EXISTS trg_check_pacientas_vizitas();

DROP INDEX idx_unique_gydytojas_vizitas;
