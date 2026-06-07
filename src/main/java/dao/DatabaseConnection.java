package dao;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseConnection {
    private static final Properties props = new Properties();

    static {
        try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream("db_config.properties")) {
            if (input == null) {
                System.err.println("Sorry, unable to find db_config.properties");
            } else {
                props.load(input);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        initializeDatabase();
    }

    private DatabaseConnection() {}
    
    public static String getProperty(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public static Connection getConnection() {
        try {
            String url = props.getProperty("db.url");
            if (url == null) {
                throw new SQLException("Database URL not found in config.");
            }
            if (!url.contains("connectTimeout")) {
                url += (url.contains("?") ? "&" : "?") + "connectTimeout=3000";
            }
            String user = props.getProperty("db.user");
            String pass = props.getProperty("db.password");
            Connection conn = DriverManager.getConnection(url, user, pass);
            return conn;
        } catch (SQLException e) {
            System.err.println("Could not connect to MySQL: " + e.getMessage());
            try {
                javafx.application.Platform.runLater(() -> 
                    utils.AlertHelper.showError("Database Connection Error", 
                        "The system is disconnected from the database. Please check your MySQL server.\nError: " + e.getMessage()));
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static void initializeDatabase() {
        try {
            System.out.println("Initializing MySQL database from schema.sql...");
            String schemaSql = new String(Files.readAllBytes(Paths.get("database/schema.sql")));
            String[] commands = schemaSql.split(";");
            try (Connection conn = getConnection()) {
                if (conn == null) return;
                try (Statement stmt = conn.createStatement()) {
                    for (String command : commands) {
                        String trimmed = command.trim();
                        if (!trimmed.isEmpty()) {
                            try { stmt.execute(trimmed); } catch (Exception ex) {}
                        }
                    }
                    
                    // Patches for existing databases
                    try { stmt.execute("ALTER TABLE patients ADD COLUMN telegram_chat_id BIGINT"); } catch (SQLException ignored) {}
                    try { stmt.execute("ALTER TABLE medicines ADD COLUMN supplier VARCHAR(255)"); } catch (SQLException ignored) {}
                    
                    // Nurse table migration patches
                    try { stmt.execute("ALTER TABLE nurses ADD COLUMN full_name VARCHAR(255) NOT NULL AFTER id"); } catch (SQLException ignored) {}
                    try { stmt.execute("ALTER TABLE nurses ADD COLUMN email VARCHAR(255) UNIQUE AFTER full_name"); } catch (SQLException ignored) {}
                    try { stmt.execute("ALTER TABLE nurses ADD COLUMN phone VARCHAR(20) UNIQUE AFTER email"); } catch (SQLException ignored) {}
                    try { stmt.execute("ALTER TABLE nurses ADD COLUMN ssn VARCHAR(20) UNIQUE AFTER phone"); } catch (SQLException ignored) {}
                    try { stmt.execute("ALTER TABLE nurses ADD COLUMN birth_date DATE NOT NULL DEFAULT '2000-01-01' AFTER ssn"); } catch (SQLException ignored) {}
                    try { stmt.execute("ALTER TABLE nurses ADD COLUMN is_active TINYINT(1) DEFAULT 1 AFTER is_available"); } catch (SQLException ignored) {}
                    try { stmt.execute("ALTER TABLE nurses DROP FOREIGN KEY nurses_ibfk_1"); } catch (SQLException ignored) {}
                    try { stmt.execute("ALTER TABLE nurses DROP COLUMN user_id"); } catch (SQLException ignored) {}
                    // Ensure phone and ssn are NOT NULL if they were added as nullable
                    try { stmt.execute("ALTER TABLE nurses MODIFY phone VARCHAR(20) NOT NULL"); } catch (SQLException ignored) {}
                    try { stmt.execute("ALTER TABLE nurses MODIFY ssn VARCHAR(20) NOT NULL"); } catch (SQLException ignored) {}
                
                    System.out.println("Database initialized successfully with MySQL.");
                }
            }
        } catch (Exception e) {
            System.out.println("Schema init note: " + e.getMessage());
        }
    }
}
