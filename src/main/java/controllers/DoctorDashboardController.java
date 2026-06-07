package controllers;

import dao.DatabaseConnection;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import utils.AlertHelper;
import utils.SceneManager;
import utils.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.Period;

public class DoctorDashboardController {

    private BorderPane rootPane;
    private VBox patientInfoBox;
    private VBox treatmentBox;
    private VBox emptyStateBox;
    
    private Label nameLabel;
    private VBox ageLabel;
    private VBox phoneLabel;
    private VBox criticalityLabel;
    
    private TextArea diagnosisArea;

    // Multi-medicine prescription support
    public static class PrescriptionEntry {
        public String medicine, dosage, frequency, instructions;
        public int days;
        public PrescriptionEntry(String medicine, String dosage, String frequency, int days, String instructions) {
            this.medicine = medicine; this.dosage = dosage; this.frequency = frequency; this.days = days; this.instructions = instructions;
        }
        public String getMedicine() { return medicine; }
        public String getDosage() { return dosage; }
        public String getFrequency() { return frequency; }
        public int getDays() { return days; }
        public String getInstructions() { return instructions; }
    }
    private ObservableList<PrescriptionEntry> prescriptionList = FXCollections.observableArrayList();
    private TableView<PrescriptionEntry> prescriptionTable;
    private TextField medicineField, dosageField, frequencyField, durationField;
    private TextArea instructionsArea;

    private int currentAppointmentId = -1;
    private Label waitListLabel;

