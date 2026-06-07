package controllers;

import dao.DatabaseConnection;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import utils.SceneManager;
import utils.Session;
import utils.ValidationUtils;
import utils.AlertHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class ReceptionistDashboardController {

    public static class PatientModel {
        public int id;
        public String fullName;
        public String ssn;
        public String phone;
        public String email;
        public String gender;
        public String birthDate;
        public String address;
        public int priority;
        public String date;
        
        public String getFullName() { return fullName; }
        public String getSsn() { return ssn; }
        public String getPhone() { return phone; }
        public String getEmail() { return email; }
        public String getGender() { return gender; }
        public String getBirthDate() { return birthDate; }
        public String getAddress() { return address; }
        public int getPriority() { return priority; }
        public String getDate() { return date; }
    }

    private BorderPane rootPane;
    private TextField searchField;
    private TableView<PatientModel> patientsTable;

    private TableColumn<PatientModel, String> nameCol;
    private TableColumn<PatientModel, String> ssnCol;
    private TableColumn<PatientModel, String> phoneCol;
    private TableColumn<PatientModel, String> emailCol;
    private TableColumn<PatientModel, String> genderCol;
    private TableColumn<PatientModel, String> birthDateCol;
    private TableColumn<PatientModel, String> addressCol;
    private TableColumn<PatientModel, Integer> priorityCol;
    private TableColumn<PatientModel, String> dateCol;

    private TextField fullNameField;
    private TextField ssnField;
    private TextField phoneField;
    private TextField addressField;
    private TextField emailField;
    private TextField ageField;
    private ComboBox<String> genderBox;
    private ComboBox<String> specialtyBox;
    private ComboBox<String> priorityBox;
    private DatePicker appointmentDatePicker;

    private ObservableList<PatientModel> patientList = FXCollections.observableArrayList();

    public Parent getView() {
        rootPane = new BorderPane();
        rootPane.setStyle("-fx-background-color: #EEEEEE;");

        // Top Bar
        HBox topBar = new HBox();
        topBar.setStyle("-fx-background-color: white; -fx-padding: 15 30; -fx-alignment: CENTER_LEFT; -fx-border-color: #D1D1D1; -fx-border-width: 0 0 1 0;");
        Label title = new Label("Reception & Registration");
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
        sidebarTitle.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: 800; -fx-padding: 0 0 20 5;");
        
        Button regBtn = new Button("📝  Registration");
        regBtn.setOnAction(e -> showManagePatients());
        regBtn.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: 600; -fx-alignment: CENTER_LEFT; -fx-padding: 12 25; -fx-max-width: 240; -fx-pref-width: 240; -fx-background-radius: 4; -fx-cursor: hand;");
        
        Button presBtn = new Button("📋  Prescriptions");
        presBtn.setOnAction(e -> showPrescriptions());
        presBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ADB5BD; -fx-font-size: 15px; -fx-font-weight: 500; -fx-alignment: CENTER_LEFT; -fx-padding: 12 25; -fx-max-width: 240; -fx-pref-width: 240; -fx-cursor: hand;");
        
        sidebar.getChildren().addAll(sidebarTitle, regBtn, presBtn);
        rootPane.setLeft(sidebar);

        // Center Content
        HBox centerHBox = new HBox(30);
        centerHBox.setPadding(new Insets(30));

        // Left Side: Registration Form
        VBox form = new VBox(20);
        form.setMinWidth(400);
        form.setMaxWidth(400);
        form.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 25; -fx-border-color: #D1D1D1; -fx-border-width: 1;");
        
        VBox formHeader = new VBox(5);
        Label formTitle = new Label("New Registration");
        formTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #334257;");
        Label formSubtitle = new Label("Enter patient information below");
        formSubtitle.setStyle("-fx-text-fill: #476072; -fx-font-weight: 600;");
        formHeader.getChildren().addAll(formTitle, formSubtitle);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        
        VBox scrollContent = new VBox(15);
        fullNameField = createStyledTextField("Enter full name");
        ssnField = createStyledTextField("Enter ID number");
        phoneField = createStyledTextField("Enter phone");
        addressField = createStyledTextField("Enter address");
        emailField = createStyledTextField("patient@example.com");
        
        ageField = createStyledTextField("Enter age");
        
        genderBox = new ComboBox<>();
        genderBox.setMaxWidth(Double.MAX_VALUE);
        genderBox.setStyle("-fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 5;");
        
        specialtyBox = new ComboBox<>();
        specialtyBox.setMaxWidth(Double.MAX_VALUE);
        specialtyBox.setStyle("-fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 5;");
        
        priorityBox = new ComboBox<>();
        priorityBox.setMaxWidth(Double.MAX_VALUE);
        priorityBox.setStyle("-fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 5;");

        appointmentDatePicker = new DatePicker();
        appointmentDatePicker.setMaxWidth(Double.MAX_VALUE);
        appointmentDatePicker.setStyle("-fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 5;");

        scrollContent.getChildren().addAll(
            createFormField("Full Name", fullNameField),
            createFormField("National ID (SSN)", ssnField),
            createFormField("Phone Number", phoneField),
            createFormField("Address", addressField),
            createFormField("Email Address", emailField),
            new HBox(10, createFormField("Age", ageField), createFormField("Gender", genderBox)),
            createFormField("Department / Specialty", specialtyBox),
            createFormField("Priority Level", priorityBox),
            createFormField("Appointment Date", appointmentDatePicker)
        );
        scrollPane.setContent(scrollContent);

        Button addBtn = new Button("Add Patient to Queue");
        addBtn.setOnAction(e -> handleAdd());
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setStyle("-fx-background-color: #548CA8; -fx-text-fill: white; -fx-font-weight: 800; -fx-padding: 15; -fx-background-radius: 10; -fx-cursor: hand;");

        form.getChildren().addAll(formHeader, scrollPane, addBtn);

        // Right Side: Table View
        VBox tableSection = new VBox(15);
        HBox.setHgrow(tableSection, Priority.ALWAYS);
        tableSection.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 25; -fx-border-color: #D1D1D1; -fx-border-width: 1;");
        
        HBox tableHeader = new HBox(10);
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        Label tableTitle = new Label("Patient Queue Overview");
        tableTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #334257;");
        HBox.setHgrow(tableTitle, Priority.ALWAYS);
        tableTitle.setMaxWidth(Double.MAX_VALUE);
        
        searchField = new TextField();
        searchField.setPromptText("Search queue...");
        searchField.setPrefWidth(200);
        searchField.setStyle("-fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8;");
        
        Button refreshBtn = new Button("🔄");
        refreshBtn.setOnAction(e -> loadPatients());
        refreshBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #548CA8; -fx-text-fill: #548CA8; -fx-padding: 5 10; -fx-background-radius: 8; -fx-cursor: hand;");
        
        tableHeader.getChildren().addAll(tableTitle, searchField, refreshBtn);

        patientsTable = new TableView<>();
        VBox.setVgrow(patientsTable, Priority.ALWAYS);
        patientsTable.setStyle("-fx-background-color: white; -fx-border-color: transparent;");
        patientsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        nameCol = new TableColumn<>("Full Name");
        ssnCol = new TableColumn<>("National ID");
        phoneCol = new TableColumn<>("Phone");
        emailCol = new TableColumn<>("Email");
        genderCol = new TableColumn<>("Gender");
        birthDateCol = new TableColumn<>("Age");
        addressCol = new TableColumn<>("Address");
        priorityCol = new TableColumn<>("Priority");
        dateCol = new TableColumn<>("Date Added");

        patientsTable.getColumns().addAll(nameCol, ssnCol, phoneCol, emailCol, genderCol, birthDateCol, addressCol, priorityCol, dateCol);
        tableSection.getChildren().addAll(tableHeader, patientsTable);

        centerHBox.getChildren().addAll(form, tableSection);
        rootPane.setCenter(centerHBox);

        initialize();
        return rootPane;
    }

    private TextField createStyledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10;");
        return tf;
    }

    private VBox createFormField(String labelText, javafx.scene.Node field) {
        VBox v = new VBox(5);
        Label l = new Label(labelText);
        l.setStyle("-fx-text-fill: #476072; -fx-font-weight: 600;");
        v.getChildren().addAll(l, field);
        if (field instanceof Region) HBox.setHgrow(v, Priority.ALWAYS);
        return v;
    }

    public void initialize() {
        if (!"receptionist".equals(Session.getInstance().getRole())) return;

        genderBox.setItems(FXCollections.observableArrayList("Male", "Female"));
        priorityBox.setItems(FXCollections.observableArrayList("1 - Normal", "2 - Low", "3 - Medium", "4 - High", "5 - Critical"));
        
        loadSpecialties();

        nameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFullName()));
        ssnCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSsn()));
        phoneCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPhone()));
        emailCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEmail()));
        genderCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getGender()));
        birthDateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getBirthDate()));
        addressCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAddress()));
        priorityCol.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getPriority()).asObject());
        dateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDate()));

        loadPatients();
        setupSearch();

        Thread poller = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000); // Poll every 5 seconds
                    if (Session.getInstance().getRole() == null || !"receptionist".equals(Session.getInstance().getRole())) {
                        break;
                    }
                    javafx.application.Platform.runLater(() -> {
                        if ("receptionist".equals(Session.getInstance().getRole())) {
                            loadPatients();
                        }
                    });
                } catch (InterruptedException e) { break; }
            }
        });
        poller.setDaemon(true);
        poller.start();
    }


    private void loadSpecialties() {
        ObservableList<String> specs = FXCollections.observableArrayList();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DISTINCT s.name FROM specializations s JOIN doctors d ON s.id = d.specialization_id")) {
            while (rs.next()) specs.add(rs.getString("name"));
        } catch (Exception e) { e.printStackTrace(); }
        specialtyBox.setItems(specs);
    }

    private String calculateAge(String birthDateStr) {
        if (birthDateStr == null || birthDateStr.isEmpty()) return "Unknown";
        try {
            String normalized = birthDateStr
                .replace('٠','0').replace('١','1').replace('٢','2').replace('٣','3')
                .replace('٤','4').replace('٥','5').replace('٦','6').replace('٧','7')
                .replace('٨','8').replace('٩','9');
            java.time.LocalDate bdate = java.time.LocalDate.parse(normalized);
            int age = java.time.Period.between(bdate, java.time.LocalDate.now()).getYears();
            return String.valueOf(age);
        } catch (Exception ex) {
            return "Unknown";
        }
    }

    public void loadPatients() {
        patientList.clear();
        String sql = "SELECT p.id, p.full_name, p.national_id, p.phone, p.email, p.gender, p.birth_date, p.address, a.priority, a.appointment_date " +
                     "FROM patients p " +
                     "JOIN appointments a ON p.id = a.patient_id " +
                     "WHERE a.status = 'waiting'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                PatientModel p = new PatientModel();
                p.id = rs.getInt("id");
                p.fullName = rs.getString("full_name");
                p.ssn = rs.getString("national_id");
                p.phone = rs.getString("phone");
                p.email = rs.getString("email");
                p.gender = rs.getString("gender");
                p.birthDate = calculateAge(rs.getString("birth_date"));
                p.address = rs.getString("address");
                p.priority = rs.getInt("priority");
                p.date = rs.getString("appointment_date");
                patientList.add(p);
            }
        } catch (Exception e) { e.printStackTrace(); }
        patientsTable.setItems(patientList);
    }


    private void setupSearch() {
        FilteredList<PatientModel> filteredData = new FilteredList<>(patientList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(patient -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                if (patient.fullName.toLowerCase().contains(lowerCaseFilter)) return true;
                if (patient.ssn.toLowerCase().contains(lowerCaseFilter)) return true;
                return false;
            });
        });
        patientsTable.setItems(filteredData);
    }


    public void handleAdd() {
        if (fullNameField.getText().isEmpty() || specialtyBox.getValue() == null || priorityBox.getValue() == null || ageField.getText().trim().isEmpty()) return;
        
        String phone = phoneField.getText().trim();
        String ssn = ssnField.getText().trim();
        String email = emailField.getText().trim();
        
        if (!ValidationUtils.isValidPhone(phone)) {
            AlertHelper.showError("Validation Error", "Phone number must be exactly 11 digits and start with 01.");
            return;
        }
        if (!ValidationUtils.isValidSSN(ssn)) {
            AlertHelper.showError("Validation Error", "National ID (SSN) must be exactly 14 digits.");
            return;
        }
        if (!ValidationUtils.isValidEmail(email)) {
            AlertHelper.showError("Validation Error", "Email must end with @gmail.com, @yahoo.com, or @medicore.gov");
            return;
        }
        
        int age = -1;
        try {
            age = Integer.parseInt(ageField.getText().trim());
        } catch (NumberFormatException e) {
            AlertHelper.showError("Validation Error", "Age must be a valid number.");
            return;
        }
        if (age < 1 || age > 100) {
            AlertHelper.showError("Validation Error", "Patient age must be between 1 and 100 years.");
            return;
        }
        java.time.LocalDate birthDate = java.time.LocalDate.now().minusYears(age);
        if (appointmentDatePicker.getValue() == null || !ValidationUtils.isFutureOrToday(appointmentDatePicker.getValue())) {
            AlertHelper.showError("Validation Error", "Appointment date must be today or in the future.");
            return;
        }
        
        int priority = Integer.parseInt(priorityBox.getValue().substring(0, 1));
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int docId = -1;
                double consultationFee = 0.0;
                try (PreparedStatement psDoc = conn.prepareStatement("SELECT d.id, d.consultation_fee FROM doctors d JOIN specializations s ON d.specialization_id = s.id WHERE s.name = ? LIMIT 1")) {
                    psDoc.setString(1, specialtyBox.getValue());
                    try (ResultSet rsDoc = psDoc.executeQuery()) {
                        if (rsDoc.next()) {
                            docId = rsDoc.getInt("id");
                            consultationFee = rsDoc.getDouble("consultation_fee");
                        } else {
                            utils.AlertHelper.showError("Error", "No doctor available for this specialty.");
                            return;
                        }
                    }
                }
                
                // Check if patient already exists by phone or national_id
                int patientId = -1;
                try (PreparedStatement psCheck = conn.prepareStatement("SELECT id FROM patients WHERE phone=? OR national_id=?")) {
                    psCheck.setString(1, phone);
                    psCheck.setString(2, ssn);
                    try (ResultSet rsCheck = psCheck.executeQuery()) {
                        if (rsCheck.next()) {
                            // Patient already exists — reuse their ID for a new appointment
                            patientId = rsCheck.getInt("id");
                        }
                    }
                }
                
                // If patient does not exist, create a new record
                if (patientId == -1) {
                    try (PreparedStatement ps1 = conn.prepareStatement("INSERT INTO patients (full_name, phone, national_id, email, gender, birth_date, address, priority_level) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                        ps1.setString(1, fullNameField.getText());
                        ps1.setString(2, phoneField.getText());
                        ps1.setString(3, ssnField.getText());
                        ps1.setString(4, emailField.getText());
                        ps1.setString(5, genderBox.getValue());
                        ps1.setString(6, birthDate.toString());
                        ps1.setString(7, addressField.getText());
                        ps1.setInt(8, priority);
                        ps1.executeUpdate();
                        try (ResultSet rsP = ps1.getGeneratedKeys()) {
                            if (rsP.next()) patientId = rsP.getInt(1);
                        }
                    }
                }
                
                if (patientId != -1) {
                    try (PreparedStatement ps2 = conn.prepareStatement("INSERT INTO appointments (patient_id, doctor_id, priority, status, appointment_date, consultation_revenue) VALUES (?, ?, ?, 'waiting', ?, ?)")) {
                        ps2.setInt(1, patientId);
                        ps2.setInt(2, docId);
                        ps2.setInt(3, priority);
                        ps2.setString(4, appointmentDatePicker.getValue().toString() + " 00:00:00");
                        ps2.setDouble(5, consultationFee);
                        ps2.executeUpdate();
                    }
                }
                
                conn.commit();
                fullNameField.clear();
                ssnField.clear();
                phoneField.clear();
                emailField.clear();
                addressField.clear();
                ageField.clear();
                appointmentDatePicker.setValue(null);
                loadPatients();
                AlertHelper.showSuccess("Success", "Patient registered and appointment scheduled.");
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) { 
            e.printStackTrace();
            AlertHelper.showError("Database Error", "Failed to add patient: " + e.getMessage());
        }
    }

    public void showManagePatients() {
        SceneManager.switchTo((Stage) rootPane.getScene().getWindow(), new ReceptionistDashboardController().getView());
    }

    public void showPrescriptions() {
        SceneManager.switchTo((Stage) rootPane.getScene().getWindow(), new ReceptionistPrescriptionsController().getView());
    }

    public void handleLogout() {
        Session.getInstance().clear();
        SceneManager.switchTo((Stage) rootPane.getScene().getWindow(), new LoginController().getView());
    }
}
