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
import java.sql.Statement;

public class PharmacyDashboardController {

    public static class PrescriptionModel {
        public int id;
        public String patientName, medicine, dosage, frequency, doctor, date;
        public int duration;

        public int getId()           { return id; }
        public String getPatientName() { return patientName; }
        public String getMedicine()  { return medicine; }
        public String getDosage()    { return dosage; }
        public String getFrequency() { return frequency; }
        public int getDuration()     { return duration; }
        public String getDoctor()    { return doctor; }
        public String getDate()      { return date; }
    }

    public static class DispensedModel {
        public int id;
        public String patientName, medicine, dispensedBy, dispensedAt;

        public int getId()             { return id; }
        public String getPatientName() { return patientName; }
        public String getMedicine()    { return medicine; }
        public String getDispensedBy() { return dispensedBy; }
        public String getDispensedAt() { return dispensedAt; }
    }

    private BorderPane rootPane;
    private TextField searchField;

    private TableView<PrescriptionModel> pendingTable;
    private TableColumn<PrescriptionModel, Integer> idCol;
    private TableColumn<PrescriptionModel, String>  patientCol;
    private TableColumn<PrescriptionModel, String>  medicineCol;
    private TableColumn<PrescriptionModel, String>  dosageCol;
    private TableColumn<PrescriptionModel, String>  freqCol;
    private TableColumn<PrescriptionModel, Integer> durationCol;
    private TableColumn<PrescriptionModel, String>  doctorCol;
    private TableColumn<PrescriptionModel, String>  dateCol;
    private Label stockLabel;

    private TableView<DispensedModel>  dispensedTable;
    private TableColumn<DispensedModel, Integer> dRefCol;
    private TableColumn<DispensedModel, String>  dPatientCol;
    private TableColumn<DispensedModel, String>  dMedCol;
    private TableColumn<DispensedModel, String>  dByCol;
    private TableColumn<DispensedModel, String>  dAtCol;

    private final ObservableList<PrescriptionModel> pendingList   = FXCollections.observableArrayList();
    private final ObservableList<DispensedModel>    dispensedList = FXCollections.observableArrayList();

