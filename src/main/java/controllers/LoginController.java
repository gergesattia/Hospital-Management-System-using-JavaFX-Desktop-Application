package controllers;

import dao.UserDAO;
import dao.DoctorDAO;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import models.User;
import utils.*;
import utils.ai.FaceEngine;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import utils.ai.VerifyResult;

public class LoginController {

    private VBox loginForm;
    private TextField usernameField;
    private PasswordField passwordField;
    private Label errorLabel;

    private VBox biometricView;
    private ImageView cameraPreview;
    private Label biometricStatusLabel;

    private CameraManager cameraManager;
    private FaceEngine faceEngine;
    private ScheduledExecutorService aiExecutor;
    
    private User pendingUser;
    private boolean isProcessingLogin = false;
    
    // Security Logic
    private static long blockUntil = 0;
    private long scanStartTime = 0;
    private long lastAlertTime = 0;
    private int nullFrameCount = 0;
    private static final int MAX_NULL_FRAMES = 5;
    private static final long SCAN_DURATION = 30000; // 30 seconds
    private static final long BLOCK_DURATION = 60000; // 1 minute

    public Parent getView() {
        HBox root = new HBox();
        root.setPrefSize(1280, 800);
        root.setStyle("-fx-background-color: #EEEEEE;");

        // Left Branding Section
        VBox branding = new VBox(20);
        branding.setAlignment(Pos.CENTER);
        branding.setPadding(new Insets(40));
        branding.setStyle("-fx-background-color: #334257;");
        HBox.setHgrow(branding, Priority.ALWAYS);

        StackPane logoStack = new StackPane();
        Circle logoCircle = new Circle(50, Color.web("#548CA8"));
        logoCircle.setOpacity(0.2);
        Label logoIcon = new Label("🏥");
        logoIcon.setStyle("-fx-font-size: 48px;");
        logoStack.getChildren().addAll(logoCircle, logoIcon);

        Label brandName = new Label("MediCore");
        brandName.setStyle("-fx-text-fill: white; -fx-font-size: 36px; -fx-font-weight: 800;");

        Label tagLine = new Label("The heartbeat of your hospital management");
        tagLine.setStyle("-fx-text-fill: #ADB5BD; -fx-font-size: 16px; -fx-text-alignment: center;");
        tagLine.setWrapText(true);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label footer = new Label("© 2026 MediCore Systems");
        footer.setStyle("-fx-text-fill: #6C757D; -fx-font-size: 12px;");

        branding.getChildren().addAll(logoStack, brandName, tagLine, spacer, footer);

        // Right Login Section
        VBox rightSection = new VBox();
        rightSection.setAlignment(Pos.CENTER);
        rightSection.setPadding(new Insets(50, 80, 50, 80));
        HBox.setHgrow(rightSection, Priority.ALWAYS);

        StackPane formsStack = new StackPane();

        // Login Form
        loginForm = new VBox(30);
        loginForm.setMaxWidth(400);
        loginForm.setAlignment(Pos.CENTER);
        loginForm.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 40;");
        
        VBox welcomeBox = new VBox(5);
        Label welcome = new Label("Welcome Back");
        welcome.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: #334257;");
        Label subWelcome = new Label("Please enter your credentials to login");
        subWelcome.setStyle("-fx-text-fill: #476072; -fx-font-weight: 600;");
        welcomeBox.getChildren().addAll(welcome, subWelcome);

        VBox fieldsBox = new VBox(20);
        
        VBox userBox = new VBox(8);
        Label userLabel = new Label("Username");
        userLabel.setStyle("-fx-text-fill: #476072; -fx-font-weight: 600;");
        usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.setStyle("-fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8; -fx-padding: 12;");
        userBox.getChildren().addAll(userLabel, usernameField);

        VBox passBox = new VBox(8);
        Label passLabel = new Label("Password");
        passLabel.setStyle("-fx-text-fill: #476072; -fx-font-weight: 600;");
        passwordField = new PasswordField();
        passwordField.setPromptText("••••••••");
        passwordField.setStyle("-fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8; -fx-padding: 12;");
        passBox.getChildren().addAll(passLabel, passwordField);
        
        fieldsBox.getChildren().addAll(userBox, passBox);

        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #E71D36; -fx-font-weight: bold;");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);

        Button signInBtn = new Button("Sign In");
        signInBtn.setOnAction(e -> handleLogin());
        signInBtn.setMaxWidth(Double.MAX_VALUE);
        signInBtn.setStyle("-fx-background-color: #548CA8; -fx-text-fill: white; -fx-font-weight: 800; -fx-padding: 15; -fx-background-radius: 10; -fx-cursor: hand;");

        Hyperlink forgotLink = new Hyperlink("Forgot Password?");
        forgotLink.setStyle("-fx-text-fill: #548CA8; -fx-font-size: 13px; -fx-font-weight: 600; -fx-underline: false;");
        HBox forgotBox = new HBox(forgotLink);
        forgotBox.setAlignment(Pos.CENTER);

        loginForm.getChildren().addAll(welcomeBox, fieldsBox, errorLabel, signInBtn, forgotBox);

        // Biometric View
        biometricView = new VBox(25);
        biometricView.setVisible(false);
        biometricView.setAlignment(Pos.CENTER);
        biometricView.setMaxWidth(450);
        biometricView.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 40;");

        VBox bioHeader = new VBox(5);
        bioHeader.setAlignment(Pos.CENTER);
        Label bioTitle = new Label("Biometric Verification");
        bioTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: #334257;");
        biometricStatusLabel = new Label("Please look directly at the camera");
        biometricStatusLabel.setStyle("-fx-text-fill: #548CA8; -fx-font-weight: 600;");
        bioHeader.getChildren().addAll(bioTitle, biometricStatusLabel);

        StackPane camStack = new StackPane();
        camStack.setStyle("-fx-background-color: #F8F9FE; -fx-background-radius: 15; -fx-border-color: #D1D1D1; -fx-border-width: 1;");
        cameraPreview = new ImageView();
        cameraPreview.setFitWidth(320);
        cameraPreview.setFitHeight(240);
        cameraPreview.setPreserveRatio(true);
        camStack.getChildren().add(cameraPreview);

        HBox bioActions = new HBox(10);
        Button cancelBio = new Button("Cancel");
        cancelBio.setOnAction(e -> handleCancelBiometric());
        cancelBio.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cancelBio, Priority.ALWAYS);
        cancelBio.setStyle("-fx-background-color: transparent; -fx-text-fill: #E71D36; -fx-border-color: #E71D36; -fx-border-width: 1.5; -fx-padding: 12; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-weight: 700;");

