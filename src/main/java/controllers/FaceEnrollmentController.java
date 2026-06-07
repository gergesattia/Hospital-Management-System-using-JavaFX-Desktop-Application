package controllers;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import models.User;
import utils.CameraManager;
import utils.ai.FaceEngine;
import javafx.application.Platform;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FaceEnrollmentController {

    private Label nameLabel;
    private ImageView cameraPreview;
    private ProgressBar progressBar;
    private Label statusLabel;
    private Button captureBtn;

    private CameraManager cameraManager;
    private FaceEngine faceEngine;
    private User targetUser;
    private int captureCount = 0;
    private static final int REQUIRED_SAMPLES = 5;
    private List<Image> capturedFrames = new ArrayList<>();

    public void setUser(User user) {
        this.targetUser = user;
        if (nameLabel != null) {
            this.nameLabel.setText(user.getFullName() + " (" + user.getRole() + ")");
        }
    }

    public Parent getView() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPrefSize(500, 600);
        root.setStyle("-fx-background-color: white; -fx-padding: 30;");

        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER);
        Label title = new Label("Staff Biometric Enrollment");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #334257;");
        nameLabel = new Label("User Name");
        nameLabel.setStyle("-fx-text-fill: #548CA8; -fx-font-weight: 600;");
        if (targetUser != null) {
            nameLabel.setText(targetUser.getFullName() + " (" + targetUser.getRole() + ")");
        }
        header.getChildren().addAll(title, nameLabel);

        StackPane camStack = new StackPane();
        camStack.setStyle("-fx-background-color: #F8F9FE; -fx-background-radius: 15; -fx-border-color: #D1D1D1; -fx-border-width: 1;");
        cameraPreview = new ImageView();
        cameraPreview.setFitWidth(400);
        cameraPreview.setFitHeight(300);
        cameraPreview.setPreserveRatio(true);
        camStack.getChildren().add(cameraPreview);

        VBox progressBox = new VBox(10);
        progressBox.setAlignment(Pos.CENTER);
        progressBar = new ProgressBar(0.0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: #548CA8;");
        statusLabel = new Label("Position face and click Start Capture");
        statusLabel.setStyle("-fx-text-fill: #476072;");
        progressBox.getChildren().addAll(progressBar, statusLabel);

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER);
        captureBtn = new Button("Start Capture");
        captureBtn.setOnAction(e -> handleStartCapture());
        captureBtn.setPrefWidth(150);
        captureBtn.setStyle("-fx-background-color: #548CA8; -fx-text-fill: white; -fx-font-weight: 700; -fx-padding: 12; -fx-background-radius: 8; -fx-cursor: hand;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction(e -> handleCancel());
        cancelBtn.setPrefWidth(120);
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6C757D; -fx-border-color: #6C757D; -fx-border-width: 1; -fx-padding: 10; -fx-background-radius: 8; -fx-cursor: hand;");
        
        actions.getChildren().addAll(captureBtn, cancelBtn);
        root.getChildren().addAll(header, camStack, progressBox, actions);

        initialize();
        return root;
    }

    public void initialize() {
        cameraManager = new CameraManager();
        cameraManager.startCamera(cameraPreview);
        
        String modelPath = "src/main/resources/ai/facenet512.onnx";
        if (new File(modelPath).exists()) {
            faceEngine = new FaceEngine(modelPath);
        }
    }

    public void handleStartCapture() {
        if (faceEngine == null) {
            faceEngine = new FaceEngine();
        }

        captureBtn.setDisable(true);
        statusLabel.setText("Capturing... Stay still.");
        
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            if (captureCount < REQUIRED_SAMPLES) {
                Image frame = cameraManager.grabCurrentFrame();
                if (frame != null) {
                    capturedFrames.add(frame);
                    captureCount++;
                    
                    Platform.runLater(() -> {
                        progressBar.setProgress((double) captureCount / REQUIRED_SAMPLES);
                        statusLabel.setText("Sample " + captureCount + " of " + REQUIRED_SAMPLES + " captured.");
                    });
                }
            } else {
                executor.shutdown();
                saveAndExit();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void saveAndExit() {
        boolean success = faceEngine.enroll(targetUser.getUsername(), capturedFrames);
        
        Platform.runLater(() -> {
            if (success) {
                statusLabel.setText("Enrollment Complete! You can close this window.");
                utils.AlertHelper.showSuccess("Success", "Biometric data for " + targetUser.getFullName() + " registered successfully.");
            } else {
                statusLabel.setText("Enrollment Failed. Please check AI Service.");
                utils.AlertHelper.showError("Enrollment Error", "Failed to register biometric data via AI Bridge.");
            }
            cameraManager.stopCamera();
            if (cameraPreview.getScene() != null) {
                ((Stage) cameraPreview.getScene().getWindow()).close();
            }
        });
    }

    public void handleCancel() {
        cameraManager.stopCamera();
        if (cameraPreview.getScene() != null) {
            ((Stage) cameraPreview.getScene().getWindow()).close();
        }
    }
}
