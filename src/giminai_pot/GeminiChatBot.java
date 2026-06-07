import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

public class GeminiChatBot {

    // إعدادات قاعدة البيانات (قم بتغييرها لتناسب إعداداتك)
    private static final String DB_URL = "jdbc:mysql://localhost:3306/medicore_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Attia123@#";

    // مفتاح API الخاص بك لـ Gemini
    private static final String API_KEY = "AIzaSyDgMxsLfnRqm-9x8f0RcTeMIf5xlilzuLs";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key="
            + API_KEY;

    // وصف مبسط لقاعدة البيانات ليفهمها Gemini
    private static final String DB_SCHEMA = "Tables in medicore_db:\n" +
            "- users(id, full_name, role, salary, is_active)\n" +
            "- patients(id, full_name, gender, priority_level)\n" +
            "- specializations(id, name)\n" +
            "- doctors(id, user_id, specialization_id, consultation_fee)\n" +
            "- appointments(id, patient_id, doctor_id, status, consultation_revenue, appointment_date)\n" +
            "- sales(id, total_amount, discount, payment_method, created_at)\n" +
            "- medicines(id, name, stock_quantity, unit_price, cost_price)\n" +
            "- sale_items(id, sale_id, medicine_id, quantity, price_at_sale)\n\n" +
            "Hints:\n" +
            "- To calculate total revenue (الإيرادات), sum 'consultation_revenue' from 'appointments' AND 'total_amount' from 'sales'.\n"
            +
            "- To calculate net profit (صافي الربح), subtract medicine costs (quantity * cost_price in medicines) and doctor/user salaries from total revenue.\n"
            +
            "- To find top specializations, count appointments per specialization_id.\n";

    public static void main(String[] args) {
        String question1 = "ما هو إجمالي الإيرادات في العيادة؟";
        System.out.println("سؤال الأدمن: " + question1);
        String answer1 = askAdminQuestion(question1);
        System.out.println("إجابة البوت:\n" + answer1);
        System.out.println("--------------------------------------------------");

        String question2 = "ما هي أكثر التخصصات التي عليها إقبال؟";
        System.out.println("سؤال الأدمن: " + question2);
        String answer2 = askAdminQuestion(question2);
        System.out.println("إجابة البوت:\n" + answer2);
    }

    public static String askAdminQuestion(String question) {
        try {
            // 1. اطلب من Gemini تحويل السؤال إلى استعلام SQL
            String sqlPrompt = "You are a MySQL expert. Given this database schema:\n" + DB_SCHEMA +
                    "\nWrite a SQL query to answer this user question: '" + question + "'.\n" +
                    "IMPORTANT: Return ONLY the raw SQL query, no markdown, no explanation, no ```sql tags.";

            String generatedSql = callGeminiAPI(sqlPrompt).trim();
            // تنظيف الكود إذا قام بإضافة علامات markdown عن طريق الخطأ
            generatedSql = generatedSql.replace("```sql", "").replace("```", "").trim();

            System.out.println("[Debug] Generated SQL: " + generatedSql);

            // 2. تنفيذ الاستعلام على قاعدة البيانات
            String queryResults = executeQuery(generatedSql);
            System.out.println("[Debug] Query Results: " + queryResults);

            // 3. إرسال النتائج إلى Gemini لصياغة إجابة باللغة العربية
            String finalPrompt = "The admin asked: '" + question + "'.\n" +
                    "The database returned this data:\n" + queryResults + "\n" +
                    "Formulate a friendly, concise, and professional response in Arabic for the admin explaining this data. "
                    +
                    "Do not mention the SQL query itself.";

            String finalAnswer = callGeminiAPI(finalPrompt);
            return finalAnswer;

        } catch (Exception e) {
            return "حدث خطأ أثناء معالجة طلبك: " + e.getMessage();
        }
    }

    private static String executeQuery(String sql) throws Exception {
        if (!sql.toUpperCase().startsWith("SELECT")) {
            throw new Exception("Only SELECT queries are allowed for safety.");
        }

        StringBuilder results = new StringBuilder();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // إضافة أسماء الأعمدة
            for (int i = 1; i <= columnCount; i++) {
                results.append(metaData.getColumnName(i)).append("\t");
            }
            results.append("\n");

            // إضافة البيانات
            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
                for (int i = 1; i <= columnCount; i++) {
                    results.append(rs.getString(i)).append("\t");
                }
                results.append("\n");
                if (rowCount > 50) { // حد أقصى للنتائج لتجنب إرسال بيانات ضخمة لـ Gemini
                    results.append("... (more rows omitted)\n");
                    break;
                }
            }
            if (rowCount == 0) {
                return "No data found.";
            }
        }
        return results.toString();
    }

    private static String callGeminiAPI(String prompt) throws Exception {
        URL url = new URL(GEMINI_API_URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        // تنظيف الـ Prompt ليكون متوافقاً مع JSON
        String safePrompt = prompt.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
        String jsonPayload = "{\"contents\":[{\"parts\":[{\"text\":\"" + safePrompt + "\"}]}]}";

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonPayload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = con.getResponseCode();
        if (responseCode != 200) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getErrorStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                throw new Exception("Gemini API Error: " + response.toString());
            }
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return extractTextFromJson(response.toString());
        }
    }

    // دالة مبسطة جداً لاستخراج النص من استجابة Gemini (JSON)
    // يُفضل استخدام مكتبة مثل Gson أو org.json في مشروعك الحقيقي
    private static String extractTextFromJson(String json) {
        String key = "\"text\": \"";
        int startIndex = json.indexOf(key);
        if (startIndex == -1) {
            return "Could not extract response from JSON: " + json;
        }
        startIndex += key.length();
        int endIndex = json.indexOf("\"", startIndex);

        // قد يحتوي النص على " escaped, لذا نبحث عن نهاية النص الفعلي
        while (endIndex > 0 && json.charAt(endIndex - 1) == '\\') {
            endIndex = json.indexOf("\"", endIndex + 1);
        }

        if (endIndex == -1)
            return "Parsing error";

        String extracted = json.substring(startIndex, endIndex);
        // إزالة التهريب (Unescape)
        extracted = extracted.replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\t", "\t")
                .replace("\\*", "*");
        return extracted;
    }
}
