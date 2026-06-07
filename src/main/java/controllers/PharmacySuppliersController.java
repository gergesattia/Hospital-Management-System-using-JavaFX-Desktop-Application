package controllers;

import dao.SupplierDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import models.Supplier;
import utils.AlertHelper;
import utils.SceneManager;
import utils.ValidationUtils;

public class PharmacySuppliersController {

    private BorderPane rootPane;
    private TableView<Supplier> supplierTable;
    private TableColumn<Supplier, Integer> idCol;
    private TableColumn<Supplier, String> nameCol;
    private TableColumn<Supplier, String> phoneCol;
    private TableColumn<Supplier, String> addressCol;

    private TextField nameField;
    private TextField phoneField;
    private TextArea addressField;

    private SupplierDAO supplierDAO = new SupplierDAO();
    private ObservableList<Supplier> supplierList = FXCollections.observableArrayList();

    public Parent getView() {
        rootPane = new BorderPane();
        rootPane.setStyle("-fx-background-color: #F4F7F6;");

        // Top Bar
        HBox topBar = new HBox();
        topBar.setStyle("-fx-background-color: white; -fx-padding: 15 30; -fx-alignment: CENTER_LEFT; -fx-border-color: #D1D1D1; -fx-border-width: 0 0 1 0;");
        Label title = new Label("Suppliers Management");
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
        sidebar.getChildren().add(createSidebarButton("📦  Inventory", e -> showInventory(), false));
        sidebar.getChildren().add(createSidebarButton("💰  POS", e -> showPos(), false));
        sidebar.getChildren().add(createSidebarButton("📊  Sales", e -> showSales(), false));
        sidebar.getChildren().add(createSidebarButton("🏭  Suppliers", e -> showSuppliers(), true));
        
        rootPane.setLeft(sidebar);

        // Center Content
        HBox mainContent = new HBox(25);
        mainContent.setPadding(new Insets(25));

        // Left Side: Table
        VBox tableSection = new VBox(15);
        HBox.setHgrow(tableSection, Priority.ALWAYS);
        tableSection.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; -fx-border-color: #D1D1D1; -fx-border-width: 1;");
        
        Label tableLabel = new Label("Supplier List");
        tableLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #334257;");
        
        supplierTable = new TableView<>();
        VBox.setVgrow(supplierTable, Priority.ALWAYS);
        supplierTable.setStyle("-fx-background-color: transparent;");
        supplierTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        idCol = new TableColumn<>("ID");
        idCol.setPrefWidth(60);
        nameCol = new TableColumn<>("Supplier Name");
        nameCol.setPrefWidth(200);
        phoneCol = new TableColumn<>("Phone");
        phoneCol.setPrefWidth(150);
        addressCol = new TableColumn<>("Address");
        addressCol.setPrefWidth(250);

        supplierTable.getColumns().addAll(idCol, nameCol, phoneCol, addressCol);
        tableSection.getChildren().addAll(tableLabel, supplierTable);

        // Right Side: Form
        VBox formSection = new VBox(20);
        formSection.setMinWidth(350);
        formSection.setMaxWidth(350);
        formSection.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; -fx-border-color: #D1D1D1; -fx-border-width: 1;");
        
        Label formTitle = new Label("Supplier Details");
        formTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #334257;");

        VBox fieldsBox = new VBox(15);
        nameField = createStyledTextField("Company Name");
        phoneField = createStyledTextField("+1 234...");
        addressField = new TextArea();
        addressField.setPromptText("Office address");
        addressField.setPrefHeight(100);
        addressField.setStyle("-fx-control-inner-background: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8;");

        fieldsBox.getChildren().addAll(
            createFormField("Name", nameField),
            createFormField("Phone", phoneField),
            createFormField("Address", addressField)
        );

        VBox actionsBox = new VBox(10);
        actionsBox.setAlignment(Pos.BOTTOM_CENTER);
        VBox.setVgrow(actionsBox, Priority.ALWAYS);

        Button addBtn = new Button("Add New Supplier");
        addBtn.setOnAction(e -> handleAdd());
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setStyle("-fx-background-color: #548CA8; -fx-text-fill: white; -fx-font-weight: 700; -fx-padding: 12; -fx-background-radius: 8; -fx-cursor: hand;");

        HBox subActions = new HBox(10);
        Button updateBtn = new Button("Update");
        updateBtn.setOnAction(e -> handleUpdate());
        HBox.setHgrow(updateBtn, Priority.ALWAYS);
        updateBtn.setMaxWidth(Double.MAX_VALUE);
        updateBtn.setStyle("-fx-background-color: white; -fx-text-fill: #548CA8; -fx-border-color: #548CA8; -fx-border-width: 1; -fx-padding: 10; -fx-background-radius: 8; -fx-cursor: hand;");

        Button deleteBtn = new Button("Delete");
        deleteBtn.setOnAction(e -> handleDelete());
        HBox.setHgrow(deleteBtn, Priority.ALWAYS);
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #E71D36; -fx-border-color: #E71D36; -fx-border-width: 1; -fx-padding: 10; -fx-background-radius: 8; -fx-cursor: hand;");

        subActions.getChildren().addAll(updateBtn, deleteBtn);
        actionsBox.getChildren().addAll(addBtn, subActions);

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
        l.setStyle("-fx-text-fill: #6C757D; -fx-font-weight: 600;");
        v.getChildren().addAll(l, field);
        return v;
    }

    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        addressCol.setCellValueFactory(new PropertyValueFactory<>("address"));

