-- =================================================================
-- SCRIPT DE CRIACAO DO BANCO DE DADOS DENTIBOT v2.0
-- =================================================================
-- Este script e destrutivo. Ele apagara o banco de dados existente.
-- Data da Criacao: 05 de Setembro de 2025
-- =================================================================

DROP DATABASE IF EXISTS dentibot_db;
CREATE DATABASE dentibot_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dentibot_db;

SET NAMES utf8mb4;


-- -----------------------------------------------------
-- Tabela: roles (Perfis de Acesso)
-- -----------------------------------------------------
CREATE TABLE roles (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL UNIQUE COMMENT 'Ex: Coordenador, Dentista, Recepcionista, Almoxarife, Paciente'
) ENGINE=InnoDB COMMENT='Perfis de acesso ao sistema.';


-- -----------------------------------------------------
-- Tabela: users (Usuários do Sistema)
-- -----------------------------------------------------
CREATE TABLE users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL COMMENT 'Armazenar hash (ex: bcrypt), nunca a senha pura',
  phone_number VARCHAR(20),
  role_id INT NOT NULL,
  is_active BOOLEAN DEFAULT TRUE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='Usuários que operam o sistema (equipe da clínica).';


-- -----------------------------------------------------
-- Tabela: patients (Pacientes)
-- -----------------------------------------------------
CREATE TABLE patients (
  id INT AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(255) NOT NULL,
  date_of_birth DATE NOT NULL,
  phone_number VARCHAR(20) NOT NULL,
  email VARCHAR(255) UNIQUE,
  address TEXT,
  special_notes TEXT COMMENT 'Observações como alergias, condições médicas, necessidades especiais.',
  avatar_path VARCHAR(255) COMMENT 'Caminho para a foto de perfil do paciente no servidor.',
  notification_preferences JSON COMMENT 'Ex: {"sms": true, "whatsapp": true, "email": false}',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_patient_name (full_name)
) ENGINE=InnoDB COMMENT='Cadastro central de pacientes.';


-- -----------------------------------------------------
-- Tabela: appointments (Agendamentos)
-- -----------------------------------------------------
CREATE TABLE appointments (
  id INT AUTO_INCREMENT PRIMARY KEY,
  patient_id INT NOT NULL,
  dentist_id INT NOT NULL,
  procedure_description VARCHAR(255) NOT NULL,
  appointment_date DATE NOT NULL,
  appointment_time TIME NOT NULL,
  status ENUM('Agendado', 'Confirmado', 'Cancelado', 'Realizado', 'Não Compareceu') NOT NULL DEFAULT 'Agendado',
  notes TEXT,
  checkin_time DATETIME NULL COMMENT 'Horário que o paciente fez check-in na clínica',
  cancellation_reason TEXT NULL COMMENT 'Motivo do cancelamento, se aplicável',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
  FOREIGN KEY (dentist_id) REFERENCES users(id) ON DELETE RESTRICT,
  INDEX idx_appointment_datetime (appointment_date, appointment_time)
) ENGINE=InnoDB COMMENT='Agenda de consultas da clínica.';


