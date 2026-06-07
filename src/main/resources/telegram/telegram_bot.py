import os
import sys
import logging
import asyncio
import mysql.connector
from datetime import datetime
from telegram import Update, ReplyKeyboardMarkup, KeyboardButton, ReplyKeyboardRemove
from telegram.ext import (
    ApplicationBuilder,
    CommandHandler,
    MessageHandler,
    filters,
    ContextTypes,
)
import bot_config

# Setup logging
logging.basicConfig(
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    level=logging.INFO,
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler("telegram_bot.log", encoding="utf-8")
    ]
)
logger = logging.getLogger(__name__)

# Track last sent prescription to prevent duplicate notifications
last_prescription_id = 0

def get_db_connection():
    return mysql.connector.connect(
        host=bot_config.DB_HOST,
        port=bot_config.DB_PORT,
        user=bot_config.DB_USER,
        password=bot_config.DB_PASSWORD,
        database=bot_config.DB_NAME
    )

def init_last_prescription_id():
    global last_prescription_id
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT MAX(id) FROM prescriptions")
        row = cursor.fetchone()
        if row and row[0] is not None:
            last_prescription_id = row[0]
            logger.info(f"Initialized last prescription ID to: {last_prescription_id}")
        else:
            last_prescription_id = 0
            logger.info("No existing prescriptions found. Setting last ID to 0.")
        cursor.close()
        conn.close()
    except Exception as e:
        logger.error(f"Error initializing last prescription ID: {e}")

def normalize_phone(phone):
    if not phone:
        return ""
    # Keep only digits
    cleaned = "".join(c for c in phone if c.isdigit())
    # Convert international prefix (e.g., 201012345678 -> 01012345678)
    if cleaned.startswith("20") and len(cleaned) > 10:
        cleaned = "0" + cleaned[2:]
    return cleaned

def get_specializations():
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT name FROM specializations ORDER BY name ASC")
        rows = cursor.fetchall()
        cursor.close()
        conn.close()
        return [r[0] for r in rows]
    except Exception as e:
        logger.error(f"Error fetching specializations from DB: {e}")
        return []

def get_doctors_by_specialization():
    """Returns dict: { specialization_name: [doctor_name, ...] }"""
    try:
        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)
        sql = """
            SELECT u.full_name AS doctor_name, COALESCE(s.name, 'General') AS spec_name
            FROM doctors d
            JOIN users u ON d.user_id = u.id
            LEFT JOIN specializations s ON d.specialization_id = s.id
            WHERE u.is_active = 1
            ORDER BY s.name ASC, u.full_name ASC
        """
        cursor.execute(sql)
        rows = cursor.fetchall()
        cursor.close()
        conn.close()

        result = {}
        for r in rows:
            spec = r['spec_name']
            if spec not in result:
                result[spec] = []
            result[spec].append(r['doctor_name'])
        return result
    except Exception as e:
        logger.error(f"Error fetching doctors by specialization: {e}")
        return {}


# Main Keyboard
def get_main_keyboard():
    keyboard = [
        [KeyboardButton(" الروشتات (My Prescriptions)"), KeyboardButton(" الموعد القادم (Next Appointment)")],
        [KeyboardButton(" التاريخ الطبي (Medical History)"), KeyboardButton(" معلومات العيادة (Clinic Info)")],
        [KeyboardButton(" الدكاترة والتخصصات (Our Doctors)")],
        [KeyboardButton(" مشاركة رقم الهاتف (Share Contact)", request_contact=True)]
    ]
    return ReplyKeyboardMarkup(keyboard, resize_keyboard=True, one_time_keyboard=False)

# Start Command
async def start_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    user_first_name = update.effective_user.first_name
    welcome_text = (
        f" *أهلاً بك يا {user_first_name} في بوت عيادة {bot_config.CLINIC_NAME}!* \n\n"
        "يسعدنا خدمتك وتسهيل وصولك لبياناتك الطبية.\n"
        "يرجى الضغط على أحد الأزرار بالأسفل، أو إرسال رقم هاتفك مباشرة المسجل في العيادة (مثال: `01012345678`) لاسترجاع بياناتك."
    )
    await update.message.reply_text(
        welcome_text,
        parse_mode="Markdown",
        reply_markup=get_main_keyboard()
    )

