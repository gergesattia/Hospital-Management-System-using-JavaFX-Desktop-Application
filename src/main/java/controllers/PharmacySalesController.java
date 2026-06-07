package controllers;

import dao.SaleDAO;
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
import models.Sale;
import utils.SceneManager;
import java.time.format.DateTimeFormatter;

public class PharmacySalesController {

    private BorderPane rootPane;
    private Label dailyRevenueLabel;
    private Label dailyProfitLabel;
    private Label monthlyRevenueLabel;
    
    private TableView<Sale> salesTable;
    private TableColumn<Sale, Integer> idCol;
    private TableColumn<Sale, String> patientCol;
    private TableColumn<Sale, Double> totalCol;
    private TableColumn<Sale, String> methodCol;
    private TableColumn<Sale, String> dateCol;
    private TableColumn<Sale, String> pharmacistCol;

    private TextField searchField;

    private SaleDAO saleDAO = new SaleDAO();
    private ObservableList<Sale> saleList = FXCollections.observableArrayList();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public Parent getView() {
        rootPane = new BorderPane();
        rootPane.setStyle("-fx-background-color: #F4F7F6;");

        // Top Bar
        HBox topBar = new HBox();
        topBar.setStyle("-fx-background-color: white; -fx-padding: 15 30; -fx-alignment: CENTER_LEFT; -fx-border-color: #D1D1D1; -fx-border-width: 0 0 1 0;");
        Label title = new Label("Sales History & Analytics");
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
        sidebar.getChildren().add(createSidebarButton("📊  Sales", e -> showSales(), true));
        sidebar.getChildren().add(createSidebarButton("🏭  Suppliers", e -> showSuppliers(), false));
        
        rootPane.setLeft(sidebar);

        // Center Content
        VBox centerBox = new VBox(25);
        centerBox.setPadding(new Insets(25));

        // Stats Cards
        HBox statsHBox = new HBox(20);
        dailyRevenueLabel = new Label("$0.00");
        dailyProfitLabel = new Label("$0.00");
        monthlyRevenueLabel = new Label("$0.00");

        statsHBox.getChildren().addAll(
            createStatCard("Today's Revenue", dailyRevenueLabel, "#334257"),
            createStatCard("Today's Profit", dailyProfitLabel, "#2A9D8F"),
            createStatCard("Monthly Revenue", monthlyRevenueLabel, "#548CA8")
        );

        // Sales Table Container
        VBox tableContainer = new VBox(15);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);
        tableContainer.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; -fx-border-color: #D1D1D1; -fx-border-width: 1;");
        
        HBox tableHeader = new HBox();
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        Label tableTitle = new Label("Transaction Log");
        tableTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #334257;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        searchField = new TextField();
        searchField.setPromptText("Search by patient...");
        searchField.setStyle("-fx-pref-width: 250; -fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8;");
        
        tableHeader.getChildren().addAll(tableTitle, spacer, searchField);

        salesTable = new TableView<>();
        VBox.setVgrow(salesTable, Priority.ALWAYS);
        salesTable.setStyle("-fx-background-color: transparent;");
        salesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        idCol = new TableColumn<>("ID");
        patientCol = new TableColumn<>("Patient Name");
        totalCol = new TableColumn<>("Total");
        methodCol = new TableColumn<>("Method");
        dateCol = new TableColumn<>("Date & Time");
        pharmacistCol = new TableColumn<>("Pharmacist");

        salesTable.getColumns().addAll(idCol, patientCol, totalCol, methodCol, dateCol, pharmacistCol);
        tableContainer.getChildren().addAll(tableHeader, salesTable);

        centerBox.getChildren().addAll(statsHBox, tableContainer);
        rootPane.setCenter(centerBox);

        initialize();
        return rootPane;
    }

    private VBox createStatCard(String title, Label valueLabel, String valueColor) {
        VBox card = new VBox(10);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 10, 0, 0, 4);");
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #6C757D; -fx-font-weight: 600;");
        
        valueLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: " + valueColor + ";");
        
        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
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
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        patientCol.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        methodCol.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        pharmacistCol.setCellValueFactory(new PropertyValueFactory<>("creatorName"));
        
        dateCol.setCellValueFactory(data -> {
            return new javafx.beans.property.SimpleStringProperty(data.getValue().getCreatedAt().format(formatter));
        });

        loadData();
        setupSearch();
    }

    private void loadData() {
        saleList.setAll(saleDAO.getAll());
        salesTable.setItems(saleList);
        
        dailyRevenueLabel.setText("$" + String.format(java.util.Locale.US, "%.2f", saleDAO.getDailyRevenue()));
        dailyProfitLabel.setText("$" + String.format(java.util.Locale.US, "%.2f", saleDAO.getDailyProfit()));
        monthlyRevenueLabel.setText("$" + String.format(java.util.Locale.US, "%.2f", saleDAO.getMonthlyRevenue()));
    }

    private void setupSearch() {
        FilteredList<Sale> filteredData = new FilteredList<>(saleList, p -> true);
        searchField.textProperty().addListener((obs, old, newVal) -> {
            filteredData.setPredicate(sale -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lowerCaseFilter = newVal.toLowerCase();
                return sale.getPatientName().toLowerCase().contains(lowerCaseFilter);
            });
        });
        salesTable.setItems(filteredData);
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