-- -----------------------------------------------------
-- Tabela: clinical_records (Prontuários)
-- -----------------------------------------------------
CREATE TABLE clinical_records (
  id INT AUTO_INCREMENT PRIMARY KEY,
  patient_id INT NOT NULL UNIQUE,
  anamnesis TEXT COMMENT 'Histórico médico geral, alergias, medicações.',
  odontogram_data JSON COMMENT 'Armazena os dados do odontograma em formato JSON.',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Prontuário eletrônico principal de cada paciente.';


-- -----------------------------------------------------
-- Tabela: record_entries (Evolução Clínica)
-- -----------------------------------------------------
CREATE TABLE record_entries (
  id INT AUTO_INCREMENT PRIMARY KEY,
  record_id INT NOT NULL,
  appointment_id INT UNIQUE,
  user_id INT NOT NULL COMMENT 'Profissional que fez o registro.',
  entry_text TEXT NOT NULL COMMENT 'Descrição detalhada do atendimento.',
  entry_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (record_id) REFERENCES clinical_records(id) ON DELETE CASCADE,
  FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='Registros individuais de cada consulta no prontuário.';


-- -----------------------------------------------------
-- Tabela: documents (Documentos - Imagens e PDFs)
-- -----------------------------------------------------
CREATE TABLE documents (
  id INT AUTO_INCREMENT PRIMARY KEY,
  patient_id INT NOT NULL,
  appointment_id INT NULL,
  document_type ENUM('Raio-X', 'Exame', 'Atestado', 'Contrato', 'Outro') NOT NULL,
  original_file_name VARCHAR(255) NOT NULL COMMENT 'Nome original do arquivo enviado.',
  stored_file_path VARCHAR(255) NOT NULL UNIQUE COMMENT 'Caminho SEGURO do arquivo no servidor.',
  mime_type VARCHAR(100) NOT NULL COMMENT 'Ex: image/jpeg, application/pdf.',
  file_size_bytes BIGINT NOT NULL,
  uploaded_by_user_id INT NULL,
  upload_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
  FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL,
  FOREIGN KEY (uploaded_by_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Gerencia metadados de arquivos (imagens, PDFs) armazenados no servidor.';


-- -----------------------------------------------------
-- Tabela: inventory_consumables (Estoque de Consumíveis)
-- -----------------------------------------------------
CREATE TABLE inventory_consumables (
  id INT AUTO_INCREMENT PRIMARY KEY,
  item_name VARCHAR(255) NOT NULL,
  description TEXT,
  supplier VARCHAR(255),
  quantity_in_stock INT NOT NULL DEFAULT 0,
  minimum_stock_level INT NOT NULL DEFAULT 1,
  unit_of_measure VARCHAR(50),
  last_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Inventário de materiais descartáveis.';


-- -----------------------------------------------------
-- Tabela: instrument_kits (Kits de Instrumentos)
-- -----------------------------------------------------
CREATE TABLE instrument_kits (
  id INT AUTO_INCREMENT PRIMARY KEY,
  kit_identifier VARCHAR(100) NOT NULL UNIQUE COMMENT 'Ex: KIT-CIR-001',
  description TEXT NOT NULL,
  status ENUM('Disponível', 'Em Uso', 'Em Esterilização', 'Bloqueado', 'Manutenção') NOT NULL DEFAULT 'Disponível',
  last_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Cadastro de kits de instrumentos reutilizáveis.';


-- -----------------------------------------------------
-- Tabela: sterilization_cycles (Ciclos de Esterilização)
-- -----------------------------------------------------
CREATE TABLE sterilization_cycles (
  id INT AUTO_INCREMENT PRIMARY KEY,
  kit_id INT NOT NULL,
  user_id INT NOT NULL COMMENT 'Operador do ciclo.',
  start_time DATETIME NOT NULL,
  end_time DATETIME,
  result ENUM('Sucesso', 'Falha', 'Em Processo') NOT NULL DEFAULT 'Em Processo',
  notes TEXT,
  FOREIGN KEY (kit_id) REFERENCES instrument_kits(id) ON DELETE RESTRICT,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='Histórico de rastreabilidade dos ciclos de autoclave.';


-- -----------------------------------------------------
-- Tabela: payments (Pagamentos)
-- -----------------------------------------------------
CREATE TABLE payments (
  id INT AUTO_INCREMENT PRIMARY KEY,
  appointment_id INT NOT NULL,
  patient_id INT NOT NULL,
  amount DECIMAL(10, 2) NOT NULL,
  payment_method ENUM('Cartão de Crédito', 'Cartão de Débito', 'PIX', 'Dinheiro', 'Convênio') NOT NULL,
  status ENUM('Pendente', 'Pago', 'Falhou', 'Reembolsado') NOT NULL DEFAULT 'Pendente',
  transaction_id VARCHAR(255) COMMENT 'ID retornado pelo gateway de pagamento',
  payment_date DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE RESTRICT,
  FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='Registros financeiros das consultas.';


-- -----------------------------------------------------
-- Tabela: insurances (Convênios)
-- -----------------------------------------------------
CREATE TABLE insurances (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE
) ENGINE=InnoDB COMMENT='Catálogo de convênios aceitos.';


-- -----------------------------------------------------
-- Tabela: patient_insurances (Convênios dos Pacientes)
-- -----------------------------------------------------
CREATE TABLE patient_insurances (
  id INT AUTO_INCREMENT PRIMARY KEY,
  patient_id INT NOT NULL,
  insurance_id INT NOT NULL,
  policy_number VARCHAR(100) NOT NULL,
  is_active BOOLEAN DEFAULT TRUE,
  FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
  FOREIGN KEY (insurance_id) REFERENCES insurances(id) ON DELETE RESTRICT,
  UNIQUE KEY uk_patient_insurance (patient_id, insurance_id)
) ENGINE=InnoDB COMMENT='Associa um paciente a um ou mais convênios.';


-- -----------------------------------------------------
-- Tabela: insurance_authorizations (Autorizações TUSS/TISS)
-- -----------------------------------------------------
CREATE TABLE insurance_authorizations (
  id INT AUTO_INCREMENT PRIMARY KEY,
  appointment_id INT NOT NULL,
  patient_insurance_id INT NOT NULL,
  procedure_code VARCHAR(100) NOT NULL COMMENT 'Código TUSS do procedimento',
  status ENUM('Solicitado', 'Aprovado', 'Glosado', 'Pendente') NOT NULL DEFAULT 'Pendente',
  request_date DATETIME DEFAULT CURRENT_TIMESTAMP,
  response_date DATETIME,
  authorization_code VARCHAR(100),
  notes TEXT,
  FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE RESTRICT,
  FOREIGN KEY (patient_insurance_id) REFERENCES patient_insurances(id) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='Rastreia as solicitações de autorização para convênios.';


-- =================================================================
-- DADOS INICIAIS (SEEDING)
-- =================================================================

-- Perfis
INSERT INTO `roles` (`name`) VALUES ('Coordenador'), ('Dentista'), ('Recepcionista'), ('Almoxarife');

-- Usuários
INSERT INTO `users` (`name`, `email`, `password_hash`, `role_id`) VALUES
('Admin DentiBot', 'admin@dentibot.com', '$2y$10$N9qo8uLOickgx2Z5P7w...exemplo', 1),
('Dr. João Silva', 'joao.silva@dentibot.com', '$2y$10$N9qo8uLOickgx2Z5P7w...exemplo', 2),
('Dra. Maria Souza', 'maria.souza@dentibot.com', '$2y$10$N9qo8uLOickgx2Z5P7w...exemplo', 2),
('Ana Paula', 'ana.paula@dentibot.com', '$2y$10$N9qo8uLOickgx2Z5P7w...exemplo', 3),
('Carlos Estoque', 'carlos.estoque@dentibot.com', '$2y$10$N9qo8uLOickgx2Z5P7w...exemplo', 4);

-- Pacientes
INSERT INTO `patients` (`full_name`, `date_of_birth`, `phone_number`, `email`, `address`, `special_notes`, `notification_preferences`) VALUES
('Mariana Costa', '1990-05-15', '(11) 98765-4321', 'mariana.costa@email.com', 'Rua das Flores, 123', 'Alergia a Penicilina.', '{"sms": true, "whatsapp": true, "email": true}'),
('Carlos Eduardo Santos', '1985-09-20', '(21) 91234-5678', 'carlos.santos@email.com', 'Av. Copacabana, 456', NULL, '{"whatsapp": true}');

-- Agendamentos
INSERT INTO `appointments` (`patient_id`, `dentist_id`, `procedure_description`, `appointment_date`, `appointment_time`, `status`) VALUES
(1, 3, 'Avaliação de Rotina', '2025-11-28', '11:30:00', 'Agendado'),
(2, 2, 'Profilaxia/Limpeza', '2025-09-08', '14:00:00', 'Agendado'); -- Consulta para segunda-feira

-- Estoque
INSERT INTO `inventory_consumables` (`item_name`, `quantity_in_stock`, `minimum_stock_level`, `unit_of_measure`) VALUES
('Luvas de Procedimento (Caixa)', 52, 20, 'Caixa'),
('Máscaras Descartáveis (Caixa)', 15, 15, 'Caixa'),
('Resina Composta A2 (Un.)', 8, 10, 'Unidade');

-- Kits
INSERT INTO `instrument_kits` (`kit_identifier`, `description`, `status`) VALUES
('KIT-CIR-001', 'Kit básico para cirurgias de siso.', 'Disponível'),
('KIT-ENDO-003', 'Kit para tratamento de canal.', 'Em Esterilização');

SELECT 'Banco de dados DentiBot v2.0 criado e populado com sucesso!' AS message;