package controllers;

import dao.DatabaseConnection;
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
import utils.SceneManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;

public class PharmacyInventoryController {

    public static class MedicineModel {
        public int id;
        public String name, supplier, expiryDate;
        public int stockQuantity;
        public double unitPrice;

        public int getId()          { return id; }
        public String getName()     { return name; }
        public int getStockQuantity(){ return stockQuantity; }
        public double getUnitPrice(){ return unitPrice; }
        public String getSupplier() { return supplier; }
        public String getExpiryDate(){ return expiryDate; }
    }

    private BorderPane rootPane;
    private TextField searchField;
    private TableView<MedicineModel> medicinesTable;

    private TableColumn<MedicineModel, String>  nameCol;
    private TableColumn<MedicineModel, Integer> qtyCol;
    private TableColumn<MedicineModel, Double>  priceCol;
    private TableColumn<MedicineModel, String>  supplierCol;
    private TableColumn<MedicineModel, String>  expiryCol;

    private TextField  nameField;
    private TextField  quantityField;
    private TextField  priceField;
    private TextField  supplierField;
    private DatePicker expiryPicker;

    private final ObservableList<MedicineModel> medicineList = FXCollections.observableArrayList();

    public Parent getView() {
        rootPane = new BorderPane();
        rootPane.setStyle("-fx-background-color: #EEEEEE;");

        // Top Bar
        HBox topBar = new HBox();
        topBar.setStyle("-fx-background-color: white; -fx-padding: 15 30; -fx-alignment: CENTER_LEFT; -fx-border-color: #D1D1D1; -fx-border-width: 0 0 1 0;");
        Label title = new Label("Medicine Inventory");
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
        sidebar.getChildren().add(createSidebarButton("💊  Prescriptions", e -> showPrescriptions(), false));
        sidebar.getChildren().add(createSidebarButton("📦  Inventory", e -> showInventory(), true));
        sidebar.getChildren().add(createSidebarButton("💰  POS", e -> showPos(), false));
        sidebar.getChildren().add(createSidebarButton("📊  Sales History", e -> showSales(), false));
        sidebar.getChildren().add(createSidebarButton("🏭  Suppliers", e -> showSuppliers(), false));
        rootPane.setLeft(sidebar);

        // Center Content
        HBox mainContent = new HBox(30);
        mainContent.setPadding(new Insets(30));

        // Left Side: Inventory Table
        VBox tableSection = new VBox(15);
        HBox.setHgrow(tableSection, Priority.ALWAYS);
        tableSection.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 25; -fx-border-color: #D1D1D1; -fx-border-width: 1;");
        
        HBox tableHeader = new HBox(10);
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        Label tableTitle = new Label("Medicine Stock");
        tableTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #334257;");
        HBox.setHgrow(tableTitle, Priority.ALWAYS);
        tableTitle.setMaxWidth(Double.MAX_VALUE);
        
        searchField = new TextField();
        searchField.setPromptText("Search name or supplier...");
        searchField.setPrefWidth(250);
        searchField.setStyle("-fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8; -fx-padding: 8;");
        
        Button refreshBtn = new Button("🔄");
        refreshBtn.setOnAction(e -> loadMedicines());
        refreshBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #548CA8; -fx-text-fill: #548CA8; -fx-padding: 5 10; -fx-background-radius: 8; -fx-cursor: hand;");
        
        tableHeader.getChildren().addAll(tableTitle, searchField, refreshBtn);

        medicinesTable = new TableView<>();
        VBox.setVgrow(medicinesTable, Priority.ALWAYS);
        medicinesTable.setStyle("-fx-background-color: white; -fx-border-color: transparent;");
        medicinesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        nameCol = new TableColumn<>("Medicine Name");
        qtyCol = new TableColumn<>("Qty");
        priceCol = new TableColumn<>("Unit Price");
        supplierCol = new TableColumn<>("Supplier");
        expiryCol = new TableColumn<>("Expiry Date");

        medicinesTable.getColumns().addAll(nameCol, qtyCol, priceCol, supplierCol, expiryCol);
        tableSection.getChildren().addAll(tableHeader, medicinesTable);

        // Right Side: Details Form
        VBox formSection = new VBox(20);
        formSection.setMinWidth(380);
        formSection.setMaxWidth(380);
        formSection.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 25; -fx-border-color: #D1D1D1; -fx-border-width: 1;");
        
        Label formTitle = new Label("Medicine Details");
        formTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #334257;");

        VBox fieldsBox = new VBox(15);
        VBox.setVgrow(fieldsBox, Priority.ALWAYS);
        
        nameField = createStyledTextField("e.g. Paracetamol 500mg");
        quantityField = createStyledTextField("0");
        priceField = createStyledTextField("0.00");
        supplierField = createStyledTextField("Supplier name");
        expiryPicker = new DatePicker();
        expiryPicker.setMaxWidth(Double.MAX_VALUE);
        expiryPicker.setStyle("-fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8; -fx-padding: 5;");

        fieldsBox.getChildren().addAll(
            createFormField("Medicine Name", nameField),
            createFormField("Quantity", quantityField),
            createFormField("Unit Price (EGP)", priceField),
            createFormField("Supplier", supplierField),
            createFormField("Expiry Date", expiryPicker)
        );

        VBox actionsBox = new VBox(10);
        HBox topActions = new HBox(10);
        Button addBtn = new Button("Add Medicine");
        addBtn.setOnAction(e -> handleAdd());
        HBox.setHgrow(addBtn, Priority.ALWAYS);
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setStyle("-fx-background-color: #548CA8; -fx-text-fill: white; -fx-font-weight: 800; -fx-padding: 12; -fx-background-radius: 10; -fx-cursor: hand;");

        Button updateBtn = new Button("Update");
        updateBtn.setOnAction(e -> handleUpdate());
        HBox.setHgrow(updateBtn, Priority.ALWAYS);
        updateBtn.setMaxWidth(Double.MAX_VALUE);
        updateBtn.setStyle("-fx-background-color: white; -fx-text-fill: #548CA8; -fx-border-color: #548CA8; -fx-border-width: 1.5; -fx-padding: 10; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-weight: 700;");
        
        topActions.getChildren().addAll(addBtn, updateBtn);

        Button deleteBtn = new Button("Delete Medicine");
        deleteBtn.setOnAction(e -> handleDelete());
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #E71D36; -fx-border-color: #E71D36; -fx-border-width: 1.5; -fx-padding: 10; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-weight: 700;");

        actionsBox.getChildren().addAll(topActions, deleteBtn);
        formSection.getChildren().addAll(formTitle, fieldsBox, actionsBox);

        mainContent.getChildren().addAll(tableSection, formSection);
        rootPane.setCenter(mainContent);

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
        socket.NotificationClient.setRefreshCallback(this::loadMedicines);
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        supplierCol.setCellValueFactory(new PropertyValueFactory<>("supplier"));
        expiryCol.setCellValueFactory(new PropertyValueFactory<>("expiryDate"));

        medicinesTable.setRowFactory(tv -> new TableRow<MedicineModel>() {
            @Override
            protected void updateItem(MedicineModel item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null && item.stockQuantity < 10) {
                    setStyle("-fx-background-color: #FFF0F0;");
                } else {
                    setStyle("");
                }
            }
        });

        medicinesTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                nameField.setText(sel.name);
                quantityField.setText(String.valueOf(sel.stockQuantity));
                priceField.setText(String.valueOf(sel.unitPrice));
                supplierField.setText(sel.supplier != null ? sel.supplier : "");
                if (sel.expiryDate != null && !sel.expiryDate.isEmpty()) {
                    try { expiryPicker.setValue(LocalDate.parse(sel.expiryDate)); }
                    catch (Exception ignored) {}
                }
            }
        });

        loadMedicines();
        setupSearch();
    }

    public void loadMedicines() {
        medicineList.clear();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, stock_quantity, unit_price, supplier, expiry_date FROM medicines ORDER BY name")) {
            while (rs.next()) {
                MedicineModel m = new MedicineModel();
                m.id            = rs.getInt("id");
                m.name          = rs.getString("name");
                m.stockQuantity = rs.getInt("stock_quantity");
                m.unitPrice     = rs.getDouble("unit_price");
                m.supplier      = rs.getString("supplier");
                m.expiryDate    = rs.getString("expiry_date");
                medicineList.add(m);
            }
        } catch (Exception e) {
            utils.AlertHelper.showError("Load Error", "Failed to load medicines:\n" + e.getMessage());
        }
        medicinesTable.setItems(medicineList);
    }

    private void setupSearch() {
        FilteredList<MedicineModel> filtered = new FilteredList<>(medicineList, p -> true);
        searchField.textProperty().addListener((obs, old, val) -> filtered.setPredicate(m -> {
            if (val == null || val.isEmpty()) return true;
            String f = val.toLowerCase();
            return m.name.toLowerCase().contains(f) ||
                   (m.supplier != null && m.supplier.toLowerCase().contains(f));
        }));
        medicinesTable.setItems(filtered);
    }

    public void handleAdd() {
        if (nameField.getText().trim().isEmpty()) {
            utils.AlertHelper.showWarning("Missing Field", "Medicine name is required.");
            return;
        }
        if (expiryPicker.getValue() != null && expiryPicker.getValue().isBefore(LocalDate.now())) {
            utils.AlertHelper.showError("Invalid Date", "Expiry date cannot be in the past.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String name = nameField.getText().trim();
            int qty = Integer.parseInt(normalizeArabicDigits(quantityField.getText().trim()));
            double price = Double.parseDouble(normalizeArabicDigits(priceField.getText().trim().isEmpty() ? "0" : priceField.getText().trim()));
            String supplier = supplierField.getText().trim();
            String expiry = expiryPicker.getValue() != null ? expiryPicker.getValue().toString() : null;

            try (PreparedStatement psCheck = conn.prepareStatement(
                "SELECT id FROM medicines WHERE name = ? AND (expiry_date = ? OR (expiry_date IS NULL AND ? IS NULL))")) {
                psCheck.setString(1, name);
                psCheck.setString(2, expiry);
                psCheck.setString(3, expiry);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        int existingId = rs.getInt("id");
                        try (PreparedStatement psUpd = conn.prepareStatement(
                            "UPDATE medicines SET stock_quantity = stock_quantity + ?, unit_price = ?, supplier = ? WHERE id = ?")) {
                            psUpd.setInt(1, qty);
                            psUpd.setDouble(2, price);
                            psUpd.setString(3, supplier);
                            psUpd.setInt(4, existingId);
                            psUpd.executeUpdate();
                            utils.AlertHelper.showSuccess("Stock Updated", "Added quantity to existing batch of " + name);
                        }
                    } else {
                        try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO medicines (name, stock_quantity, unit_price, supplier, expiry_date) VALUES (?, ?, ?, ?, ?)")) {
                            ps.setString(1, name);
                            ps.setInt(2, qty);
                            ps.setDouble(3, price);
                            ps.setString(4, supplier);
                            ps.setString(5, expiry);
                            ps.executeUpdate();
                            utils.AlertHelper.showSuccess("Success", "New medicine batch added to inventory.");
                        }
                    }
                }
            }

            clearForm();
            loadMedicines();
            socket.NotificationClient.sendMessage("REFRESH_INVENTORY");
        } catch (NumberFormatException e) {
            utils.AlertHelper.showError("Invalid Input", "Quantity must be a whole number.");
        } catch (Exception e) {
            utils.AlertHelper.showError("Add Error", "Failed to add medicine:\n" + e.getMessage());
        }
    }

    public void handleUpdate() {
        MedicineModel selected = medicinesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            utils.AlertHelper.showWarning("No Selection", "Please select a medicine to update.");
            return;
        }
        if (expiryPicker.getValue() != null && expiryPicker.getValue().isBefore(LocalDate.now())) {
            utils.AlertHelper.showError("Invalid Date", "Expiry date cannot be in the past.");
            return;
        }
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE medicines SET name=?, stock_quantity=?, unit_price=?, supplier=?, expiry_date=? WHERE id=?")) {
            int qty = Integer.parseInt(normalizeArabicDigits(quantityField.getText().trim()));
            double price = Double.parseDouble(normalizeArabicDigits(priceField.getText().trim().isEmpty() ? "0" : priceField.getText().trim()));

            ps.setString(1, nameField.getText().trim());
            ps.setInt(2, qty);
            ps.setDouble(3, price);
            ps.setString(4, supplierField.getText().trim());
            ps.setString(5, expiryPicker.getValue() != null ? expiryPicker.getValue().toString() : null);
            ps.setInt(6, selected.id);
            ps.executeUpdate();

            utils.AlertHelper.showSuccess("Updated", "Medicine updated successfully.");
            clearForm();
            loadMedicines();
            socket.NotificationClient.sendMessage("REFRESH_INVENTORY");
        } catch (NumberFormatException e) {
            utils.AlertHelper.showError("Invalid Input", "Quantity must be a whole number.");
        } catch (Exception e) {
            utils.AlertHelper.showError("Update Error", "Failed to update medicine:\n" + e.getMessage());
        }
    }

    private String normalizeArabicDigits(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= '\u0660' && c <= '\u0669') {
                sb.append((char) ('0' + (c - '\u0660')));
            } else if (c >= '\u06f0' && c <= '\u06f9') {
                sb.append((char) ('0' + (c - '\u06f0')));
            } else if (c == '٫' || c == ',') {
                sb.append('.');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public void handleDelete() {
        MedicineModel selected = medicinesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            utils.AlertHelper.showWarning("No Selection", "Please select a medicine to delete.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete \"" + selected.name + "\" from inventory?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement("DELETE FROM medicines WHERE id=?")) {
                    ps.setInt(1, selected.id);
                    ps.executeUpdate();
                    utils.AlertHelper.showSuccess("Deleted", "Medicine removed from inventory.");
                    clearForm();
                    loadMedicines();
                    socket.NotificationClient.sendMessage("REFRESH_INVENTORY");
                } catch (Exception e) {
                    utils.AlertHelper.showError("Delete Error", "Failed to delete medicine:\n" + e.getMessage());
                }
            }
        });
    }

    private void clearForm() {
        nameField.clear(); quantityField.clear();
        priceField.clear(); supplierField.clear();
        expiryPicker.setValue(null);
        medicinesTable.getSelectionModel().clearSelection();
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
        utils.Session.getInstance().clear();
        SceneManager.switchTo((Stage) rootPane.getScene().getWindow(), new LoginController().getView());
    }
}