        loadSuppliers();

        supplierTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                nameField.setText(newVal.getName());
                phoneField.setText(newVal.getPhone());
                addressField.setText(newVal.getAddress());
            }
        });
    }

    private void loadSuppliers() {
        supplierList.setAll(supplierDAO.getAll());
        supplierTable.setItems(supplierList);
    }

    public void handleAdd() {
        if (validate(-1)) {
            Supplier s = new Supplier();
            s.setName(nameField.getText().trim());
            s.setPhone(phoneField.getText().trim());
            s.setAddress(addressField.getText().trim());
            
            supplierDAO.insert(s);
            AlertHelper.showSuccess("Success", "Supplier added successfully.");
            clear();
            loadSuppliers();
        }
    }

    public void handleUpdate() {
        Supplier selected = supplierTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showError("No Selection", "Please select a supplier to update.");
            return;
        }

        if (validate(selected.getId())) {
            selected.setName(nameField.getText().trim());
            selected.setPhone(phoneField.getText().trim());
            selected.setAddress(addressField.getText().trim());
            
            supplierDAO.update(selected);
            AlertHelper.showSuccess("Success", "Supplier updated successfully.");
            loadSuppliers();
        }
    }

    public void handleDelete() {
        Supplier selected = supplierTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showError("No Selection", "Please select a supplier to delete.");
            return;
        }

        if (AlertHelper.showConfirm("Confirm Delete", "Are you sure you want to delete " + selected.getName() + "?")) {
            supplierDAO.delete(selected.getId());
            clear();
            loadSuppliers();
        }
    }

    private boolean validate(int excludeId) {
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();

        if (name.isEmpty()) {
            AlertHelper.showError("Validation Error", "Supplier name is required.");
            return false;
        }

        if (!ValidationUtils.isValidPhone(phone)) {
            AlertHelper.showError("Validation Error", "Phone number must be 11 digits and start with '01'.");
            return false;
        }

        if (supplierDAO.isPhoneExists(phone, excludeId)) {
            AlertHelper.showError("Validation Error", "This phone number is already registered to another supplier.");
            return false;
        }

        return true;
    }

    private void clear() {
        nameField.clear();
        phoneField.clear();
        addressField.clear();
        supplierTable.getSelectionModel().clearSelection();
    }

    public void showDashboard() {
        SceneManager.switchTo((Stage) rootPane.getScene().getWindow(), new PharmacyDashboardController().getView());
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