    public Parent getView() {
        rootPane = new BorderPane();
        rootPane.setStyle("-fx-background-color: #EEEEEE;");

        // Top Bar
        HBox topBar = new HBox();
        topBar.setStyle("-fx-background-color: white; -fx-padding: 15 30; -fx-alignment: CENTER_LEFT; -fx-border-color: #D1D1D1; -fx-border-width: 0 0 1 0;");
        Label title = new Label("Doctor Workspace");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: #334257;");
        HBox.setHgrow(title, Priority.ALWAYS);
        title.setMaxWidth(Double.MAX_VALUE);
        
        Button signOut = new Button("Sign Out");
        signOut.setOnAction(e -> handleLogout());
        signOut.setStyle("-fx-background-color: white; -fx-text-fill: #548CA8; -fx-padding: 10 20; -fx-background-radius: 10; -fx-cursor: hand; -fx-border-color: #548CA8; -fx-border-width: 1.5; -fx-font-weight: 600;");
        
        topBar.getChildren().addAll(title, signOut);
        rootPane.setTop(topBar);

        // Sidebar
        VBox sidebar = new VBox(12);
        sidebar.setStyle("-fx-background-color: #334257; -fx-padding: 30 15; -fx-pref-width: 260;");
        Label sidebarTitle = new Label("MEDICORE");
        sidebarTitle.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: 800; -fx-padding: 0 0 10 5;");
        
        waitListLabel = new Label("Patients Waiting: 0");
        waitListLabel.setStyle("-fx-text-fill: #ADB5BD; -fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 0 0 20 5;");

        sidebar.getChildren().addAll(sidebarTitle, waitListLabel);
        sidebar.getChildren().add(createSidebarButton("👤  Current Patient", e -> showCurrentPatient(), true));
        sidebar.getChildren().add(createSidebarButton("📜  Patient History", e -> showHistory(), false));
        rootPane.setLeft(sidebar);

        // Center Content
        StackPane centerStack = new StackPane();
        centerStack.setPadding(new Insets(30));

        // Empty State
        emptyStateBox = new VBox(20);
        emptyStateBox.setAlignment(Pos.CENTER);
        emptyStateBox.setVisible(false);
        emptyStateBox.setManaged(false);
        
        StackPane emptyIconStack = new StackPane();
        Circle emptyCircle = new Circle(70);
        emptyCircle.setStyle("-fx-fill: #548CA8; -fx-opacity: 0.07;");
        Label emptyIcon = new Label("🏥");
        emptyIcon.setStyle("-fx-font-size: 64px;");
        emptyIconStack.getChildren().addAll(emptyCircle, emptyIcon);
        
        Label emptyTitle = new Label("No Patients Waiting");
        emptyTitle.setStyle("-fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: #334257;");
        Label emptyDesc = new Label("Waiting for new patients to be assigned to you...");
        emptyDesc.setStyle("-fx-font-size: 14px; -fx-text-fill: #6C757D;");
        emptyStateBox.getChildren().addAll(emptyIconStack, emptyTitle, emptyDesc);

        // Active Workspace
        HBox workspace = new HBox(30);
        
        // Left: Patient Info
        patientInfoBox = new VBox(20);
        patientInfoBox.setMinWidth(300);
        patientInfoBox.setMaxWidth(300);
        patientInfoBox.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 25; -fx-border-color: #D1D1D1; -fx-border-width: 1;");
        
        VBox patientHeader = new VBox(10);
        patientHeader.setAlignment(Pos.CENTER);
        StackPane iconStack = new StackPane();
        Circle iconCircle = new Circle(40);
        iconCircle.setStyle("-fx-fill: #548CA8; -fx-opacity: 0.1;");
        Label iconLabel = new Label("👤");
        iconLabel.setStyle("-fx-font-size: 32px;");
        iconStack.getChildren().addAll(iconCircle, iconLabel);
        nameLabel = new Label("Patient Name");
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #334257;");
        patientHeader.getChildren().addAll(iconStack, nameLabel);

        VBox detailsBox = new VBox(15);
        ageLabel = createInfoLabel("Age", "-");
        phoneLabel = createInfoLabel("Phone Number", "-");
        criticalityLabel = createInfoLabel("Priority Level", "-");
        criticalityLabel.lookup(".value-label").setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #E71D36;");
        
        detailsBox.getChildren().addAll(new Separator(), ageLabel, phoneLabel, criticalityLabel);
        patientInfoBox.getChildren().addAll(patientHeader, detailsBox);

        // Right: Treatment
        treatmentBox = new VBox(20);
        HBox.setHgrow(treatmentBox, Priority.ALWAYS);
        treatmentBox.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 25; -fx-border-color: #D1D1D1; -fx-border-width: 1;");
        
        Label treatmentTitle = new Label("Diagnosis & Clinical Notes");
        treatmentTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #334257;");
        
        diagnosisArea = new TextArea();
        diagnosisArea.setPromptText("Describe diagnosis...");
        diagnosisArea.setWrapText(true);
        VBox.setVgrow(diagnosisArea, Priority.ALWAYS);
        diagnosisArea.setStyle("-fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8; -fx-padding: 10;");

        Label prescTitle = new Label("Prescription Details");
        prescTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #334257;");

        // Add medicine form
        GridPane prescGrid = new GridPane();
        prescGrid.setHgap(10);
        prescGrid.setVgap(10);
        ColumnConstraints cg1 = new ColumnConstraints(); cg1.setHgrow(Priority.ALWAYS);
        ColumnConstraints cg2 = new ColumnConstraints(); cg2.setPrefWidth(100);
        ColumnConstraints cg3 = new ColumnConstraints(); cg3.setPrefWidth(120);
        ColumnConstraints cg4 = new ColumnConstraints(); cg4.setPrefWidth(60);
        prescGrid.getColumnConstraints().addAll(cg1, cg2, cg3, cg4);

        medicineField = createStyledTextField("Medicine name...", "Medicine");
        dosageField = createStyledTextField("500mg", "Dosage");
        frequencyField = createStyledTextField("2x daily", "Frequency");
        durationField = createStyledTextField("7", "Days");

        prescGrid.add(createFormField("Medicine", medicineField), 0, 0);
        prescGrid.add(createFormField("Dosage", dosageField), 1, 0);
        prescGrid.add(createFormField("Frequency", frequencyField), 2, 0);
        prescGrid.add(createFormField("Days", durationField), 3, 0);

        HBox addMedRow = new HBox(10);
        addMedRow.setAlignment(Pos.CENTER_LEFT);
        instructionsArea = new TextArea();
        instructionsArea.setPromptText("Instructions...");
        instructionsArea.setPrefHeight(40);
        instructionsArea.setMaxHeight(50);
        instructionsArea.setWrapText(true);
        instructionsArea.setStyle("-fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8; -fx-padding: 5;");
        HBox.setHgrow(instructionsArea, Priority.ALWAYS);

        Button addMedBtn = new Button("+ Add");
        addMedBtn.setStyle("-fx-background-color: #2A9D8F; -fx-text-fill: white; -fx-font-weight: 700; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        addMedBtn.setOnAction(e -> addMedicineToPrescription());
        addMedRow.getChildren().addAll(instructionsArea, addMedBtn);

        // Prescription table
        prescriptionTable = new TableView<>();
        prescriptionTable.setPrefHeight(150);
        prescriptionTable.setStyle("-fx-background-color: white; -fx-border-color: #D1D1D1;");
        prescriptionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        prescriptionTable.setPlaceholder(new Label("No medicines added yet"));

        TableColumn<PrescriptionEntry, String> tMedCol = new TableColumn<>("Medicine");
        tMedCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMedicine()));
        TableColumn<PrescriptionEntry, String> tDoseCol = new TableColumn<>("Dosage");
        tDoseCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDosage()));
        TableColumn<PrescriptionEntry, String> tFreqCol = new TableColumn<>("Frequency");
        tFreqCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFrequency()));
        TableColumn<PrescriptionEntry, Integer> tDaysCol = new TableColumn<>("Days");
        tDaysCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getDays()).asObject());
        TableColumn<PrescriptionEntry, String> tInstCol = new TableColumn<>("Instructions");
        tInstCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getInstructions()));
        TableColumn<PrescriptionEntry, Void> tDelCol = new TableColumn<>("");
        tDelCol.setPrefWidth(50);
        tDelCol.setCellFactory(param -> new TableCell<>() {
            private final Button delBtn = new Button("✕");
            { delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #E71D36; -fx-font-weight: 800; -fx-cursor: hand;");
              delBtn.setOnAction(e -> { prescriptionList.remove(getIndex()); }); }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty); setGraphic(empty ? null : delBtn); }
        });

        prescriptionTable.getColumns().addAll(tMedCol, tDoseCol, tFreqCol, tDaysCol, tInstCol, tDelCol);
        prescriptionTable.setItems(prescriptionList);

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button clearBtn = new Button("Clear");
        clearBtn.setOnAction(e -> clearForm());
        clearBtn.setStyle("-fx-background-color: white; -fx-text-fill: #476072; -fx-border-color: #D1D1D1; -fx-border-width: 1.5; -fx-padding: 12 25; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-weight: 600;");
        
        Button completeBtn = new Button("Complete Appointment");
        completeBtn.setOnAction(e -> handleComplete());
        completeBtn.setStyle("-fx-background-color: #548CA8; -fx-text-fill: white; -fx-font-weight: 800; -fx-padding: 12 25; -fx-background-radius: 10; -fx-cursor: hand;");
        
        actions.getChildren().addAll(clearBtn, completeBtn);
        treatmentBox.getChildren().addAll(treatmentTitle, diagnosisArea, new Separator(), prescTitle, prescGrid, addMedRow, prescriptionTable, actions);

        workspace.getChildren().addAll(patientInfoBox, treatmentBox);
        centerStack.getChildren().addAll(emptyStateBox, workspace);
        rootPane.setCenter(centerStack);

        initialize();
        return rootPane;
    }

    private Button createSidebarButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler, boolean active) {
        Button btn = new Button(text);
        btn.setOnAction(handler);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setMaxWidth(240);
        btn.setPrefWidth(240);
        btn.setCursor(javafx.scene.Cursor.HAND);
        if (active) {
            btn.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: 600; -fx-padding: 12 25; -fx-background-radius: 4;");
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ADB5BD; -fx-font-size: 15px; -fx-font-weight: 500; -fx-padding: 12 25;");
        }
        return btn;
    }

    private VBox createInfoLabel(String label, String value) {
        VBox v = new VBox(5);
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: #476072; -fx-font-weight: 600;");
        Label val = new Label(value);
        val.getStyleClass().add("value-label");
        val.setStyle("-fx-font-size: 15px; -fx-font-weight: 600;");
        v.getChildren().addAll(l, val);
        return v;
    }

    private TextField createStyledTextField(String prompt, String label) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8; -fx-padding: 10;");
        return tf;
    }

    private VBox createFormField(String labelText, javafx.scene.Node field) {
        VBox v = new VBox(8);
        Label l = new Label(labelText);
        l.setStyle("-fx-text-fill: #476072; -fx-font-weight: 600;");
        v.getChildren().addAll(l, field);
        return v;
    }

    public void initialize() {
        if (!"doctor".equals(Session.getInstance().getRole())) {
            return;
        }
        loadHighestPriorityPatient();
        updateWaitListCount();
        
        Thread poller = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10000);
                    if (Session.getInstance().getDoctorId() == null) {
                        break;
                    }
                    Platform.runLater(() -> {
                        if (Session.getInstance().getDoctorId() == null) return;
                        if (currentAppointmentId == -1) loadHighestPriorityPatient();
                        updateWaitListCount();
                    });
                } catch (InterruptedException e) { break; }
            }
        });
        poller.setDaemon(true);
        poller.start();
    }

    private void loadHighestPriorityPatient() {
        Integer doctorId = Session.getInstance().getDoctorId();
        if (doctorId == null || doctorId < 0) return;
        int docId = doctorId;
        String sql = "SELECT a.id, p.id as p_id, p.full_name, p.phone, p.birth_date, a.priority " +
                     "FROM appointments a JOIN patients p ON a.patient_id = p.id " +
                     "WHERE a.doctor_id = ? AND a.status = 'waiting' " +
                     "ORDER BY a.priority DESC, a.appointment_date ASC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, docId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    currentAppointmentId = rs.getInt("id");
                    
                    nameLabel.setText(rs.getString("full_name"));
                    phoneLabel.lookup(".value-label").setStyle(""); // Reset style if needed
                    ((Label)phoneLabel.lookup(".value-label")).setText(rs.getString("phone"));
                    ((Label)criticalityLabel.lookup(".value-label")).setText(String.valueOf(rs.getInt("priority")));
                    
                    String bdateStr = rs.getString("birth_date");
                    if (bdateStr != null && !bdateStr.isEmpty()) {
                        try {
                            String normalized = bdateStr
                                .replace('٠','0').replace('١','1').replace('٢','2').replace('٣','3')
                                .replace('٤','4').replace('٥','5').replace('٦','6').replace('٧','7')
                                .replace('٨','8').replace('٩','9');
                            LocalDate bdate = LocalDate.parse(normalized);
                            int age = Period.between(bdate, LocalDate.now()).getYears();
                            ((Label)ageLabel.lookup(".value-label")).setText(String.valueOf(age));
                        } catch (Exception ex) {
                            ((Label)ageLabel.lookup(".value-label")).setText("Unknown");
                        }
                    } else {
                        ((Label)ageLabel.lookup(".value-label")).setText("Unknown");
                    }
                    
                    patientInfoBox.setVisible(true);
                    patientInfoBox.setManaged(true);
                    treatmentBox.setVisible(true);
                    treatmentBox.setManaged(true);
                    emptyStateBox.setVisible(false);
                    emptyStateBox.setManaged(false);
                } else {
                    currentAppointmentId = -1;
                    patientInfoBox.setVisible(false);
                    patientInfoBox.setManaged(false);
                    treatmentBox.setVisible(false);
                    treatmentBox.setManaged(false);
                    emptyStateBox.setVisible(true);
                    emptyStateBox.setManaged(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateWaitListCount() {
        Integer doctorId = Session.getInstance().getDoctorId();
        if (doctorId == null || doctorId < 0) return;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM appointments WHERE doctor_id = ? AND status = 'waiting'")) {
            ps.setInt(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    if (waitListLabel != null) waitListLabel.setText("Patients Waiting: " + count);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void addMedicineToPrescription() {
        String med = medicineField.getText().trim();
        if (med.isEmpty()) {
            AlertHelper.showError("Missing Medicine", "Please enter a medicine name.");
            return;
        }
        int days = 0;
        try { days = Integer.parseInt(durationField.getText().trim()); } catch (Exception ignored) {}
        prescriptionList.add(new PrescriptionEntry(
            med,
            dosageField.getText().trim(),
            frequencyField.getText().trim(),
            days,
            instructionsArea.getText().trim()
        ));
        medicineField.clear();
        dosageField.clear();
        frequencyField.clear();
        durationField.clear();
        instructionsArea.clear();
    }

    public void handleComplete() {
        if (currentAppointmentId == -1) return;
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps1 = conn.prepareStatement("UPDATE appointments SET status = 'completed' WHERE id = ?")) {
                    ps1.setInt(1, currentAppointmentId);
                    ps1.executeUpdate();
                }
                
                int recordId = -1;
                try (PreparedStatement ps2 = conn.prepareStatement("INSERT INTO medical_records (appointment_id, doctor_id, diagnosis) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps2.setInt(1, currentAppointmentId);
                    ps2.setInt(2, Session.getInstance().getDoctorId());
                    ps2.setString(3, diagnosisArea.getText());
                    ps2.executeUpdate();
                    try (ResultSet rs = ps2.getGeneratedKeys()) {
                        if (rs.next()) recordId = rs.getInt(1);
                    }
                }
                
                // Save ALL medicines in the prescription list
                if (recordId != -1 && !prescriptionList.isEmpty()) {
                    try (PreparedStatement ps3 = conn.prepareStatement(
                        "INSERT INTO prescriptions (medical_record_id, medicine_name, dosage, frequency, duration_days, instructions) VALUES (?, ?, ?, ?, ?, ?)")) {
                        for (PrescriptionEntry entry : prescriptionList) {
                            ps3.setInt(1, recordId);
                            ps3.setString(2, entry.getMedicine());
                            ps3.setString(3, entry.getDosage());
                            ps3.setString(4, entry.getFrequency());
                            ps3.setInt(5, entry.getDays());
                            ps3.setString(6, entry.getInstructions());
                            ps3.addBatch();
                        }
                        ps3.executeBatch();
                    }
                }
                
                conn.commit();
                AlertHelper.showSuccess("Visit Completed", "Patient record and " + prescriptionList.size() + " prescription(s) saved successfully.");
                clearForm();
                loadHighestPriorityPatient();
                updateWaitListCount();
                // Notify pharmacy
                try { socket.NotificationClient.sendMessage("REFRESH_PRESCRIPTIONS"); } catch (Exception ignored) {}
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertHelper.showError("Save Error", "Failed to save visit record:\n" + e.getMessage());
        }
    }

    private void clearForm() {
        diagnosisArea.clear();
        medicineField.clear();
        dosageField.clear();
        frequencyField.clear();
        durationField.clear();
        instructionsArea.clear();
        prescriptionList.clear();
    }

    public void showCurrentPatient() {
        // already here
    }

    public void showHistory() {
        SceneManager.switchTo((Stage) rootPane.getScene().getWindow(), new DoctorHistoryController().getView());
    }

    public void handleLogout() {
        Session.getInstance().clear();
        SceneManager.switchTo((Stage) rootPane.getScene().getWindow(), new LoginController().getView());
    }
}