    public Parent getView() {
        rootPane = new BorderPane();
        rootPane.setStyle("-fx-background-color: #EEEEEE;");

        // Top Bar
        HBox topBar = new HBox();
        topBar.setStyle("-fx-background-color: white; -fx-padding: 15 30; -fx-alignment: CENTER_LEFT; -fx-border-color: #D1D1D1; -fx-border-width: 0 0 1 0;");
        Label title = new Label("Pharmacy");
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
        sidebar.getChildren().add(createSidebarButton("💊  Prescriptions", e -> showPrescriptions(), true));
        sidebar.getChildren().add(createSidebarButton("📦  Inventory", e -> showInventory(), false));
        sidebar.getChildren().add(createSidebarButton("💰  POS", e -> showPos(), false));
        sidebar.getChildren().add(createSidebarButton("📊  Sales History", e -> showSales(), false));
        sidebar.getChildren().add(createSidebarButton("🏭  Suppliers", e -> showSuppliers(), false));
        rootPane.setLeft(sidebar);

        // Center Content
        VBox centerBox = new VBox(20);
        centerBox.setPadding(new Insets(30));

        // Pending Card
        VBox pendingCard = new VBox(15);
        VBox.setVgrow(pendingCard, Priority.ALWAYS);
        pendingCard.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 25; -fx-border-color: #D1D1D1; -fx-border-width: 1;");
        
        HBox pendingHeader = new HBox(10);
        pendingHeader.setAlignment(Pos.CENTER_LEFT);
        Label pendingTitle = new Label("Pending Prescriptions");
        pendingTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #334257;");
        HBox.setHgrow(pendingTitle, Priority.ALWAYS);
        pendingTitle.setMaxWidth(Double.MAX_VALUE);
        
        searchField = new TextField();
        searchField.setPromptText("Search patient or medicine...");
        searchField.setPrefWidth(280);
        searchField.setStyle("-fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8; -fx-padding: 8;");
        
        Button refreshBtn = new Button("🔄");
        refreshBtn.setOnAction(e -> loadPending());
        refreshBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #548CA8; -fx-text-fill: #548CA8; -fx-padding: 5 10; -fx-background-radius: 8; -fx-cursor: hand;");
        
        pendingHeader.getChildren().addAll(pendingTitle, searchField, refreshBtn);

        pendingTable = new TableView<>();
        VBox.setVgrow(pendingTable, Priority.ALWAYS);
        pendingTable.setStyle("-fx-background-color: white; -fx-border-color: transparent;");
        pendingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        idCol = new TableColumn<>("Ref #");
        patientCol = new TableColumn<>("Patient");
        medicineCol = new TableColumn<>("Medicine");
        dosageCol = new TableColumn<>("Dosage");
        freqCol = new TableColumn<>("Frequency");
        durationCol = new TableColumn<>("Days");
        doctorCol = new TableColumn<>("Doctor");
        dateCol = new TableColumn<>("Prescribed");

        pendingTable.getColumns().addAll(idCol, patientCol, medicineCol, dosageCol, freqCol, durationCol, doctorCol, dateCol);
        
        HBox pendingFooter = new HBox(10);
        pendingFooter.setAlignment(Pos.CENTER_RIGHT);
        stockLabel = new Label("");
        stockLabel.setStyle("-fx-text-fill: #476072; -fx-font-weight: 600;");
        HBox.setHgrow(stockLabel, Priority.ALWAYS);
        stockLabel.setMaxWidth(Double.MAX_VALUE);
        
        Button dispenseBtn = new Button("✅  Dispense Selected");
        dispenseBtn.setOnAction(e -> handleDispense());
        dispenseBtn.setStyle("-fx-background-color: #548CA8; -fx-text-fill: white; -fx-font-weight: 800; -fx-padding: 12 25; -fx-background-radius: 10; -fx-cursor: hand;");
        
        pendingFooter.getChildren().addAll(stockLabel, dispenseBtn);
        pendingCard.getChildren().addAll(pendingHeader, pendingTable, pendingFooter);

        // History Card
        VBox historyCard = new VBox(15);
        historyCard.setPrefHeight(250);
        historyCard.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 25; -fx-border-color: #D1D1D1; -fx-border-width: 1;");
        
        Label historyTitle = new Label("Dispensed History");
        historyTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #334257;");

        dispensedTable = new TableView<>();
        VBox.setVgrow(dispensedTable, Priority.ALWAYS);
        dispensedTable.setStyle("-fx-background-color: white; -fx-border-color: transparent;");
        dispensedTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        dRefCol = new TableColumn<>("Ref #");
        dPatientCol = new TableColumn<>("Patient");
        dMedCol = new TableColumn<>("Medicine");
        dByCol = new TableColumn<>("Dispensed By");
        dAtCol = new TableColumn<>("Dispensed At");

        dispensedTable.getColumns().addAll(dRefCol, dPatientCol, dMedCol, dByCol, dAtCol);
        historyCard.getChildren().addAll(historyTitle, dispensedTable);

        centerBox.getChildren().addAll(pendingCard, historyCard);
        rootPane.setCenter(centerBox);

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
        socket.NotificationClient.setRefreshCallback(this::loadPending);

        idCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        patientCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPatientName()));
        medicineCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMedicine()));
        dosageCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDosage()));
        freqCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFrequency()));
        durationCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getDuration()).asObject());
        doctorCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDoctor()));
        dateCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDate()));

        dRefCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        dPatientCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPatientName()));
        dMedCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMedicine()));
        dByCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDispensedBy()));
        dAtCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDispensedAt()));

        pendingTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) checkStock(sel.medicine);
            else stockLabel.setText("");
        });

        loadPending();
        loadDispensed();
        setupSearch();
    }

    public void loadPending() {
        pendingList.clear();
        String sql = "SELECT pr.id, pr.medicine_name, pr.dosage, pr.frequency, pr.duration_days, pr.created_at, " +
                     "p.full_name AS p_name, u.full_name AS d_name " +
                     "FROM prescriptions pr " +
                     "JOIN medical_records mr ON pr.medical_record_id = mr.id " +
                     "JOIN appointments a ON mr.appointment_id = a.id " +
                     "JOIN patients p ON a.patient_id = p.id " +
                     "JOIN doctors d ON mr.doctor_id = d.id " +
                     "JOIN users u ON d.user_id = u.id " +
                     "WHERE pr.id NOT IN (SELECT prescription_id FROM prescription_dispensing) " +
                     "ORDER BY pr.created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                PrescriptionModel m = new PrescriptionModel();
                m.id          = rs.getInt("id");
                m.patientName = rs.getString("p_name");
                m.medicine    = rs.getString("medicine_name");
                m.dosage      = rs.getString("dosage");
                m.frequency   = rs.getString("frequency");
                m.duration    = rs.getInt("duration_days");
                m.doctor      = "Dr. " + rs.getString("d_name");
                m.date        = rs.getString("created_at");
                pendingList.add(m);
            }
        } catch (Exception e) {
            utils.AlertHelper.showError("Load Error", "Failed to load pending prescriptions:\n" + e.getMessage());
        }
        pendingTable.setItems(pendingList);
    }

    private void loadDispensed() {
        dispensedList.clear();
        String sql = "SELECT pr.id, pr.medicine_name, p.full_name AS p_name, u.full_name AS by_name, pd.dispensed_at " +
                     "FROM prescription_dispensing pd " +
                     "JOIN prescriptions pr ON pd.prescription_id = pr.id " +
                     "JOIN medical_records mr ON pr.medical_record_id = mr.id " +
                     "JOIN appointments a ON mr.appointment_id = a.id " +
                     "JOIN patients p ON a.patient_id = p.id " +
                     "JOIN users u ON pd.dispensed_by = u.id " +
                     "ORDER BY pd.dispensed_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                DispensedModel d = new DispensedModel();
                d.id          = rs.getInt("id");
                d.patientName = rs.getString("p_name");
                d.medicine    = rs.getString("medicine_name");
                d.dispensedBy = rs.getString("by_name");
                d.dispensedAt = rs.getString("dispensed_at");
                dispensedList.add(d);
            }
        } catch (Exception e) {
            utils.AlertHelper.showError("Load Error", "Failed to load dispensing history:\n" + e.getMessage());
        }
        dispensedTable.setItems(dispensedList);
    }

    private void checkStock(String medicineName) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT stock_quantity FROM medicines WHERE name = ?")) {
            ps.setString(1, medicineName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int qty = rs.getInt("stock_quantity");
                    stockLabel.setText("Stock: " + qty + " units available");
                    stockLabel.setStyle(qty < 10
                        ? "-fx-text-fill: #E71D36; -fx-font-weight: 700;"
                        : "-fx-text-fill: #2A9D8F; -fx-font-weight: 700;");
                } else {
                    stockLabel.setText("⚠ Medicine not found in inventory");
                    stockLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-weight: 700;");
                }
            }
        } catch (Exception e) {
            stockLabel.setText("");
        }
    }

    private void setupSearch() {
        FilteredList<PrescriptionModel> filtered = new FilteredList<>(pendingList, p -> true);
        searchField.textProperty().addListener((obs, old, val) -> filtered.setPredicate(pm -> {
            if (val == null || val.isEmpty()) return true;
            String f = val.toLowerCase();
            return pm.patientName.toLowerCase().contains(f) || pm.medicine.toLowerCase().contains(f);
        }));
        pendingTable.setItems(filtered);
    }

    public void handleDispense() {
        PrescriptionModel selected = pendingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            utils.AlertHelper.showWarning("No Selection", "Please select a prescription to dispense.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Dispense \"" + selected.medicine + "\" for " + selected.patientName + "?",
            ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Confirm Dispensing");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    conn.setAutoCommit(false);
                    try {
                        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO prescription_dispensing (prescription_id, dispensed_by) VALUES (?, ?)")) {
                            ps.setInt(1, selected.id);
                            ps.setInt(2, Session.getInstance().getUserId());
                            ps.executeUpdate();
                        }

                        try (PreparedStatement psStock = conn.prepareStatement(
                            "UPDATE medicines SET stock_quantity = stock_quantity - 1 " +
                            "WHERE id = (SELECT id FROM (SELECT id FROM medicines " +
                            "WHERE name = ? AND stock_quantity > 0 " +
                            "ORDER BY expiry_date ASC LIMIT 1) AS tmp)")) {
                            psStock.setString(1, selected.medicine);
                            int updated = psStock.executeUpdate();

                            if (updated == 0) {
                                utils.AlertHelper.showWarning("Out of Stock", "This medicine is currently out of stock in all batches.");
                                conn.rollback();
                                return;
                            }
                        }

                        conn.commit();
                        utils.AlertHelper.showSuccess("Dispensed",
                            selected.medicine + " dispensed successfully to " + selected.patientName + ".");
                        loadPending();
                        loadDispensed();
                        socket.NotificationClient.sendMessage("REFRESH_PRESCRIPTIONS");
                        stockLabel.setText("");
                    } catch (Exception e) {
                        conn.rollback();
                        throw e;
                    } finally {
                        conn.setAutoCommit(true);
                    }
                } catch (Exception e) {
                    utils.AlertHelper.showError("Dispense Error", "Failed to dispense prescription:\n" + e.getMessage());
                }
            }
        });
    }

    public void showPrescriptions() {
        SceneManager.switchTo((Stage) rootPane.getScene().getWindow(), new PharmacyDashboardController().getView());
    }

    public void showInventory() {
        SceneManager.switchTo((Stage) rootPane.getScene().getWindow(), new PharmacyInventoryController().getView());
    }

    public void showPos() {
        SceneManager.switchTo((Stage) rootPane.getScene().getWindow(), new PharmacyPosController().getView());
    }

    public void showSales() {
        SceneManager.switchTo((Stage) rootPane.getScene().getWindow(), new PharmacySalesController().getView());
    }

    public void showSuppliers() {
        SceneManager.switchTo((Stage) rootPane.getScene().getWindow(), new PharmacySuppliersController().getView());
    }

    public void handleLogout() {
        Session.getInstance().clear();
        SceneManager.switchTo((Stage) rootPane.getScene().getWindow(), new LoginController().getView());
    }
}