# Contact Sharing Handler
async def contact_handler(update: Update, context: ContextTypes.DEFAULT_TYPE):
    contact = update.message.contact
    raw_phone = contact.phone_number
    normalized = normalize_phone(raw_phone)
    chat_id = update.effective_chat.id
    
    logger.info(f"Received contact from chat {chat_id}: raw={raw_phone}, normalized={normalized}")
    
    await process_phone_lookup(update, normalized, chat_id)

# Text / Manual phone lookup
async def message_handler(update: Update, context: ContextTypes.DEFAULT_TYPE):
    text = update.message.text.strip()
    chat_id = update.effective_chat.id
    
    if text == " الروشتات (My Prescriptions)":
        await update.message.reply_text(
            "رجاءً أرسل رقم هاتفك المسجل في العيادة، أو اضغط على زر *مشاركة رقم الهاتف* بالأسفل 📱",
            parse_mode="Markdown"
        )
        return
    elif text == " الموعد القادم (Next Appointment)":
        context.user_data['action'] = 'appointment'
        await update.message.reply_text("رجاءً أرسل رقم هاتفك المسجل للاستعلام عن موعدك القادم:")
        return
    elif text == " التاريخ الطبي (Medical History)":
        context.user_data['action'] = 'history'
        await update.message.reply_text("رجاءً أرسل رقم هاتفك المسجل لعرض تاريخك الطبي:")
        return
    elif text == " الدكاترة والتخصصات (Our Doctors)":
        docs = get_doctors_by_specialization()
        if not docs:
            await update.message.reply_text("⚠️ لا يوجد دكاترة مسجلين حالياً.")
            return
        msg = f" *الدكاترة المتاحين في {bot_config.CLINIC_NAME}*\n"
        msg += "━━━━━━━━━━━━━━━━━━━\n\n"
        for spec, doctors in docs.items():
            msg += f"🩺 *{spec}:*\n"
            for doc in doctors:
                msg += f"   • د. {doc}\n"
            msg += "\n"
        msg += "━━━━━━━━━━━━━━━━━━━\n"
        msg += " للحجز اتصل بـ: 01030650770"
        await update.message.reply_text(msg, parse_mode="Markdown")
        return
    elif text == "🏥 معلومات العيادة (Clinic Info)":
        specs = get_specializations()
        specs_str = "، ".join(specs) if specs else "الباطنة، الأطفال، القلب، الجلدية، العظام"
        info_text = (
            f"🏥 *مستشفى {bot_config.CLINIC_NAME}* \n"
            "━━━━━━━━━━━━━━━━━━━\n"
            " *العنوان:* القاهرة، مصر\n"
            " *رقم التواصل:* 01030650770\n"
            " *مواعيد العمل:* يومياً من 10:00 صباحاً حتى 10:00 مساءً ماعدا الجمعة.\n"
            f" *التخصصات المتوفرة:* {specs_str}."
        )
        await update.message.reply_text(info_text, parse_mode="Markdown")
        return
        
    # Check if text looks like a phone number
    normalized = normalize_phone(text)
    if len(normalized) >= 10 and normalized.isdigit():
        action = context.user_data.get('action', 'prescription')
        if action == 'appointment':
            await process_appointment_lookup(update, normalized)
            context.user_data['action'] = 'prescription' # reset
        elif action == 'history':
            await process_history_lookup(update, normalized)
            context.user_data['action'] = 'prescription' # reset
        else:
            await process_phone_lookup(update, normalized, chat_id)
    else:
        await update.message.reply_text(
            "⚠️ عذراً، لم أفهم طلبك. برجاء كتابة رقم الهاتف بشكل صحيح المكون من 11 رقماً، أو استخدام الأزرار المتاحة.",
            reply_markup=get_main_keyboard()
        )

