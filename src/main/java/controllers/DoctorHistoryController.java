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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.Period;

public class DoctorHistoryController {

    public static class HistoryRecord {
        public int recordId;
        public String name;
        public int age;
        public String phone;
        public String ssn;
        public int criticality;
        public String date;

        public String getName() { return name; }
        public int getAge() { return age; }
        public String getPhone() { return phone; }
        public String getSsn() { return ssn; }
        public int getCriticality() { return criticality; }
        public String getDate() { return date; }
    }

    private BorderPane rootPane;
    private TextField searchField;
    private TableView<HistoryRecord> historyTable;

    private TableColumn<HistoryRecord, String> nameCol;
    private TableColumn<HistoryRecord, Integer> ageCol;
    private TableColumn<HistoryRecord, String> phoneCol;
    private TableColumn<HistoryRecord, String> ssnCol;
    private TableColumn<HistoryRecord, Integer> criticalityCol;
    private TableColumn<HistoryRecord, String> dateCol;

    private ObservableList<HistoryRecord> recordList = FXCollections.observableArrayList();

    public Parent getView() {
        rootPane = new BorderPane();
        rootPane.setStyle("-fx-background-color: #EEEEEE;");

        // Top Bar
        HBox topBar = new HBox();
        topBar.setStyle("-fx-background-color: white; -fx-padding: 15 30; -fx-alignment: CENTER_LEFT; -fx-border-color: #D1D1D1; -fx-border-width: 0 0 1 0;");
        Label title = new Label("Patient History");
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
        sidebar.getChildren().add(createSidebarButton("👤  Current Patient", e -> showCurrentPatient(), false));
        sidebar.getChildren().add(createSidebarButton("📜  Patient History", e -> showHistory(), true));
        rootPane.setLeft(sidebar);

        // Center Content
        VBox centerContent = new VBox(25);
        centerContent.setPadding(new Insets(30));
        
        VBox tableCard = new VBox(15);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        tableCard.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 25; -fx-border-color: #D1D1D1; -fx-border-width: 1;");
        
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label tableTitle = new Label("Global Medical Records");
        tableTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #334257;");
        HBox.setHgrow(tableTitle, Priority.ALWAYS);
        tableTitle.setMaxWidth(Double.MAX_VALUE);
        
        searchField = new TextField();
        searchField.setPromptText("Search by Name or ID...");
        searchField.setPrefWidth(250);
        searchField.setStyle("-fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8; -fx-padding: 8;");
        
        Button refreshBtn = new Button("🔄");
        refreshBtn.setOnAction(e -> loadHistory());
        refreshBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #548CA8; -fx-text-fill: #548CA8; -fx-padding: 5 10; -fx-background-radius: 8; -fx-cursor: hand;");
        
        header.getChildren().addAll(tableTitle, searchField, refreshBtn);

        historyTable = new TableView<>();
        VBox.setVgrow(historyTable, Priority.ALWAYS);
        historyTable.setStyle("-fx-background-color: white; -fx-border-color: transparent;");
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        nameCol = new TableColumn<>("Patient Name");
        ageCol = new TableColumn<>("Age");
        phoneCol = new TableColumn<>("Contact");
        ssnCol = new TableColumn<>("National ID");
        criticalityCol = new TableColumn<>("Priority");
        dateCol = new TableColumn<>("Visit Date");

        nameCol.setPrefWidth(180);
        ageCol.setPrefWidth(60);
        phoneCol.setPrefWidth(120);
        ssnCol.setPrefWidth(120);
        criticalityCol.setPrefWidth(80);
        dateCol.setPrefWidth(150);

        historyTable.getColumns().addAll(nameCol, ageCol, phoneCol, ssnCol, criticalityCol, dateCol);

        HBox tableActions = new HBox(15);
        tableActions.setAlignment(Pos.CENTER_RIGHT);
        Button viewBtn = new Button("View Full Record");
        viewBtn.setStyle("-fx-background-color: white; -fx-text-fill: #476072; -fx-border-color: #D1D1D1; -fx-border-width: 1.5; -fx-padding: 10 20; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-weight: 600;");
        
        Button deleteBtn = new Button("Delete Entry");
        deleteBtn.setOnAction(e -> handleDelete());
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #E71D36; -fx-border-color: #E71D36; -fx-border-width: 1.5; -fx-padding: 10 20; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-weight: 600;");
        
        tableActions.getChildren().addAll(viewBtn, deleteBtn);
        tableCard.getChildren().addAll(header, historyTable, tableActions);
        centerContent.getChildren().add(tableCard);
        rootPane.setCenter(centerContent);

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

    public void initialize() {
        if (!"doctor".equals(Session.getInstance().getRole())) return;

        nameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        ageCol.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getAge()).asObject());
        phoneCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPhone()));
        ssnCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSsn()));
        criticalityCol.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getCriticality()).asObject());
        dateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDate()));

        loadHistory();
        setupSearch();
    }

    public void loadHistory() {
        recordList.clear();
        Integer doctorId = Session.getInstance().getDoctorId();
        System.out.println("[DEBUG] Doctor ID from session: " + doctorId);
        
        if (doctorId == null || doctorId < 0) {
            System.out.println("[ERROR] Invalid doctor ID: " + doctorId);
            historyTable.setItems(recordList);
            return;
        }
        int docId = doctorId;
        String sql = "SELECT m.id, p.full_name, p.birth_date, p.phone, p.national_id, a.priority, m.created_at " +
                "FROM medical_records m " +
                "JOIN appointments a ON m.appointment_id = a.id " +
                "JOIN patients p ON a.patient_id = p.id " +
                "WHERE m.doctor_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, docId);
            System.out.println("[DEBUG] Executing query for doctor ID: " + docId);
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    HistoryRecord rec = new HistoryRecord();
                    rec.recordId = rs.getInt("id");
                    rec.name = rs.getString("full_name");
                    rec.phone = rs.getString("phone");
                    rec.ssn = rs.getString("national_id");
                    rec.criticality = rs.getInt("priority");
                    rec.date = rs.getString("created_at");

                    String bdateStr = rs.getString("birth_date");
                    if (bdateStr != null && !bdateStr.isEmpty()) {
                        try {
                            String normalized = bdateStr
                                .replace('٠','0').replace('١','1').replace('٢','2').replace('٣','3')
                                .replace('٤','4').replace('٥','5').replace('٦','6').replace('٧','7')
                                .replace('٨','8').replace('٩','9');
                            LocalDate bdate = LocalDate.parse(normalized);
                            rec.age = Period.between(bdate, LocalDate.now()).getYears();
                        } catch (Exception ex) {
                            rec.age = 0;
                        }
                    } else {
                        rec.age = 0;
                    }
                    recordList.add(rec);
                    count++;
                }
                System.out.println("[DEBUG] Loaded " + count + " records for doctor " + docId);
            }
        } catch (Exception e) { 
            System.out.println("[ERROR] Exception loading history: " + e.getMessage());
            e.printStackTrace(); 
        }
        historyTable.setItems(recordList);
    }

    private void setupSearch() {
        FilteredList<HistoryRecord> filteredData = new FilteredList<>(recordList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(record -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return record.name.toLowerCase().contains(lowerCaseFilter) || record.ssn.toLowerCase().contains(lowerCaseFilter);
            });
        });
        historyTable.setItems(filteredData);
    }

    public void handleDelete() {
        HistoryRecord selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM medical_records WHERE id = ?")) {
                ps.setInt(1, selected.recordId);
                ps.executeUpdate();
                loadHistory();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public void showCurrentPatient() {
        SceneManager.switchTo((Stage) rootPane.getScene().getWindow(), new DoctorDashboardController().getView());
    }

    public void showHistory() {
        // already here
    }

    public void handleLogout() {
        Session.getInstance().clear();
        SceneManager.switchTo((Stage) rootPane.getScene().getWindow(), new LoginController().getView());
    }
}
