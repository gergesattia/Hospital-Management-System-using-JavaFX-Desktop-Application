-- =============================================
-- 🏥 MEDICORE HOSPITAL MANAGEMENT SYSTEM
-- FINAL MYSQL DATABASE SCHEMA (JAVA PORT)
-- =============================================

-- 1. users
CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(255) NOT NULL,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,
    ssn VARCHAR(20) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('admin', 'doctor', 'receptionist', 'pharmacist') NOT NULL,
    salary DECIMAL(10, 2) NOT NULL DEFAULT 0.00 CHECK (salary >= 0),
    birth_date DATE NOT NULL DEFAULT '2000-01-01',
    is_active TINYINT(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. specializations
CREATE TABLE IF NOT EXISTS specializations (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) UNIQUE NOT NULL
);

-- 3. rooms (Clinics/Examination Rooms)
CREATE TABLE IF NOT EXISTS rooms (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) UNIQUE NOT NULL,
    status ENUM('Free', 'Busy') DEFAULT 'Free'
);

-- 4. doctors
CREATE TABLE IF NOT EXISTS doctors (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT UNIQUE NOT NULL,
    specialization_id INT,
    room_number VARCHAR(50),
    consultation_fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00 CHECK (consultation_fee >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (specialization_id) REFERENCES specializations(id) ON DELETE SET NULL
);

-- 5. nurses
CREATE TABLE IF NOT EXISTS nurses (
    id INT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(20) UNIQUE NOT NULL,
    ssn VARCHAR(20) UNIQUE NOT NULL,
    birth_date DATE NOT NULL DEFAULT '2000-01-01',
    doctor_id INT NULL,
    shift ENUM('Morning', 'Evening', 'Night'),
    department VARCHAR(100),
    experience_years INT DEFAULT 0,
    license_number VARCHAR(100) UNIQUE,
    hire_date DATE,
    salary DECIMAL(10, 2),
    is_available TINYINT(1) DEFAULT 1,
    is_active TINYINT(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE SET NULL
);

-- 5b. nurse_assignments (Max 2 per doctor enforced via app logic)
CREATE TABLE IF NOT EXISTS nurse_assignments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nurse_id INT NOT NULL UNIQUE,
    doctor_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (nurse_id) REFERENCES nurses(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
);

-- 6. patients
CREATE TABLE IF NOT EXISTS patients (
    id INT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    national_id VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(255),
    gender ENUM('Male', 'Female', 'Other'),
    birth_date DATE,
    address TEXT,
    priority_level INT DEFAULT 1,
    booked_specialization VARCHAR(255),
    telegram_chat_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. appointments
CREATE TABLE IF NOT EXISTS appointments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    priority INT CHECK(priority BETWEEN 1 AND 5),
    status ENUM('waiting', 'completed', 'cancelled', 'in_progress') DEFAULT 'waiting',
    appointment_date DATETIME NOT NULL,
    queue_number INT,
    consultation_revenue DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
);

-- 8. medical_records
CREATE TABLE IF NOT EXISTS medical_records (
    id INT PRIMARY KEY AUTO_INCREMENT,
    appointment_id INT UNIQUE NOT NULL,
    doctor_id INT NOT NULL,
    diagnosis TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
);

-- 9. prescriptions
CREATE TABLE IF NOT EXISTS prescriptions (
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

-- 10. medicines (pharmacy inventory)
CREATE TABLE IF NOT EXISTS medicines (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    stock_quantity INT DEFAULT 0,
    min_stock_alert INT DEFAULT 10,
    unit_price DECIMAL(10,2) DEFAULT 0.00, -- Selling Price
    cost_price DECIMAL(10,2) DEFAULT 0.00, -- Buying Price
    supplier VARCHAR(255),
    expiry_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 10b. prescription_dispensing
CREATE TABLE IF NOT EXISTS prescription_dispensing (
    id INT PRIMARY KEY AUTO_INCREMENT,
    prescription_id INT UNIQUE NOT NULL,
    dispensed_by INT NOT NULL,
    dispensed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE CASCADE,
    FOREIGN KEY (dispensed_by) REFERENCES users(id)
);

-- 11. suppliers
CREATE TABLE IF NOT EXISTS suppliers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 12. sales (Pharmacy POS)
CREATE TABLE IF NOT EXISTS sales (
    id INT PRIMARY KEY AUTO_INCREMENT,
    patient_id INT NULL, -- Can be null for walk-ins
    patient_name VARCHAR(255),
    total_amount DECIMAL(10,2) NOT NULL,
    discount DECIMAL(10,2) DEFAULT 0.00,
    payment_method ENUM('Cash', 'Card', 'Wallet') DEFAULT 'Cash',
    created_by INT, -- Pharmacist user_id
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- 13. sale_items
CREATE TABLE IF NOT EXISTS sale_items (
    id INT PRIMARY KEY AUTO_INCREMENT,
    sale_id INT NOT NULL,
    medicine_id INT NOT NULL,
    quantity INT NOT NULL,
    price_at_sale DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE,
    FOREIGN KEY (medicine_id) REFERENCES medicines(id)
);

-- 14. stock_movements (Audit Trail)
CREATE TABLE IF NOT EXISTS stock_movements (
    id INT PRIMARY KEY AUTO_INCREMENT,
    medicine_id INT NOT NULL,
    type ENUM('in', 'out', 'adjustment') NOT NULL,
    quantity INT NOT NULL,
    reference_type VARCHAR(50), -- 'sale', 'purchase', 'manual'
    reference_id INT, -- sale_id or purchase_id
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE
);

-- 15. face_encodings (Biometric Data)
-- Stores the 512-dimensional vector as a JSON string or BLOB
CREATE TABLE IF NOT EXISTS face_encodings (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT UNIQUE NOT NULL,
    encoding_json LONGTEXT NOT NULL, 
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 16. login_logs
CREATE TABLE IF NOT EXISTS login_logs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    username VARCHAR(100),
    status ENUM('success', 'failed_password', 'failed_biometric') NOT NULL,
    ip_address VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- 💉 SEED DATA
-- =============================================

INSERT IGNORE INTO specializations (name) VALUES 
('General Medicine'), ('Pediatrics'), ('Cardiology'), ('Dermatology'), ('Orthopedics');

INSERT IGNORE INTO rooms (name) VALUES 
('Room 101'), ('Room 102'), ('Room 103'), ('Room 201'), ('Room 202');

-- Default admin user (password: 123456)
INSERT IGNORE INTO users (full_name, username, email, phone, ssn, password_hash, role) 
VALUES ('System Administrator', 'admin', 'admin@medicore.local', '01000000000', '12345678901234', '123456', 'admin');

-- Seed Medicines
INSERT IGNORE INTO medicines (name, stock_quantity, unit_price, cost_price) VALUES
('Paracetamol 500mg', 100, 5.00, 3.50),
('Amoxicillin 250mg', 50, 15.00, 10.00),
('Ibuprofen 400mg', 80, 8.00, 5.00);