# Database lookup & update chat_id
async def process_phone_lookup(update: Update, phone: str, chat_id: int):
    try:
        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)
        
        # 1. Look up patient
        cursor.execute("SELECT id, full_name, telegram_chat_id FROM patients WHERE phone = %s", (phone,))
        patient = cursor.fetchone()
        
        if not patient:
            await update.message.reply_text(
                "❌ لم نجد أي مريض مسجل برقم الهاتف هذا في نظام العيادة.\n"
                "تأكد من إدخال الرقم الصحيح أو تواصل مع السكرتارية لتسجيل رقمك."
            )
            cursor.close()
            conn.close()
            return
            
        # 2. Update chat ID if not already saved
        if patient['telegram_chat_id'] != chat_id:
            cursor.execute("UPDATE patients SET telegram_chat_id = %s WHERE id = %s", (chat_id, patient['id']))
            conn.commit()
            logger.info(f"Updated telegram_chat_id={chat_id} for patient {patient['full_name']} (ID {patient['id']})")
            
        # 3. Get latest prescription grouped by medical_record_id
        sql = """
            SELECT pr.id, pr.medicine_name, pr.dosage, pr.frequency, pr.duration_days, pr.instructions, pr.created_at,
                   u.full_name AS doctor_name, mr.id AS record_id
            FROM prescriptions pr
            JOIN medical_records mr ON pr.medical_record_id = mr.id
            JOIN appointments a ON mr.appointment_id = a.id
            JOIN doctors d ON mr.doctor_id = d.id
            JOIN users u ON d.user_id = u.id
            WHERE a.patient_id = %s
            ORDER BY mr.id DESC, pr.id ASC
        """
        cursor.execute(sql, (patient['id'],))
        rows = cursor.fetchall()
        
        if not rows:
            await update.message.reply_text(
                f" *المريض:* {patient['full_name']}\n\n"
                " لا توجد روشتات مسجلة لك حتى الآن في النظام."
            )
            cursor.close()
            conn.close()
            return
            
        # Group by medical_record_id to get the most recent visit
        latest_record_id = rows[0]['record_id']
        latest_rows = [r for r in rows if r['record_id'] == latest_record_id]
        
        # Format response
        date_str = latest_rows[0]['created_at'].strftime("%Y-%m-%d %I:%M %p")
        doctor = latest_rows[0]['doctor_name']
        
        msg = (
            f" *روشتتك الطبية الرسمية من {bot_config.CLINIC_NAME}* \n"
            f"━━━━━━━━━━━━━━━━━━━\n"
            f" *المريض:* {patient['full_name']}\n"
            f" *الطبيب المعالج:* د. {doctor}\n"
            f" *التاريخ:* {date_str}\n"
            f"━━━━━━━━━━━━━━━━━━━\n\n"
        )
        
        for idx, row in enumerate(latest_rows, 1):
            msg += f" *علاج {idx}:* `{row['medicine_name']}`\n"
            msg += f"    *الجرعة:* {row['dosage']}\n"
            msg += f"    *التكرار:* {row['frequency']}\n"
            msg += f"    *المدة:* {row['duration_days']} أيام\n"
            if row['instructions'] and row['instructions'].strip():
                msg += f"    *ملاحظات:* {row['instructions']}\n"
            msg += "\n"
            
        msg += "━━━━━━━━━━━━━━━━━━━\n"
        msg += " تم تفعيل الإشعارات التلقائية! سيقوم البوت بإرسال أي روشتة جديدة فور كتابتها من قبل الطبيب 🔔"
        
        await update.message.reply_text(msg, parse_mode="Markdown")
        cursor.close()
        conn.close()
        
    except Exception as e:
        logger.error(f"Database error in process_phone_lookup: {e}")
        await update.message.reply_text("⚠️ حدث خطأ أثناء الاتصال بقاعدة البيانات. يرجى المحاولة مرة أخرى لاحقاً.")

