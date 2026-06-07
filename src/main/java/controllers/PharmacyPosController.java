package controllers;

import dao.DatabaseConnection;
import dao.MedicineDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import models.CartItem;
import models.Medicine;
import utils.AlertHelper;
import utils.SceneManager;
import utils.Session;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PharmacyPosController {

    private BorderPane rootPane;
    private Label dateLabel;
    private TextField searchField;
    private FlowPane medicineContainer;
    
    private TableView<CartItem> cartTable;
    private TableColumn<CartItem, String> itemCol;
    private TableColumn<CartItem, Integer> qtyCol;
    private TableColumn<CartItem, Double> priceCol;
    private TableColumn<CartItem, Void> actionCol;

    private TextField patientNameField;
    private Label subtotalLabel;
    private TextField discountField;
    private Label totalLabel;

    private MedicineDAO medicineDAO = new MedicineDAO();
    private ObservableList<CartItem> cartItems = FXCollections.observableArrayList();

    public Parent getView() {
        rootPane = new BorderPane();
        rootPane.setStyle("-fx-background-color: #F4F7F6;");

        // Top Bar
        HBox topBar = new HBox();
        topBar.setStyle("-fx-background-color: white; -fx-padding: 15 30; -fx-alignment: CENTER_LEFT; -fx-border-color: #D1D1D1; -fx-border-width: 0 0 1 0;");
        
        VBox titleBox = new VBox(2);
        Label title = new Label("Pharmacy POS");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: #334257;");
        dateLabel = new Label("Date");
        dateLabel.setStyle("-fx-text-fill: #6C757D; -fx-font-size: 13px;");
        titleBox.getChildren().addAll(title, dateLabel);
        
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        
        Button signOut = new Button("Sign Out");
        signOut.setOnAction(e -> handleLogout());
        signOut.setStyle("-fx-background-color: white; -fx-text-fill: #548CA8; -fx-padding: 10 20; -fx-background-radius: 10; -fx-cursor: hand; -fx-border-color: #548CA8; -fx-border-width: 1.5; -fx-font-weight: 600;");
        
        topBar.getChildren().addAll(titleBox, signOut);
        rootPane.setTop(topBar);

        // Sidebar
        VBox sidebar = new VBox(12);
        sidebar.setStyle("-fx-background-color: #334257; -fx-padding: 30 15; -fx-pref-width: 260;");
        Label sidebarTitle = new Label("MEDICORE");
        sidebarTitle.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: 800; -fx-padding: 0 0 20 5;");
        
        sidebar.getChildren().add(sidebarTitle);
        sidebar.getChildren().add(createSidebarButton("💊  Prescriptions", e -> showPrescriptions(), false));
        sidebar.getChildren().add(createSidebarButton("📦  Inventory", e -> showInventory(), false));
        sidebar.getChildren().add(createSidebarButton("💰  POS", e -> showPos(), true));
        sidebar.getChildren().add(createSidebarButton("📊  Sales History", e -> showSales(), false));
        sidebar.getChildren().add(createSidebarButton("🏭  Suppliers", e -> showSuppliers(), false));
        rootPane.setLeft(sidebar);

        // Center Content
        HBox mainLayout = new HBox(25);
        mainLayout.setPadding(new Insets(25));

        // Left Side: Medicine Selection
        VBox leftSide = new VBox(20);
        HBox.setHgrow(leftSide, Priority.ALWAYS);
        
        HBox searchBar = new HBox(15);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 12; -fx-border-color: #D1D1D1; -fx-border-width: 1;");
        
        searchField = new TextField();
        searchField.setPromptText("Search medicine by name or description...");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.setStyle("-fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8; -fx-padding: 12;");
        
        Button searchBtn = new Button("🔍");
        searchBtn.setStyle("-fx-background-color: #548CA8; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 8;");
        searchBar.getChildren().addAll(searchField, searchBtn);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        
        medicineContainer = new FlowPane(20, 20);
        medicineContainer.setPadding(new Insets(5));
        scroll.setContent(medicineContainer);
        
        leftSide.getChildren().addAll(searchBar, scroll);

        // Right Side: Cart
        VBox rightSide = new VBox(20);
        rightSide.setMinWidth(450);
        rightSide.setMaxWidth(450);
        rightSide.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 25; -fx-border-color: #D1D1D1; -fx-border-width: 1;");
        
        Label cartTitle = new Label("Current Order");
        cartTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #334257;");

        cartTable = new TableView<>();
        VBox.setVgrow(cartTable, Priority.ALWAYS);
        cartTable.setStyle("-fx-background-color: transparent; -fx-border-color: #EEEEEE; -fx-border-width: 0 0 1 0;");
        cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        itemCol = new TableColumn<>("Item");
        qtyCol = new TableColumn<>("Qty");
        priceCol = new TableColumn<>("Price");
        actionCol = new TableColumn<>("");

        cartTable.getColumns().addAll(itemCol, qtyCol, priceCol, actionCol);

        VBox totalsBox = new VBox(15);
        totalsBox.setStyle("-fx-padding: 20 0 0 0; -fx-border-color: #EEEEEE; -fx-border-width: 1 0 0 0;");
        
        patientNameField = new TextField();
        patientNameField.setPromptText("Walk-in Patient");
        patientNameField.setStyle("-fx-pref-width: 200; -fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 5;");
        
        subtotalLabel = new Label("$0.00");
        subtotalLabel.setStyle("-fx-font-weight: 800; -fx-text-fill: #334257;");
        
        discountField = new TextField();
        discountField.setPromptText("0.00");
        discountField.setStyle("-fx-pref-width: 80; -fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 5; -fx-alignment: CENTER_RIGHT;");
        
        totalLabel = new Label("$0.00");
        totalLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: #548CA8;");

        totalsBox.getChildren().addAll(
            createSummaryRow("Patient Name", patientNameField),
            createSummaryRow("Subtotal", subtotalLabel),
            createSummaryRow("Discount", discountField),
            new Separator(),
            createSummaryRow("Total Payable", totalLabel)
        );

        Button completeBtn = new Button("Complete Sale");
        completeBtn.setOnAction(e -> handleCompleteSale());
        completeBtn.setMaxWidth(Double.MAX_VALUE);
        completeBtn.setStyle("-fx-background-color: #548CA8; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: 800; -fx-padding: 18; -fx-background-radius: 12; -fx-cursor: hand;");
        
        Button clearBtn = new Button("Clear Order");
        clearBtn.setOnAction(e -> handleClearOrder());
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6C757D; -fx-font-weight: 600; -fx-cursor: hand;");

        rightSide.getChildren().addAll(cartTitle, cartTable, totalsBox, completeBtn, clearBtn);

        mainLayout.getChildren().addAll(leftSide, rightSide);
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

    private HBox createSummaryRow(String label, javafx.scene.Node node) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: #6C757D; -fx-font-weight: 600;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(l, spacer, node);
        return row;
    }

    public void initialize() {
        socket.NotificationClient.setRefreshCallback(() -> loadMedicines(searchField.getText()));
        dateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd yyyy")));
        
        setupTable();
        loadMedicines("");
        
        searchField.textProperty().addListener((obs, old, newVal) -> loadMedicines(newVal));
        discountField.textProperty().addListener((obs, old, newVal) -> calculateTotals());
    }

    private void setupTable() {
        itemCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getMedicine().getName()));
        qtyCol.setCellValueFactory(data -> data.getValue().quantityProperty().asObject());
        priceCol.setCellValueFactory(data -> data.getValue().priceProperty().asObject());
        
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("🗑");
            {
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #E71D36; -fx-cursor: hand;");
                deleteBtn.setOnAction(event -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    cartItems.remove(item);
                    calculateTotals();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });

        cartTable.setItems(cartItems);
    }

    private void loadMedicines(String query) {
        medicineContainer.getChildren().clear();
        List<Medicine> meds = query.isEmpty() ? medicineDAO.getAllGrouped() : medicineDAO.searchGrouped(query);
        for (Medicine m : meds) {
            medicineContainer.getChildren().add(createMedicineCard(m));
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

    private VBox createMedicineCard(Medicine m) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2); -fx-pref-width: 180;");
        
        Label name = new Label(m.getName());
        name.setStyle("-fx-font-weight: 800; -fx-text-fill: #334257;");
        name.setWrapText(true);
        
        Label price = new Label("$" + String.format(java.util.Locale.US, "%.2f", m.getUnitPrice()));
        price.setStyle("-fx-text-fill: #548CA8; -fx-font-weight: 700;");
        
        Label stock = new Label("Stock: " + m.getStockQuantity());
        stock.setStyle("-fx-text-fill: " + (m.getStockQuantity() < 10 ? "#E71D36" : "#6C757D") + "; -fx-font-size: 11px;");
        
        Button addBtn = new Button("Add to Cart");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setStyle("-fx-background-color: #F0F4F8; -fx-text-fill: #548CA8; -fx-font-weight: 700; -fx-cursor: hand;");
        addBtn.setDisable(m.getStockQuantity() <= 0);
        
        addBtn.setOnAction(e -> addToCart(m));
        
        card.getChildren().addAll(name, price, stock, addBtn);
        return card;
    }

    private void addToCart(Medicine m) {
        for (CartItem item : cartItems) {
            if (item.getMedicine().getId() == m.getId()) {
                if (item.getQuantity() < m.getStockQuantity()) {
                    item.setQuantity(item.getQuantity() + 1);
                    calculateTotals();
                } else {
                    AlertHelper.showError("Stock Limit", "Cannot add more. Stock limit reached.");
                }
                return;
            }
        }
        cartItems.add(new CartItem(m, 1));
        calculateTotals();
    }

    private void calculateTotals() {
        double subtotal = 0;
        for (CartItem item : cartItems) subtotal += item.getTotal();
        
        double discount = 0;
        try {
            String discountText = normalizeArabicDigits(discountField.getText().trim());
            discount = Double.parseDouble(discountText);
        } catch (Exception e) {}
        
        double total = subtotal - discount;
        subtotalLabel.setText("$" + String.format(java.util.Locale.US, "%.2f", subtotal));
        totalLabel.setText("$" + String.format(java.util.Locale.US, "%.2f", Math.max(0, total)));
    }

    public void handleCompleteSale() {
        if (cartItems.isEmpty()) {
            AlertHelper.showError("Empty Order", "Please add items to the cart.");
            return;
        }

        String patientName = patientNameField.getText().trim();
        if (patientName.isEmpty()) patientName = "Walk-in Patient";
        
        double subtotal = 0;
        for (CartItem item : cartItems) subtotal += item.getTotal();
        
        double discount = 0;
        try {
            String discountText = normalizeArabicDigits(discountField.getText().trim());
            discount = Double.parseDouble(discountText);
        } catch (Exception e) {}
        
        double total = Math.max(0, subtotal - discount);
        int pharmacistId = Session.getInstance().getUserId();

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int saleId = -1;
                try (PreparedStatement psSale = conn.prepareStatement(
                    "INSERT INTO sales (patient_name, total_amount, discount, payment_method, created_by) VALUES (?, ?, ?, 'Cash', ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                    psSale.setString(1, patientName);
                    psSale.setDouble(2, total);
                    psSale.setDouble(3, discount);
                    psSale.setInt(4, pharmacistId);
                    psSale.executeUpdate();
                    
                    try (ResultSet rsKeys = psSale.getGeneratedKeys()) {
                        if (rsKeys.next()) saleId = rsKeys.getInt(1);
                    }
                }

                if (saleId != -1) {
                    for (CartItem item : cartItems) {
                        try (PreparedStatement psItem = conn.prepareStatement(
                            "INSERT INTO sale_items (sale_id, medicine_id, quantity, price_at_sale) VALUES (?, ?, ?, ?)")) {
                            psItem.setInt(1, saleId);
                            psItem.setInt(2, item.getMedicine().getId());
                            psItem.setInt(3, item.getQuantity());
                            psItem.setDouble(4, item.getPrice());
                            psItem.executeUpdate();
                        }
                        
                        // Internal stock deduction logic (FEFO)
                        deductStockFEFOManual(conn, item.getMedicine().getName(), item.getQuantity());
                        
                        try (PreparedStatement psMove = conn.prepareStatement(
                            "INSERT INTO stock_movements (medicine_id, type, quantity, reference_type, reference_id) VALUES (?, 'out', ?, 'sale', ?)")) {
                            psMove.setInt(1, item.getMedicine().getId());
                            psMove.setInt(2, item.getQuantity());
                            psMove.setInt(3, saleId);
                            psMove.executeUpdate();
                        }
                    }
                }
                conn.commit();
                AlertHelper.showSuccess("Sale Completed", "Transaction processed successfully.");
                handleClearOrder();
                loadMedicines("");
                socket.NotificationClient.sendMessage("REFRESH_INVENTORY");
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            AlertHelper.showError("Transaction Failed", "Could not complete sale: " + e.getMessage());
        }
    }

    private void deductStockFEFOManual(Connection conn, String name, int qtyToDeduct) throws SQLException {
        String sql = "SELECT id, stock_quantity FROM medicines WHERE name = ? AND stock_quantity > 0 ORDER BY expiry_date ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                int remaining = qtyToDeduct;
                while (rs.next() && remaining > 0) {
                    int batchId = rs.getInt("id");
                    int batchStock = rs.getInt("stock_quantity");
                    int take = Math.min(batchStock, remaining);
                    
                    try (PreparedStatement psUpd = conn.prepareStatement("UPDATE medicines SET stock_quantity = stock_quantity - ? WHERE id = ?")) {
                        psUpd.setInt(1, take);
                        psUpd.setInt(2, batchId);
                        psUpd.executeUpdate();
                    }
                    remaining -= take;
                }
                if (remaining > 0) {
                    throw new SQLException("Insufficient stock for " + name);
                }
            }
        }
    }

    public void handleClearOrder() {
        cartItems.clear();
        patientNameField.clear();
        discountField.clear();
        calculateTotals();
    }

    public void handleLogout() {
        utils.Session.getInstance().clear();
        SceneManager.switchTo((Stage) rootPane.getScene().getWindow(), new LoginController().getView());
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
}
