package controllers;

import dao.DatabaseConnection;
import dao.DoctorDAO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import models.User;
import utils.AlertHelper;
import utils.ExportService;
import utils.SceneManager;
import utils.Session;
import utils.PasswordUtil;
import utils.ValidationUtils;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class AdminManageUsersController {

    private BorderPane rootPane;
    private TextField searchField;
    private TableView<User> usersTable;

    private TableColumn<User, Integer> idCol;
    private TableColumn<User, String> nameCol;
    private TableColumn<User, String> usernameCol;
    private TableColumn<User, String> roleCol;
    private TableColumn<User, String> specialtyCol;
    private TableColumn<User, String> ssnCol;
    private TableColumn<User, String> emailCol;
    private TableColumn<User, String> phoneCol;
    private TableColumn<User, Double> salaryCol;
    private TableColumn<User, String> birthDateCol;
    private TableColumn<User, String> assignedDoctorCol;
    private TableColumn<User, Boolean> activeCol;

    private TextField fullNameField;
    private TextField ssnField;
    private DatePicker birthDatePicker;
    private TextField salaryField;
    private TextField usernameField;
    private TextField emailField;
    private TextField phoneField;
    private PasswordField passwordField;
    private ComboBox<String> roleBox;
    private TextField specialtyField;
    private TextField consultationFeeField;
    private ComboBox<String> doctorBox;

    private VBox specialtyContainer;
    private VBox consultationFeeContainer;
    private VBox nurseDoctorContainer;
    private VBox salaryContainer;
    private VBox usernameContainer;
    private VBox passwordContainer;
    
    private Button toggleActiveBtn;

    private ObservableList<User> userList = FXCollections.observableArrayList();
    private Map<String, Integer> doctorMap = new HashMap<>();

    public Parent getView() {
        rootPane = new BorderPane();
        rootPane.setStyle("-fx-background-color: #EEEEEE;");

        // Top Bar
        HBox topBar = new HBox();
        topBar.setStyle("-fx-background-color: white; -fx-padding: 15 30; -fx-alignment: CENTER_LEFT; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.03), 10, 0, 0, 4); -fx-border-color: #D1D1D1; -fx-border-width: 0 0 1 0;");
        Label title = new Label("User Management");
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
        
        sidebar.getChildren().add(sidebarTitle);
        sidebar.getChildren().add(createSidebarButton("📊  Dashboard", e -> showDashboard(), false));
        sidebar.getChildren().add(createSidebarButton("👥  User Management", e -> showManageUsers(), true));
        
        Separator sep = new Separator();
        sep.setStyle("-fx-padding: 10 0; -fx-opacity: 0.1;");
        sidebar.getChildren().add(sep);
        
        sidebar.getChildren().add(createSidebarButton("📤  Export Now", e -> handleExport(), false));
        sidebar.getChildren().add(createSidebarButton("🤖  Ask AI (Beta)", e -> handleOpenAiChat(), false));
        rootPane.setLeft(sidebar);

        // Center Content
        HBox mainLayout = new HBox(30);
        mainLayout.setPadding(new Insets(30));

        // Left Side: Table Section
        VBox tableSection = new VBox(15);
        HBox.setHgrow(tableSection, Priority.ALWAYS);
        tableSection.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 25; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 20, 0, 0, 8); -fx-border-color: #D1D1D1; -fx-border-width: 1;");
        
        HBox tableHeader = new HBox(10);
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        Label tableTitle = new Label("System Users");
        tableTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #334257;");
        HBox.setHgrow(tableTitle, Priority.ALWAYS);
        tableTitle.setMaxWidth(Double.MAX_VALUE);
        
        searchField = new TextField();
        searchField.setPromptText("Search users...");
        searchField.setPrefWidth(250);
        searchField.setStyle("-fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8;");
        
        Button refreshBtn = new Button("🔄");
        refreshBtn.setOnAction(e -> loadUsers());
        refreshBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #548CA8; -fx-text-fill: #548CA8; -fx-padding: 5 10; -fx-background-radius: 8; -fx-cursor: hand;");
        
        tableHeader.getChildren().addAll(tableTitle, searchField, refreshBtn);

        usersTable = new TableView<>();
        VBox.setVgrow(usersTable, Priority.ALWAYS);
        usersTable.setStyle("-fx-background-color: white; -fx-border-color: transparent;");
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        idCol = new TableColumn<>("ID");
        nameCol = new TableColumn<>("Full Name");
        usernameCol = new TableColumn<>("Username");
        roleCol = new TableColumn<>("Role");
        specialtyCol = new TableColumn<>("Specialty");
        ssnCol = new TableColumn<>("SSN");
        emailCol = new TableColumn<>("Email");
        phoneCol = new TableColumn<>("Phone");
        salaryCol = new TableColumn<>("Salary");
        birthDateCol = new TableColumn<>("Birth Date");
        assignedDoctorCol = new TableColumn<>("Assigned To");
        activeCol = new TableColumn<>("Status");

        usersTable.getColumns().addAll(idCol, nameCol, usernameCol, roleCol, specialtyCol, ssnCol, emailCol, phoneCol, salaryCol, birthDateCol, assignedDoctorCol, activeCol);
        tableSection.getChildren().addAll(tableHeader, usersTable);

        // Right Side: Form Section
        VBox formSection = new VBox(20);
        formSection.setMinWidth(400);
        formSection.setMaxWidth(400);
        formSection.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 25; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 20, 0, 0, 8); -fx-border-color: #D1D1D1; -fx-border-width: 1;");
        
        Label formTitle = new Label("User Details");
        formTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #334257;");

        ScrollPane formScroll = new ScrollPane();
        formScroll.setFitToWidth(true);
        formScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox formFields = new VBox(15);
        formScroll.setContent(formFields);
        VBox.setVgrow(formScroll, Priority.ALWAYS);

        fullNameField = createStyledTextField("Enter full name");
        ssnField = createStyledTextField("14-digit National ID");
        birthDatePicker = new DatePicker();
        birthDatePicker.setMaxWidth(Double.MAX_VALUE);
        birthDatePicker.setStyle("-fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8; -fx-padding: 5;");
        salaryField = createStyledTextField("e.g. 5000");
        usernameField = createStyledTextField("Choose username");
        emailField = createStyledTextField("email@example.com");
        phoneField = createStyledTextField("+1 234...");
        passwordField = new PasswordField();
        passwordField.setPromptText("••••••••");
        passwordField.setStyle("-fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8; -fx-padding: 10;");
        roleBox = new ComboBox<>();
        roleBox.setMaxWidth(Double.MAX_VALUE);
        roleBox.setStyle("-fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8; -fx-padding: 8;");
        
        specialtyField = createStyledTextField("e.g. Cardiology");
        consultationFeeField = createStyledTextField("e.g. 50");
        doctorBox = new ComboBox<>();
        doctorBox.setMaxWidth(Double.MAX_VALUE);
        doctorBox.setStyle("-fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8; -fx-padding: 8;");

        specialtyContainer = createFormField("Specialization", specialtyField);
        consultationFeeContainer = createFormField("Consultation Fee ($)", consultationFeeField);
        nurseDoctorContainer = createFormField("Assign to Doctor", doctorBox);
        salaryContainer = createFormField("Monthly Salary ($)", salaryField);
        usernameContainer = createFormField("Username", usernameField);
        passwordContainer = createFormField("Password", passwordField);

        formFields.getChildren().addAll(
            createFormField("Full Name", fullNameField),
            createFormField("National ID (SSN)", ssnField),
            new HBox(10, createFormField("Date of Birth", birthDatePicker), salaryContainer),
            usernameContainer,
            createFormField("Email Address", emailField),
            createFormField("Phone Number", phoneField),
            passwordContainer,
            createFormField("System Role", roleBox),
            specialtyContainer,
            consultationFeeContainer,
            nurseDoctorContainer
        );

        specialtyContainer.setVisible(false); specialtyContainer.setManaged(false);
        consultationFeeContainer.setVisible(false); consultationFeeContainer.setManaged(false);
        nurseDoctorContainer.setVisible(false); nurseDoctorContainer.setManaged(false);

        VBox actionsBox = new VBox(10);
        HBox addUpdateRow = new HBox(10);
        Button addBtn = new Button("Add User");
        addBtn.setOnAction(e -> handleAdd());
        HBox.setHgrow(addBtn, Priority.ALWAYS); addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setStyle("-fx-background-color: #548CA8; -fx-text-fill: white; -fx-font-weight: 800; -fx-padding: 12; -fx-background-radius: 10; -fx-cursor: hand;");
        
        Button updateBtn = new Button("Update");
        updateBtn.setOnAction(e -> handleUpdate());
        HBox.setHgrow(updateBtn, Priority.ALWAYS); updateBtn.setMaxWidth(Double.MAX_VALUE);
        updateBtn.setStyle("-fx-background-color: white; -fx-text-fill: #548CA8; -fx-border-color: #548CA8; -fx-border-width: 1.5; -fx-padding: 10; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-weight: 700;");
        addUpdateRow.getChildren().addAll(addBtn, updateBtn);

        HBox deactDeleteRow = new HBox(10);
        toggleActiveBtn = new Button("Deactivate");
        toggleActiveBtn.setOnAction(e -> handleDeactivate());
        HBox.setHgrow(toggleActiveBtn, Priority.ALWAYS); toggleActiveBtn.setMaxWidth(Double.MAX_VALUE);
        toggleActiveBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #E71D36; -fx-border-color: #E71D36; -fx-border-width: 1.5; -fx-padding: 10; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-weight: 700;");
        
        Button deleteBtn = new Button("Delete");
        deleteBtn.setOnAction(e -> handleDelete());
        HBox.setHgrow(deleteBtn, Priority.ALWAYS); deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #E71D36; -fx-border-color: #E71D36; -fx-border-width: 1.5; -fx-padding: 10; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-weight: 700;");
        deactDeleteRow.getChildren().addAll(toggleActiveBtn, deleteBtn);

        actionsBox.getChildren().addAll(addUpdateRow, deactDeleteRow);
        formSection.getChildren().addAll(formTitle, formScroll, actionsBox);

        mainLayout.getChildren().addAll(tableSection, formSection);
        rootPane.setCenter(mainLayout);

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

    private TextField createStyledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8; -fx-padding: 10;");
        return tf;
    }

    private VBox createFormField(String labelText, javafx.scene.Node field) {
        VBox v = new VBox(5);
        Label l = new Label(labelText);
        l.setStyle("-fx-text-fill: #476072; -fx-font-weight: 600;");
        v.getChildren().addAll(l, field);
        return v;
    }

    public void initialize() {
        if (!"admin".equals(Session.getInstance().getRole())) return;

        roleBox.setItems(FXCollections.observableArrayList("admin", "doctor", "receptionist", "pharmacist", "nurse"));
        roleBox.setOnAction(e -> handleRoleSelection());

        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        specialtyCol.setCellValueFactory(new PropertyValueFactory<>("specialty"));
        ssnCol.setCellValueFactory(new PropertyValueFactory<>("ssn"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        salaryCol.setCellValueFactory(new PropertyValueFactory<>("salary"));
        birthDateCol.setCellValueFactory(new PropertyValueFactory<>("birthDate"));
        assignedDoctorCol.setCellValueFactory(new PropertyValueFactory<>("assignedDoctor"));
        activeCol.setCellValueFactory(new PropertyValueFactory<>("active"));

        usersTable.setRowFactory(tv -> {
            TableRow<User> row = new TableRow<>();
            ContextMenu cm = new ContextMenu();
            MenuItem biometricItem = new MenuItem("Enroll Biometrics");
            biometricItem.setOnAction(e -> openFaceEnrollment(row.getItem()));
            cm.getItems().add(biometricItem);
            row.contextMenuProperty().bind(javafx.beans.binding.Bindings.when(row.emptyProperty()).then((ContextMenu)null).otherwise(cm));
            return row;
        });

        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) populateForm(newVal);
        });

        loadUsers();
        loadDoctorsForNurses();
        setupSearch();
    }

    private void handleRoleSelection() {
        String role = roleBox.getValue();
        boolean isNurse = "nurse".equals(role);
        boolean isDoctor = "doctor".equals(role);
        
        specialtyContainer.setVisible(isDoctor); specialtyContainer.setManaged(isDoctor);
        consultationFeeContainer.setVisible(isDoctor); consultationFeeContainer.setManaged(isDoctor);
        nurseDoctorContainer.setVisible(isNurse); nurseDoctorContainer.setManaged(isNurse);
        
        // Hide username and password for nurses
        usernameContainer.setVisible(!isNurse); usernameContainer.setManaged(!isNurse);
        passwordContainer.setVisible(!isNurse); passwordContainer.setManaged(!isNurse);
    }

    private void loadUsers() {
        userList.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Load regular users
            String sqlUsers = "SELECT u.*, s.name as spec_name FROM users u " +
                             "LEFT JOIN doctors d ON u.id = d.user_id " +
                             "LEFT JOIN specializations s ON d.specialization_id = s.id " +
                             "WHERE u.role != 'nurse'"; // Assuming 'nurse' role is removed from users table eventually
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlUsers)) {
                while (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setFullName(rs.getString("full_name"));
                    user.setUsername(rs.getString("username"));
                    user.setRole(rs.getString("role"));
                    user.setSpecialty(rs.getString("spec_name"));
                    user.setSsn(rs.getString("ssn"));
                    user.setEmail(rs.getString("email"));
                    user.setPhone(rs.getString("phone"));
                    user.setSalary(rs.getDouble("salary"));
                    user.setBirthDate(rs.getString("birth_date"));
                    user.setActive(rs.getBoolean("is_active"));
                    userList.add(user);
                }
            }

            // 2. Load nurses from separate table
            String sqlNurses = "SELECT n.*, u.full_name as doc_name FROM nurses n " +
                              "LEFT JOIN doctors d ON n.doctor_id = d.id " +
                              "LEFT JOIN users u ON d.user_id = u.id";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlNurses)) {
                while (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id")); // This is the nurse_id from nurses table
                    user.setFullName(rs.getString("full_name"));
                    user.setUsername("-"); // No username for nurses
                    user.setRole("nurse");
                    user.setSsn(rs.getString("ssn"));
                    user.setEmail(rs.getString("email"));
                    user.setPhone(rs.getString("phone"));
                    user.setSalary(rs.getDouble("salary"));
                    user.setBirthDate(rs.getString("birth_date"));
                    user.setActive(rs.getBoolean("is_active"));
                    user.setAssignedDoctor(rs.getString("doc_name") != null ? "Dr. " + rs.getString("doc_name") : "None");
                    userList.add(user);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        usersTable.setItems(userList);
    }

    private void loadDoctorsForNurses() {
        doctorBox.getItems().clear();
        doctorMap.clear();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT d.id, u.full_name FROM doctors d JOIN users u ON d.user_id = u.id WHERE u.is_active = 1")) {
            while (rs.next()) {
                String label = "Dr. " + rs.getString("full_name");
                doctorBox.getItems().add(label);
                doctorMap.put(label, rs.getInt("id"));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupSearch() {
        FilteredList<User> filtered = new FilteredList<>(userList, p -> true);
        searchField.textProperty().addListener((obs, old, val) -> filtered.setPredicate(u -> {
            if (val == null || val.isEmpty()) return true;
            String f = val.toLowerCase();
            return (u.getFullName() != null && u.getFullName().toLowerCase().contains(f)) || 
                   (u.getUsername() != null && u.getUsername().toLowerCase().contains(f)) || 
                   (u.getRole() != null && u.getRole().toLowerCase().contains(f));
        }));
        usersTable.setItems(filtered);
    }

    private void populateForm(User user) {
        fullNameField.setText(user.getFullName());
        usernameField.setText(user.getUsername());
        emailField.setText(user.getEmail());
        phoneField.setText(user.getPhone());
        roleBox.setValue(user.getRole());
        salaryField.setText(String.valueOf(user.getSalary()));
        ssnField.setText(user.getSsn());
        if (user.getBirthDate() != null) {
            try { birthDatePicker.setValue(LocalDate.parse(user.getBirthDate())); }
            catch (Exception e) { birthDatePicker.setValue(null); }
        }
        toggleActiveBtn.setText(user.isActive() ? "Deactivate" : "Activate");
        
        boolean isNurse = "nurse".equals(user.getRole());
        boolean isDoctor = "doctor".equals(user.getRole());
        
        if (isDoctor) {
            specialtyField.setText(user.getSpecialty());
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT consultation_fee FROM doctors WHERE user_id = ?")) {
                ps.setInt(1, user.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) consultationFeeField.setText(String.valueOf(rs.getDouble(1)));
                }
            } catch (Exception e) {}
        } else if (isNurse) {
            doctorBox.setValue(user.getAssignedDoctor());
        }
    }

    public void handleAdd() {
        String role = roleBox.getValue();
        String fullName = fullNameField.getText().trim();
        String ssn = ssnField.getText().trim();
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = passwordField.getText();
        LocalDate dob = birthDatePicker.getValue();

        if (fullName.isEmpty() || ssn.isEmpty() || email.isEmpty() || phone.isEmpty() || role == null || dob == null) {
            AlertHelper.showError("Validation Error", "Please fill in all required fields.");
            return;
        }

        if (!ValidationUtils.isValidPhone(phone)) {
            AlertHelper.showError("Validation Error", "Phone number must be 11 digits and start with '01'.");
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
        if (!ValidationUtils.isValidAge(dob)) {
            AlertHelper.showError("Validation Error", "User age must be between 1 and 100 years.");
            return;
        }

        boolean isNurse = "nurse".equals(role);
        if (!isNurse && username.isEmpty()) {
            AlertHelper.showError("Validation Error", "Username is required for this role.");
            return;
        }

        int newUserId = -1;
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (isNurse) {
                    String sqlNurse = "INSERT INTO nurses (full_name, email, phone, ssn, birth_date, salary, doctor_id, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, 1)";
                    try (PreparedStatement ps = conn.prepareStatement(sqlNurse, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, fullName);
                        ps.setString(2, email);
                        ps.setString(3, phone);
                        ps.setString(4, ssn);
                        ps.setString(5, dob.toString());
                        try { ps.setDouble(6, Double.parseDouble(salaryField.getText())); } catch (Exception e) { ps.setDouble(6, 0.0); }
                        
                        Integer doctorId = doctorMap.get(doctorBox.getValue());
                        if (doctorId != null) ps.setInt(7, doctorId);
                        else ps.setNull(7, java.sql.Types.INTEGER);
                        
                        ps.executeUpdate();
                        try (ResultSet rs = ps.getGeneratedKeys()) {
                            if (rs.next()) {
                                int nurseId = rs.getInt(1);
                                if (doctorId != null) {
                                    try (PreparedStatement psAss = conn.prepareStatement("INSERT INTO nurse_assignments (nurse_id, doctor_id) VALUES (?, ?)")) {
                                        psAss.setInt(1, nurseId);
                                        psAss.setInt(2, doctorId);
                                        psAss.executeUpdate();
                                    }
                                }
                            }
                        }
                    }
                } else {
                    String sqlUser = "INSERT INTO users (full_name, username, password_hash, role, ssn, email, phone, salary, birth_date, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)";
                    try (PreparedStatement ps = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, fullName);
                        ps.setString(2, username);
                        ps.setString(3, PasswordUtil.hash(password));
                        ps.setString(4, role);
                        ps.setString(5, ssn);
                        ps.setString(6, email);
                        ps.setString(7, phone);
                        try { ps.setDouble(8, Double.parseDouble(salaryField.getText())); } catch (Exception e) { ps.setDouble(8, 0.0); }
                        ps.setString(9, dob.toString());
                        ps.executeUpdate();

                        try (ResultSet rs = ps.getGeneratedKeys()) {
                            if (rs.next()) {
                                int userId = rs.getInt(1);
                                newUserId = userId;
                                if ("doctor".equals(role)) {
                                    int specId = -1;
                                    try (PreparedStatement psSpec = conn.prepareStatement("INSERT INTO specializations (name) VALUES (?) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id)", Statement.RETURN_GENERATED_KEYS)) {
                                        psSpec.setString(1, specialtyField.getText());
                                        psSpec.executeUpdate();
                                        try (ResultSet rsS = psSpec.getGeneratedKeys()) {
                                            if (rsS.next()) specId = rsS.getInt(1);
                                        }
                                    }
                                    if (specId != -1) {
                                        try (PreparedStatement psDoc = conn.prepareStatement("INSERT INTO doctors (user_id, specialization_id, consultation_fee) VALUES (?, ?, ?)")) {
                                            psDoc.setInt(1, userId);
                                            psDoc.setInt(2, specId);
                                            try { psDoc.setDouble(3, Double.parseDouble(consultationFeeField.getText())); } catch (Exception e) { psDoc.setDouble(3, 0.0); }
                                            psDoc.executeUpdate();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                conn.commit();
                AlertHelper.showSuccess("Success", role.substring(0,1).toUpperCase() + role.substring(1) + " added successfully.");
                
                if ("admin".equals(role) && newUserId != -1) {
                    User newUser = new User();
                    newUser.setId(newUserId);
                    newUser.setFullName(fullName);
                    newUser.setUsername(username);
                    newUser.setRole(role);
                    newUser.setEmail(email);
                    newUser.setPhone(phone);
                    openFaceEnrollment(newUser);
                }
                
                loadUsers(); clearForm();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertHelper.showError("Error", "Could not add " + role + ": " + e.getMessage());
        }
    }

    public void handleUpdate() {
        User sel = usersTable.getSelectionModel().getSelectedItem();
        if (sel == null) { AlertHelper.showWarning("Selection Required", "Select a user to update."); return; }
        
        boolean isNurse = "nurse".equals(sel.getRole());
        
        if (!ValidationUtils.isValidPhone(phoneField.getText())) {
            AlertHelper.showError("Validation Error", "Phone number must be 11 digits and start with '01'.");
            return;
        }
        if (!ValidationUtils.isValidSSN(ssnField.getText())) {
            AlertHelper.showError("Validation Error", "National ID (SSN) must be exactly 14 digits.");
            return;
        }
        if (!ValidationUtils.isValidEmail(emailField.getText())) {
            AlertHelper.showError("Validation Error", "Email must end with @gmail.com, @yahoo.com, or @medicore.gov");
            return;
        }
        if (!ValidationUtils.isValidAge(birthDatePicker.getValue())) {
            AlertHelper.showError("Validation Error", "User age must be between 1 and 100 years.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (isNurse) {
                    String sqlNurse = "UPDATE nurses SET full_name=?, ssn=?, email=?, phone=?, salary=?, birth_date=?, doctor_id=? WHERE id=?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlNurse)) {
                        ps.setString(1, fullNameField.getText());
                        ps.setString(2, ssnField.getText());
                        ps.setString(3, emailField.getText());
                        ps.setString(4, phoneField.getText());
                        ps.setDouble(5, Double.parseDouble(salaryField.getText()));
                        ps.setString(6, birthDatePicker.getValue().toString());
                        
                        Integer doctorId = doctorMap.get(doctorBox.getValue());
                        if (doctorId != null) ps.setInt(7, doctorId);
                        else ps.setNull(7, java.sql.Types.INTEGER);
                        
                        ps.setInt(8, sel.getId());
                        ps.executeUpdate();
                        
                        // Update assignment
                        try (PreparedStatement psDel = conn.prepareStatement("DELETE FROM nurse_assignments WHERE nurse_id = ?")) {
                            psDel.setInt(1, sel.getId());
                            psDel.executeUpdate();
                        }
                        if (doctorId != null) {
                            try (PreparedStatement psAss = conn.prepareStatement("INSERT INTO nurse_assignments (nurse_id, doctor_id) VALUES (?, ?)")) {
                                psAss.setInt(1, sel.getId());
                                psAss.setInt(2, doctorId);
                                psAss.executeUpdate();
                            }
                        }
                    }
                } else {
                    String sqlUser = "UPDATE users SET full_name=?, ssn=?, email=?, phone=?, salary=?, birth_date=? WHERE id=?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlUser)) {
                        ps.setString(1, fullNameField.getText());
                        ps.setString(2, ssnField.getText());
                        ps.setString(3, emailField.getText());
                        ps.setString(4, phoneField.getText());
                        ps.setDouble(5, Double.parseDouble(salaryField.getText()));
                        ps.setString(6, birthDatePicker.getValue().toString());
                        ps.setInt(7, sel.getId());
                        ps.executeUpdate();
                    }

                    if ("doctor".equals(sel.getRole())) {
                        int specId = -1;
                        try (PreparedStatement psSpec = conn.prepareStatement("INSERT INTO specializations (name) VALUES (?) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id)", Statement.RETURN_GENERATED_KEYS)) {
                            psSpec.setString(1, specialtyField.getText());
                            psSpec.executeUpdate();
                            try (ResultSet rsS = psSpec.getGeneratedKeys()) {
                                if (rsS.next()) specId = rsS.getInt(1);
                            }
                        }
                        if (specId != -1) {
                            try (PreparedStatement psDoc = conn.prepareStatement("UPDATE doctors SET specialization_id = ?, consultation_fee = ? WHERE user_id = ?")) {
                                psDoc.setInt(1, specId);
                                psDoc.setDouble(2, Double.parseDouble(consultationFeeField.getText()));
                                psDoc.setInt(3, sel.getId());
                                psDoc.executeUpdate();
                            }
                        }
                    }
                    
                    if (!passwordField.getText().isEmpty()) {
                        try (PreparedStatement psPass = conn.prepareStatement("UPDATE users SET password_hash = ? WHERE id = ?")) {
                            psPass.setString(1, PasswordUtil.hash(passwordField.getText()));
                            psPass.setInt(2, sel.getId());
                            psPass.executeUpdate();
                        }
                    }
                }

                conn.commit();
                AlertHelper.showSuccess("Success", "User updated.");
                loadUsers();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertHelper.showError("Error", "Could not update user: " + e.getMessage());
        }
    }

    public void handleDeactivate() {
        User sel = usersTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        boolean isNurse = "nurse".equals(sel.getRole());
        String table = isNurse ? "nurses" : "users";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE " + table + " SET is_active=? WHERE id=?")) {
            ps.setBoolean(1, !sel.isActive());
            ps.setInt(2, sel.getId());
            ps.executeUpdate();
            loadUsers();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void handleDelete() {
        User sel = usersTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        if (!AlertHelper.showConfirm("Confirm Delete", "Permanently delete " + sel.getFullName() + "?")) return;
        
        boolean isNurse = "nurse".equals(sel.getRole());
        String table = isNurse ? "nurses" : "users";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM " + table + " WHERE id=?")) {
            ps.setInt(1, sel.getId());
            int affected = ps.executeUpdate();
            
            if (affected > 0) {
                loadUsers(); clearForm();
                AlertHelper.showSuccess("Deleted", "User has been permanently removed.");
            } else {
                AlertHelper.showError("Delete Error", "No record found with ID " + sel.getId());
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
            AlertHelper.showError("Delete Error", "Could not remove user. They may have related records.\nDetails: " + e.getMessage());
        }
    }

    private void clearForm() {
        fullNameField.clear(); usernameField.clear(); passwordField.clear(); ssnField.clear(); emailField.clear(); phoneField.clear(); salaryField.clear(); specialtyField.clear(); consultationFeeField.clear();
        birthDatePicker.setValue(null); roleBox.setValue(null);
    }

    private void openFaceEnrollment(User user) {
        try {
            FaceEnrollmentController controller = new FaceEnrollmentController();
            controller.setUser(user);
            Stage stage = new Stage();
            stage.setTitle("Biometric Enrollment - " + user.getFullName());
            stage.setScene(new javafx.scene.Scene(controller.getView()));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void handleExport() {
        new Thread(() -> {
            try {
                Platform.runLater(() -> AlertHelper.showInfo("Export Started", "Backing up database locally..."));
                String path = ExportService.exportDatabaseToExcel();
                Platform.runLater(() -> AlertHelper.showSuccess("Export Successful", "Saved locally to: " + path));
            } catch (Exception e) { Platform.runLater(() -> AlertHelper.showError("Export Failed", e.getMessage())); }
        }).start();
    }

    public void handleOpenAiChat() {
        try {
            Stage s = new Stage();
            s.setTitle("MediCore AI Assistant");
            s.setScene(new javafx.scene.Scene(new AiChatController().getView()));
            s.show();
        } catch (Exception e) { AlertHelper.showError("Chat Error", e.getMessage()); }
    }

    public void showDashboard() {
        SceneManager.switchTo((Stage) rootPane.getScene().getWindow(), new AdminDashboardController().getView());
    }

    public void showManageUsers() {
        SceneManager.switchTo((Stage) rootPane.getScene().getWindow(), new AdminManageUsersController().getView());
    }

    public void handleLogout() {
        Session.getInstance().clear();
        SceneManager.switchTo((Stage) rootPane.getScene().getWindow(), new LoginController().getView());
    }
}