# Appointment lookup
async def process_appointment_lookup(update: Update, phone: str):
    try:
        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)
        
        sql = """
            SELECT a.appointment_date, a.status, a.queue_number, p.full_name, u.full_name as doctor_name
            FROM appointments a
            JOIN patients p ON a.patient_id = p.id
            JOIN doctors d ON a.doctor_id = d.id
            JOIN users u ON d.user_id = u.id
            WHERE p.phone = %s AND a.status IN ('waiting', 'in_progress')
            ORDER BY a.appointment_date ASC
            LIMIT 1
        """
        cursor.execute(sql, (phone,))
        app = cursor.fetchone()
        
        if not app:
            await update.message.reply_text("📅 لا توجد مواعيد قادمة أو قيد الانتظار مسجلة برقم الهاتف هذا.")
        else:
            date_str = app['appointment_date'].strftime("%Y-%m-%d %I:%M %p")
            status_arabic = "قيد الانتظار" if app['status'] == 'waiting' else "في الغرفة الآن"
            msg = (
                f" *موعدك القادم في {bot_config.CLINIC_NAME}* \n"
                "━━━━━━━━━━━━━━━━━━━\n"
                f" *المريض:* {app['full_name']}\n"
                f" *الطبيب:* د. {app['doctor_name']}\n"
                f" *الوقت:* {date_str}\n"
                f" *رقم الدور الخاص بك:* {app['queue_number']}\n"
                f" *الحالة:* {status_arabic}\n"
                "━━━━━━━━━━━━━━━━━━━\n"
                "يرجى الحضور قبل الموعد بـ 15 دقيقة."
            )
            await update.message.reply_text(msg, parse_mode="Markdown")
            
        cursor.close()
        conn.close()
    except Exception as e:
        logger.error(f"Error lookup appointment: {e}")
        await update.message.reply_text("⚠️ عذراً، حدث خطأ أثناء جلب تفاصيل الموعد.")

# Medical History lookup
async def process_history_lookup(update: Update, phone: str):
    try:
        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)
        
        sql = """
            SELECT mr.created_at, mr.diagnosis, mr.notes, u.full_name as doctor_name, p.full_name as patient_name
            FROM medical_records mr
            JOIN appointments a ON mr.appointment_id = a.id
            JOIN patients p ON a.patient_id = p.id
            JOIN doctors d ON mr.doctor_id = d.id
            JOIN users u ON d.user_id = u.id
            WHERE p.phone = %s
            ORDER BY mr.id DESC
            LIMIT 3
        """
        cursor.execute(sql, (phone,))
        records = cursor.fetchall()
        
        if not records:
            await update.message.reply_text("📜 لا يوجد تاريخ طبي مسجل برقم الهاتف هذا بعد.")
        else:
            msg = (
                f"📜 *السجل الطبي الأخير ({records[0]['patient_name']})* \n"
                "━━━━━━━━━━━━━━━━━━━\n\n"
            )
            for idx, r in enumerate(records, 1):
                date_str = r['created_at'].strftime("%Y-%m-%d")
                msg += f" *زيارة رقم {idx} ({date_str}):*\n"
                msg += f"    *الطبيب:* د. {r['doctor_name']}\n"
                msg += f"    *التشخيص:* {r['diagnosis']}\n"
                if r['notes'] and r['notes'].strip():
                    msg += f"    *ملاحظات:* {r['notes']}\n"
                msg += "\n--------------------\n\n"
                
            await update.message.reply_text(msg, parse_mode="Markdown")
            
        cursor.close()
        conn.close()
    except Exception as e:
        logger.error(f"Error lookup medical history: {e}")
        await update.message.reply_text("⚠️ عذراً، حدث خطأ أثناء جلب التاريخ الطبي.")

