package app;

import javafx.application.Application;
import javafx.stage.Stage;
import socket.NotificationServer;
import socket.NotificationClient;
import utils.SceneManager;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Start socket server in background
        NotificationServer server = new NotificationServer();
        server.setDaemon(true);
        server.start();

        // Start socket client in background
        NotificationClient client = new NotificationClient();
        client.setDaemon(true);
        client.start();

        // Start Telegram Bot in background
        Thread telegramBot = new Thread(() -> {
            try {
                System.out.println("Starting MediCore Telegram Bot...");
                String pythonExe = "src/main/resources/telegram/venv/Scripts/python.exe".replace('/', java.io.File.separatorChar);
                String botPy = "src/main/resources/telegram/telegram_bot.py".replace('/', java.io.File.separatorChar);
                
                java.io.File exeFile = new java.io.File(pythonExe);
                if (!exeFile.exists()) {
                    System.err.println("[Telegram Bot] Venv python not found at: " + exeFile.getAbsolutePath() + ". Skipping auto-start.");
                    return;
                }
                
                ProcessBuilder pb = new ProcessBuilder(pythonExe, botPy);
                pb.inheritIO();
                Process process = pb.start();
                Runtime.getRuntime().addShutdownHook(new Thread(process::destroyForcibly));
                System.out.println("[Telegram Bot] Auto-started successfully.");
            } catch (Exception e) {
                System.err.println("[Telegram Bot] Failed to auto-start: " + e.getMessage());
            }
        });
        telegramBot.setDaemon(true);
        telegramBot.start();
        // Start n8n and AI Bridge are now handled externally by run_full.bat to prevent resource conflicts and console hangs.

        // Initialize login screen
        primaryStage.setTitle("MediCore - Hospital & Pharmacy Management System");
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.setOnCloseRequest(e -> {
            System.out.println("Shutting down MediCore...");
            System.exit(0);
        });

        SceneManager.switchTo(primaryStage, new controllers.LoginController().getView());
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}