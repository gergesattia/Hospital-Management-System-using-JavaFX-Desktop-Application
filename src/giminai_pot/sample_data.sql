USE medicore_db;

-- إيقاف التحقق من المفاتيح الأجنبية مؤقتاً لتسهيل إدخال البيانات إذا لزم الأمر
SET FOREIGN_KEY_CHECKS = 0;

-- 1. إضافة المستخدمين (أدمن، أطباء، ممرضة، صيدلي)
INSERT INTO users (full_name, username, email, phone, ssn, password_hash, role, salary) VALUES
('Ahmed Admin', 'ahmed_admin', 'ahmed@medicore.com', '01000000001', '29001010101011', 'hashed_pass', 'admin', 15000.00),
('Dr. Sara Ali', 'sara_doc', 'sara@medicore.com', '01000000002', '29001010101012', 'hashed_pass', 'doctor', 20000.00),
('Dr. Omar Khaled', 'omar_doc', 'omar@medicore.com', '01000000003', '29001010101013', 'hashed_pass', 'doctor', 25000.00),
('Mona Nurse', 'mona_nurse', 'mona@medicore.com', '01000000004', '29001010101014', 'hashed_pass', 'nurse', 8000.00),
('Khaled Pharma', 'khaled_pharma', 'khaled@medicore.com', '01000000005', '29001010101015', 'hashed_pass', 'pharmacist', 10000.00);

-- 2. إضافة التخصصات الطبية
INSERT INTO specializations (name) VALUES 
('Cardiology - قلب'), 
('Pediatrics - أطفال'), 
('Dermatology - جلدية'), 
('Orthopedics - عظام');

-- 3. إضافة الأطباء (وربطهم بالمستخدمين والتخصصات)
INSERT INTO doctors (user_id, specialization_id, room_number, consultation_fee) VALUES
(2, 1, 'Room 101', 500.00), -- دكتورة سارة (قلب)، الكشف 500
(3, 2, 'Room 102', 400.00); -- دكتور عمر (أطفال)، الكشف 400

-- 4. إضافة المرضى
INSERT INTO patients (full_name, phone, national_id, email, gender, birth_date) VALUES
('Ali Hassan', '01111111111', '28001010101011', 'alihassan@gmail.com', 'Male', '1980-05-15'),
('Nourhan Tarek', '01222222222', '29501010101012', 'nourhan@gmail.com', 'Female', '1995-10-20'),
('Kareem Youssef', '01555555555', '30001010101013', 'kareem@gmail.com', 'Male', '2010-02-12');

-- 5. إضافة الحجوزات (المواعيد) - بعضها مكتمل والآخر في الانتظار
-- الدخل من الكشفيات هنا: 500 + 400 + 400 = 1300
INSERT INTO appointments (patient_id, doctor_id, priority, status, appointment_date, consultation_revenue) VALUES
(1, 1, 1, 'completed', '2026-05-10 10:00:00', 500.00),
(2, 2, 2, 'completed', '2026-05-11 11:30:00', 400.00),
(3, 2, 1, 'waiting', '2026-05-15 12:00:00', 400.00),
(2, 1, 1, 'completed', '2026-05-13 14:00:00', 500.00);

-- 6. إضافة الأدوية في الصيدلية (تكلفة الدواء وسعر البيع)
INSERT INTO medicines (name, description, stock_quantity, unit_price, cost_price) VALUES
('Panadol Extra', 'Painkiller', 100, 50.00, 30.00),
('Amoxicillin', 'Antibiotic', 50, 120.00, 80.00),
('Omeprazole', 'Antacid', 200, 80.00, 50.00);

-- 7. إضافة مبيعات من الصيدلية (فواتير)
-- المبيعات: 170 (ربح 60) + 80 (ربح 30)
INSERT INTO sales (patient_id, patient_name, total_amount, payment_method, created_by) VALUES
(1, 'Ali Hassan', 170.00, 'Cash', 5), 
(2, 'Nourhan Tarek', 80.00, 'Card', 5);

-- 8. تفاصيل المبيعات (Sale Items)
INSERT INTO sale_items (sale_id, medicine_id, quantity, price_at_sale) VALUES
(1, 1, 1, 50.00), -- بانادول
(1, 2, 1, 120.00), -- اموكسيسلين
(2, 3, 1, 80.00); -- اوميبرازول

SET FOREIGN_KEY_CHECKS = 1;
