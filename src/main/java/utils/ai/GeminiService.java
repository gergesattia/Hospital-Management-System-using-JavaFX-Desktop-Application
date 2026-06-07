package utils.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dao.DatabaseConnection;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

public class GeminiService {

    private static final String API_KEY = "AIzaSyDgMxsLfnRqm-9x8f0RcTeMIf5xlilzuLs";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + API_KEY;

    private static final String HINTS = "\nHints:\n" +
            "- To calculate total revenue (الإيرادات), sum 'consultation_revenue' from 'appointments' AND 'total_amount' from 'sales'.\n" +
            "- To calculate net profit (صافي الربح), subtract medicine costs (quantity * cost_price in medicines) and doctor/user salaries from total revenue.\n" +
            "- To find top specializations, count appointments per specialization_id.\n";

    private static final Gson gson = new Gson();

    private static String getDynamicSchema() {
        StringBuilder schema = new StringBuilder("Tables in database:\n");
        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Get Specializations from DB
            schema.append("SPECIALIZATIONS in system: ");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT name FROM specializations")) {
                while (rs.next()) {
                    schema.append(rs.getString("name")).append(", ");
                }
            }
            schema.append("\n\n");

            // 2. Get all tables and columns
            String tableSql = "SELECT table_name FROM information_schema.tables WHERE table_schema = (SELECT DATABASE())";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(tableSql)) {
                
                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    schema.append("- ").append(tableName).append("(");
                    
                    String colSql = "SELECT column_name FROM information_schema.columns WHERE table_name = '" + tableName + "' AND table_schema = (SELECT DATABASE())";
                    try (Statement stmtCol = conn.createStatement();
                         ResultSet rsCol = stmtCol.executeQuery(colSql)) {
                        boolean first = true;
                        while (rsCol.next()) {
                            if (!first) schema.append(", ");
                            schema.append(rsCol.getString("column_name"));
                            first = false;
                        }
                    }
                    schema.append(")\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error fetching schema: " + e.getMessage();
        }
        return schema.toString() + HINTS;
    }

    public static String askQuestion(String question) {
        try {
            String currentSchema = getDynamicSchema();
            // 1. Ask Gemini to convert natural language to SQL
            String sqlPrompt = "You are a MySQL expert. Given this database schema:\n" + currentSchema +
                    "\nWrite a SQL query to answer this user question: '" + question + "'.\n" +
                    "IMPORTANT: Return ONLY the raw SQL query, no markdown, no explanation, no ```sql tags.";

            String generatedSql = callGeminiAPI(sqlPrompt).trim();
            generatedSql = generatedSql.replace("```sql", "").replace("```", "").trim();

            System.out.println("[Gemini] Generated SQL: " + generatedSql);

            // 2. Execute SQL
            String queryResults = executeQuery(generatedSql);
            System.out.println("[Gemini] Query Results: " + queryResults);

            // 3. Send results back to Gemini to formulate Arabic response
            String finalPrompt = "The admin asked: '" + question + "'.\n" +
                    "The database returned this data:\n" + queryResults + "\n" +
                    "Formulate a friendly, concise, and professional response in Arabic for the admin explaining this data. " +
                    "Do not mention the SQL query itself. " +
                    "IMPORTANT: Do NOT use any emojis in the response. Keep the text plain without any emoji characters.";

            return callGeminiAPI(finalPrompt);

        } catch (Exception e) {
            e.printStackTrace();
            return "حدث خطأ أثناء معالجة طلبك: " + e.getMessage();
        }
    }

    private static String executeQuery(String sql) throws Exception {
        if (!sql.toUpperCase().trim().startsWith("SELECT")) {
            throw new Exception("Only SELECT queries are allowed for safety.");
        }

        StringBuilder results = new StringBuilder();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Append Column Names
            for (int i = 1; i <= columnCount; i++) {
                results.append(metaData.getColumnName(i)).append("\t");
            }
            results.append("\n");

            // Append Data
            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
                for (int i = 1; i <= columnCount; i++) {
                    results.append(rs.getString(i)).append("\t");
                }
                results.append("\n");
                if (rowCount > 50) { 
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

        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);

        JsonArray partsArray = new JsonArray();
        partsArray.add(textPart);

        JsonObject contentObject = new JsonObject();
        contentObject.add("parts", partsArray);

        JsonArray contentsArray = new JsonArray();
        contentsArray.add(contentObject);

        JsonObject payload = new JsonObject();
        payload.add("contents", contentsArray);

        String jsonPayload = gson.toJson(payload);

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

    private static String extractTextFromJson(String json) {
        try {
            JsonObject root = gson.fromJson(json, JsonObject.class);
            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates != null && candidates.size() > 0) {
                JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
                if (content != null) {
                    JsonArray parts = content.getAsJsonArray("parts");
                    if (parts != null && parts.size() > 0) {
                        return parts.get(0).getAsJsonObject().get("text").getAsString();
                    }
                }
            }
            return "Could not extract response from JSON.";
        } catch (Exception e) {
            return "Parsing error: " + e.getMessage();
        }
    }
}
