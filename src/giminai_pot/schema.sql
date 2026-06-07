USE medicore_db;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS prescription_dispensing;
DROP TABLE IF EXISTS prescriptions;
DROP TABLE IF EXISTS medical_records;
DROP TABLE IF EXISTS appointments;
DROP TABLE IF EXISTS nurse_assignments;
DROP TABLE IF EXISTS nurses;
DROP TABLE IF EXISTS doctors;
DROP TABLE IF EXISTS face_encodings;
DROP TABLE IF EXISTS login_logs;
DROP TABLE IF EXISTS sale_items;
DROP TABLE IF EXISTS sales;
DROP TABLE IF EXISTS stock_movements;
DROP TABLE IF EXISTS medicines;
DROP TABLE IF EXISTS suppliers;
DROP TABLE IF EXISTS patients;
DROP TABLE IF EXISTS specializations;
DROP TABLE IF EXISTS rooms;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
  id INT PRIMARY KEY AUTO_INCREMENT,
  full_name VARCHAR(255) NOT NULL,
  username VARCHAR(100) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL UNIQUE,
  phone VARCHAR(20) NOT NULL UNIQUE,
  ssn VARCHAR(20) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('admin','doctor','nurse','receptionist','pharmacist') NOT NULL,
  salary DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  birth_date DATE NOT NULL DEFAULT '2000-01-01',
  is_active TINYINT(1) DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE specializations (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE patients (
  id INT PRIMARY KEY AUTO_INCREMENT,
  full_name VARCHAR(255) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  national_id VARCHAR(20) NOT NULL UNIQUE,
  email VARCHAR(255),
  gender ENUM('Male','Female','Other'),
  birth_date DATE,
  address TEXT,
  priority_level INT DEFAULT 1,
  telegram_chat_id BIGINT DEFAULT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE doctors (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL UNIQUE,
  specialization_id INT,
  room_number VARCHAR(50),
  consultation_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (specialization_id) REFERENCES specializations(id) ON DELETE SET NULL
);

CREATE TABLE nurses (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL UNIQUE,
  doctor_id INT,
  shift ENUM('Morning','Evening','Night'),
  department VARCHAR(100),
  experience_years INT DEFAULT 0,
  license_number VARCHAR(100) UNIQUE,
  hire_date DATE,
  salary DECIMAL(10,2),
  is_available TINYINT(1) DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE SET NULL
);

CREATE TABLE nurse_assignments (
  id INT PRIMARY KEY AUTO_INCREMENT,
  nurse_user_id INT NOT NULL UNIQUE,
  doctor_id INT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (nurse_user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
);

CREATE TABLE rooms (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL UNIQUE,
  status ENUM('Free','Busy') DEFAULT 'Free'
);

CREATE TABLE appointments (
  id INT PRIMARY KEY AUTO_INCREMENT,
  patient_id INT NOT NULL,
  doctor_id INT NOT NULL,
  priority INT CHECK (priority BETWEEN 1 AND 5),
  status ENUM('waiting','completed','cancelled','in_progress') DEFAULT 'waiting',
  appointment_date DATETIME NOT NULL,
  queue_number INT,
  consultation_revenue DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
  FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
);

CREATE TABLE medical_records (
  id INT PRIMARY KEY AUTO_INCREMENT,
  appointment_id INT NOT NULL UNIQUE,
  doctor_id INT NOT NULL,
  diagnosis TEXT,
  notes TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE,
  FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
);

CREATE TABLE prescriptions (
  id INT PRIMARY KEY AUTO_INCREMENT,
  medical_record_id INT NOT NULL,
  medicine_name VARCHAR(255) NOT NULL,
  dosage VARCHAR(100),
  frequency VARCHAR(100),
  duration_days INT,
  instructions TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (medical_record_id) REFERENCES medical_records(id) ON DELETE CASCADE
);

CREATE TABLE prescription_dispensing (
  id INT PRIMARY KEY AUTO_INCREMENT,
  prescription_id INT NOT NULL UNIQUE,
  dispensed_by INT NOT NULL,
  dispensed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE CASCADE,
  FOREIGN KEY (dispensed_by) REFERENCES users(id)
);

CREATE TABLE medicines (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL UNIQUE,
  description TEXT,
  stock_quantity INT DEFAULT 0,
  min_stock_alert INT DEFAULT 10,
  unit_price DECIMAL(10,2) DEFAULT 0.00,
  cost_price DECIMAL(10,2) DEFAULT 0.00,
  supplier_id INT,
  expiry_date DATE,
  supplier VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE suppliers (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  phone VARCHAR(20),
  address TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sales (
  id INT PRIMARY KEY AUTO_INCREMENT,
  patient_id INT,
  patient_name VARCHAR(255),
  total_amount DECIMAL(10,2) NOT NULL,
  discount DECIMAL(10,2) DEFAULT 0.00,
  payment_method ENUM('Cash','Card','Wallet') DEFAULT 'Cash',
  created_by INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE sale_items (
  id INT PRIMARY KEY AUTO_INCREMENT,
  sale_id INT NOT NULL,
  medicine_id INT NOT NULL,
  quantity INT NOT NULL,
  price_at_sale DECIMAL(10,2) NOT NULL,
  FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE,
  FOREIGN KEY (medicine_id) REFERENCES medicines(id)
);

CREATE TABLE stock_movements (
  id INT PRIMARY KEY AUTO_INCREMENT,
  medicine_id INT NOT NULL,
  type ENUM('in','out','adjustment') NOT NULL,
  quantity INT NOT NULL,
  reference_type VARCHAR(50),
  reference_id INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE
);

CREATE TABLE face_encodings (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL UNIQUE,
  encoding_json LONGTEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE login_logs (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT,
  username VARCHAR(100),
  status ENUM('success','failed_password','failed_biometric') NOT NULL,
  ip_address VARCHAR(50),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
