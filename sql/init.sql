-- =============================================================
-- init.sql – Wetterdienst Pro
-- Datenbankschema für MariaDB
-- =============================================================

CREATE DATABASE IF NOT EXISTS dbdemo;
USE dbdemo;

-- Benutzertabelle für Registrierung
CREATE TABLE IF NOT EXISTS users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(150) NOT NULL UNIQUE,
    passwort      VARCHAR(512) NOT NULL,  -- Format: salt:hash (PBKDF2)
    benutzername  VARCHAR(100) NOT NULL UNIQUE,
    adresse       VARCHAR(200) NOT NULL,
    postleitzahl  VARCHAR(10)  NOT NULL,
    stadt         VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