        Button skipBio = new Button("Skip (Dev)");
        skipBio.setOnAction(e -> handleBypassBiometric());
        skipBio.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(skipBio, Priority.ALWAYS);
        skipBio.setStyle("-fx-background-color: #F8F9FE; -fx-text-fill: #548CA8; -fx-border-color: #D1D1D1; -fx-border-width: 1.5; -fx-padding: 12; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-weight: 700;");
        
        bioActions.getChildren().addAll(cancelBio, skipBio);
        biometricView.getChildren().addAll(bioHeader, camStack, bioActions);

        formsStack.getChildren().addAll(loginForm, biometricView);
        rightSection.getChildren().add(formsStack);

        root.getChildren().addAll(branding, rightSection);

        initialize();
        return root;
    }

    public void initialize() {
        errorLabel.setVisible(false);
        cameraManager = new CameraManager();
        
        // Check if already blocked from previous attempt
        if (System.currentTimeMillis() < blockUntil) {
            startBlockCountdown();
        }
        
        new Thread(() -> {
            String modelPath = "src/main/resources/ai/facenet512.onnx";
            if (new File(modelPath).exists()) {
                faceEngine = new FaceEngine(modelPath);
            }
        }).start();
    }

    public void handleLogin() {
        isProcessingLogin = false; // Reset flag on manual login attempt
        // Check for active block
        long now = System.currentTimeMillis();
        if (now < blockUntil) {
            long remaining = (blockUntil - now) / 1000;
            showError("System blocked. Try again in " + remaining + "s");
            return;
        }

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and password required.");
            return;
        }

        UserDAO userDAO = new UserDAO();
        User user = userDAO.login(username);

        if (user != null && PasswordUtil.check(password, user.getPasswordHash())) {
            if ("nurse".equals(user.getRole())) {
                showError("Nurses are not allowed to login.");
                return;
            }
            pendingUser = user;
            if ("admin".equals(user.getRole())) {
                startBiometricStep();
            } else {
                completeLogin(user);
            }
        } else {
            showError("Invalid username or password.");
            // Optional: send telegram for failed password attempts like the old system
            TelegramManager.sendMessage("⚠️ <b>Password Failed</b>\nUser: " + username + "\nTime: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
    }

    private void startBiometricStep() {
        if (faceEngine == null) faceEngine = new FaceEngine();
        
        scanStartTime = System.currentTimeMillis();
        lastAlertTime = 0;
        nullFrameCount = 0;
        
        loginForm.setVisible(false);
        biometricView.setVisible(true);
        cameraManager.startCamera(cameraPreview);
        
        biometricStatusLabel.setText("Please look directly at the camera");
        biometricStatusLabel.setStyle("-fx-text-fill: #548CA8;");
        
        aiExecutor = Executors.newSingleThreadScheduledExecutor();
        aiExecutor.scheduleAtFixedRate(this::processBiometricFrame, 1, 1, TimeUnit.SECONDS);
    }

    private void processBiometricFrame() {
        long now = System.currentTimeMillis();
        
        // 1. Check for Timeout
        if (now - scanStartTime > SCAN_DURATION) {
            handleBiometricTimeout();
            return;
        }

        Image frame = cameraManager.grabCurrentFrame();
        
        // 2. Camera unavailable — auto-skip after MAX_NULL_FRAMES consecutive failures
        if (frame == null) {
            nullFrameCount++;
            if (nullFrameCount >= MAX_NULL_FRAMES) {
                Platform.runLater(() -> {
                    System.out.println("[Biometric] Camera unavailable after " + MAX_NULL_FRAMES + " attempts. Skipping biometric.");
                    biometricStatusLabel.setText("⚠ Camera unavailable — skipping biometric");
                    biometricStatusLabel.setStyle("-fx-text-fill: #E9A820;");
                    stopBiometrics();
                    completeLogin(pendingUser);
                });
            } else {
                Platform.runLater(() -> {
                    biometricStatusLabel.setText("Waiting for camera... (" + nullFrameCount + "/" + MAX_NULL_FRAMES + ")");
                });
            }
            return;
        }
        
        nullFrameCount = 0; // Reset on successful frame
        
        if (faceEngine != null) {
            VerifyResult result = faceEngine.verify(pendingUser.getUsername(), frame);
            
            if (result.isMatch()) {
                Platform.runLater(() -> {
                    biometricStatusLabel.setText("✔ Identity Verified!");
                    biometricStatusLabel.setStyle("-fx-text-fill: #2A9D8F;");
                    stopBiometrics();
                    completeLogin(pendingUser);
                });
            } else {
                // Handle Mismatch
                handleMismatch(frame, result);
                Platform.runLater(() -> {
                    biometricStatusLabel.setText(result.getStatus().equals("error") ? "Scanning..." : "Face mismatch. Please try again.");
                    biometricStatusLabel.setStyle("-fx-text-fill: #E71D36;");
                });
            }
        }
    }

    private void handleBiometricTimeout() {
        Platform.runLater(() -> {
            biometricStatusLabel.setText("🚫 Verification Timeout. Blocked.");
            stopBiometrics();
            isProcessingLogin = false;
            
            blockUntil = System.currentTimeMillis() + BLOCK_DURATION;
            startBlockCountdown(); // Start the dynamic countdown
            
            // Log and Alert
            Image frame = cameraManager.grabCurrentFrame();
            File logFile = saveFailPhoto(frame, "timeout");
            
            String msg = "🚫 <b>ACCOUNT LOCKED</b>\n" +
                         "👤 <b>User:</b> " + pendingUser.getUsername() + "\n" +
                         "Reason: 30s mismatch threshold exceeded.\n" +
                         "Lock Duration: 1 minute.";
            
            if (logFile != null) {
                TelegramManager.sendPhoto(msg, logFile);
            } else {
                TelegramManager.sendMessage(msg);
            }
            
            // Switch back to login form to see the error
            biometricView.setVisible(false);
            loginForm.setVisible(true);
            passwordField.clear();
        });
    }

    private void startBlockCountdown() {
        ScheduledExecutorService countdownExecutor = Executors.newSingleThreadScheduledExecutor();
        countdownExecutor.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            if (now < blockUntil) {
                long remaining = (blockUntil - now) / 1000;
                Platform.runLater(() -> {
                    showError("System blocked. Try again in " + remaining + "s");
                });
            } else {
                Platform.runLater(() -> {
                    errorLabel.setVisible(false);
                });
                countdownExecutor.shutdown();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void handleMismatch(Image frame, VerifyResult result) {
        long now = System.currentTimeMillis();
        
        // Alert every 5 seconds of failure
        if (now - lastAlertTime > 5000) {
            lastAlertTime = now;
            File logFile = saveFailPhoto(frame, "mismatch");
            
            String msg = "🚨 <b>Unauthorized Access Attempt!</b>\n\n" +
                         "👤 <b>Account:</b> " + pendingUser.getFullName() + " (" + pendingUser.getUsername() + ")\n" +
                         "📊 <b>Distance:</b> " + String.format("%.4f", result.getDistance()) + "\n" +
                         "⏰ <b>Time:</b> " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            if (logFile != null) {
                TelegramManager.sendPhoto(msg, logFile);
            } else {
                TelegramManager.sendMessage(msg);
            }
        }
    }

    private File saveFailPhoto(Image frame, String type) {
        if (frame == null) return null;
        
        File dir = new File("logs/biometric_fails");
        if (!dir.exists()) dir.mkdirs();
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File file = new File(dir, "fail_" + pendingUser.getUsername() + "_" + type + "_" + timestamp + ".png");
        
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(frame, null), "png", file);
            return file;
        } catch (IOException e) {
            System.err.println("Failed to save fail photo: " + e.getMessage());
            return null;
        }
    }

    private void completeLogin(User user) {
        if (isProcessingLogin) return;
        isProcessingLogin = true;

        Session session = Session.getInstance();
        session.set(user.getId(), user.getFullName(), user.getRole());
        
        if (usernameField.getScene() == null) {
            System.err.println("Login aborted: Node detached from scene.");
            isProcessingLogin = false;
            return;
        }
        
        Stage stage = (Stage) usernameField.getScene().getWindow();
        
        if ("doctor".equals(user.getRole())) {
            int docId = new DoctorDAO().getDoctorIdByUserId(user.getId());
            session.setDoctorId(docId);
            SceneManager.switchTo(stage, new DoctorDashboardController().getView());
        } else if ("admin".equals(user.getRole())) {
            SceneManager.switchTo(stage, new AdminDashboardController().getView());
        } else if ("receptionist".equals(user.getRole())) {
            SceneManager.switchTo(stage, new ReceptionistDashboardController().getView());
        } else if ("pharmacist".equals(user.getRole())) {
            SceneManager.switchTo(stage, new PharmacyDashboardController().getView());
        } else {
            SceneManager.switchTo(stage, new PharmacyDashboardController().getView());
        }
    }

    private void stopBiometrics() {
        if (cameraManager != null) cameraManager.stopCamera();
        if (aiExecutor != null) aiExecutor.shutdown();
    }

    public void handleCancelBiometric() {
        stopBiometrics();
        isProcessingLogin = false;
        biometricView.setVisible(false);
        loginForm.setVisible(true);
        passwordField.clear();
    }

    private void handleBypassBiometric() {
        if (pendingUser != null) {
            stopBiometrics();
            completeLogin(pendingUser);
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