# Push Notification Task (Runs every 10 seconds in background)
async def db_polling_task(application):
    global last_prescription_id
    logger.info("Database notification poller started...")
    
    while True:
        await asyncio.sleep(10)
        try:
            conn = get_db_connection()
            cursor = conn.cursor(dictionary=True)
            
            # Fetch new prescriptions for patients with chat IDs
            sql = """
                SELECT pr.id, pr.medical_record_id, pr.medicine_name, pr.dosage, pr.frequency, pr.duration_days, pr.instructions, pr.created_at,
                       p.telegram_chat_id, p.full_name AS patient_name, u.full_name AS doctor_name
                FROM prescriptions pr
                JOIN medical_records mr ON pr.medical_record_id = mr.id
                JOIN appointments a ON mr.appointment_id = a.id
                JOIN patients p ON a.patient_id = p.id
                JOIN doctors d ON mr.doctor_id = d.id
                JOIN users u ON d.user_id = u.id
                WHERE pr.id > %s AND p.telegram_chat_id IS NOT NULL
                ORDER BY pr.medical_record_id ASC, pr.id ASC
            """
            cursor.execute(sql, (last_prescription_id,))
            new_rows = cursor.fetchall()
            
            if new_rows:
                logger.info(f"Poller found {len(new_rows)} new prescription items.")
                
                # Group items by medical_record_id to send one message per visit
                groups = {}
                for row in new_rows:
                    mr_id = row['medical_record_id']
                    if mr_id not in groups:
                        groups[mr_id] = []
                    groups[mr_id].append(row)
                
                for mr_id, items in groups.items():
                    chat_id = items[0]['telegram_chat_id']
                    patient = items[0]['patient_name']
                    doctor = items[0]['doctor_name']
                    date_str = items[0]['created_at'].strftime("%Y-%m-%d %I:%M %p")
                    
                    msg = (
                        f" *روشتة جديدة تم إصدارها لك الآن!* 🔔\n"
                        f" *{bot_config.CLINIC_NAME}*\n"
                        f"━━━━━━━━━━━━━━━━━━━\n"
                        f" *المريض:* {patient}\n"
                        f" *الطبيب:* د. {doctor}\n"
                        f" *الوقت:* {date_str}\n"
                        f"━━━━━━━━━━━━━━━━━━━\n\n"
                    )
                    
                    for idx, row in enumerate(items, 1):
                        msg += f" *علاج {idx}:* `{row['medicine_name']}`\n"
                        msg += f"    *الجرعة:* {row['dosage']}\n"
                        msg += f"    *التكرار:* {row['frequency']}\n"
                        msg += f"    *المدة:* {row['duration_days']} أيام\n"
                        if row['instructions'] and row['instructions'].strip():
                            msg += f"    *ملاحظات:* {row['instructions']}\n"
                        msg += "\n"
                        
                    msg += "━━━━━━━━━━━━━━━━━━━\n"
                    msg += "سلامتك يا رب 🩺 نتمنى لك الشفاء العاجل 🤲"
                    
                    try:
                        await application.bot.send_message(
                            chat_id=chat_id,
                            text=msg,
                            parse_mode="Markdown"
                        )
                        logger.info(f"Successfully sent prescription push message to chat {chat_id} for {patient}")
                    except Exception as send_err:
                        logger.error(f"Failed to send telegram message to chat {chat_id}: {send_err}")
                
                # Update high-water mark
                max_id = max(row['id'] for row in new_rows)
                last_prescription_id = max_id
                logger.info(f"Updated last_prescription_id to {last_prescription_id}")
                
            cursor.close()
            conn.close()
        except Exception as poll_err:
            logger.error(f"Error in polling task: {poll_err}")

# Main Runner
def main():
    logger.info("Initializing MediCore Telegram Bot...")
    init_last_prescription_id()
    
    app = ApplicationBuilder().token(bot_config.BOT_TOKEN).build()
    
    # Handlers
    app.add_handler(CommandHandler("start", start_command))
    app.add_handler(MessageHandler(filters.CONTACT, contact_handler))
    app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, message_handler))
    
    # Start bot and background poller
    loop = asyncio.get_event_loop()
    loop.create_task(db_polling_task(app))
    
    logger.info("MediCore Telegram Bot started successfully and listening for updates!")
    app.run_polling()

if __name__ == '__main__':
    main()
