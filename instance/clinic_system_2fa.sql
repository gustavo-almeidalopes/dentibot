CREATE DATABASE clinic_system_2fa
USE clinic_system_2fa

CREATE TABLE user (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(200) NOT NULL,
    email VARCHAR(120) UNIQUE NOT NULL,
    phone VARCHAR(20),
    two_factor_method VARCHAR(10) DEFAULT 'email',
    totp_secret VARCHAR(32),
    auth_code VARCHAR(6),
    code_expiration DATETIME
);
