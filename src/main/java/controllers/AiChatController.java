package controllers;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import utils.ai.GeminiService;

public class AiChatController {

    private ScrollPane chatScrollPane;
    private VBox chatContainer;
    private TextField messageInput;

    public Parent getView() {
        VBox root = new VBox();
        root.setPrefSize(450, 600);
        root.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #334257; -fx-padding: 15 20;");
        Label headerTitle = new Label("MediCore AI Assistant");
        headerTitle.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        header.getChildren().add(headerTitle);

        // Chat Area
        chatScrollPane = new ScrollPane();
        VBox.setVgrow(chatScrollPane, Priority.ALWAYS);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        chatContainer = new VBox(15);
        chatContainer.setPadding(new Insets(20));
        chatContainer.setStyle("-fx-background-color: white;");
        chatScrollPane.setContent(chatContainer);

        // Input Area
        HBox inputBar = new HBox(10);
        inputBar.setAlignment(Pos.CENTER);
        inputBar.setStyle("-fx-padding: 15; -fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-width: 1 0 0 0;");

        messageInput = new TextField();
        messageInput.setPromptText("اسأل أي شيء عن العيادة...");
        HBox.setHgrow(messageInput, Priority.ALWAYS);
        messageInput.setStyle("-fx-background-color: white; -fx-border-color: #D1D1D1; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 10 15; -fx-font-size: 14px;");
        messageInput.setOnAction(e -> handleSendMessage());

        Button sendBtn = new Button("إرسال");
        sendBtn.setOnAction(e -> handleSendMessage());
        sendBtn.setStyle("-fx-background-color: #548CA8; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 10 20; -fx-cursor: hand;");

        inputBar.getChildren().addAll(messageInput, sendBtn);
        root.getChildren().addAll(header, chatScrollPane, inputBar);

        initialize();
        return root;
    }

    public void initialize() {
        chatContainer.heightProperty().addListener((observable, oldValue, newValue) -> 
            chatScrollPane.setVvalue(1.0));
        
        addBotMessage("مرحباً! أنا المساعد الذكي. كيف يمكنني مساعدتك اليوم؟");
    }

    public void handleSendMessage() {
        String question = messageInput.getText().trim();
        if (question.isEmpty()) return;

        addUserMessage(question);
        messageInput.clear();
        messageInput.setDisable(true);

        Label loadingLabel = new Label("جاري التفكير...");
        loadingLabel.setStyle("-fx-text-fill: #888888; -fx-font-style: italic; -fx-padding: 10;");
        chatContainer.getChildren().add(loadingLabel);

        new Thread(() -> {
            String answer = GeminiService.askQuestion(question);
            Platform.runLater(() -> {
                chatContainer.getChildren().remove(loadingLabel);
                addBotMessage(answer);
                messageInput.setDisable(false);
                messageInput.requestFocus();
            });
        }).start();
    }

    private void addUserMessage(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-background-color: #548CA8; -fx-text-fill: white; -fx-padding: 12 16; -fx-background-radius: 15 15 0 15; -fx-font-size: 14px;");
        
        HBox hbox = new HBox(label);
        hbox.setAlignment(Pos.CENTER_RIGHT);
        hbox.setPadding(new Insets(5, 10, 5, 50));
        chatContainer.getChildren().add(hbox);
    }

    private void addBotMessage(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-background-color: #F8F9FE; -fx-text-fill: #334257; -fx-padding: 12 16; -fx-background-radius: 15 15 15 0; -fx-border-color: #D1D1D1; -fx-border-radius: 15 15 15 0; -fx-font-size: 14px;");
        
        HBox hbox = new HBox(label);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setPadding(new Insets(5, 50, 5, 10));
        chatContainer.getChildren().add(hbox);
    }
}
