package controllers;

import dao.DatabaseConnection;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import utils.SceneManager;
import utils.Session;
import utils.AlertHelper;
import utils.ExportService;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AdminDashboardController {

    private BorderPane rootPane;
    private VBox dashboardContent;
    private PieChart statusPieChart;
    private StackPane fxglChartContainer;
    private HBox pieLegendBox;
    private HBox barLegendBox;

    private Label totalDoctorsLabel;
    private Label totalPatientsLabel;
    private Label totalAppointmentsLabel;
    private Label revenueLabel;
    private Label profitLabel;

    private Map<String, Integer> fullChartData = new LinkedHashMap<>();
    private Set<String> activeFilters = new HashSet<>();

    public Parent getView() {
        rootPane = new BorderPane();
        rootPane.setStyle("-fx-background-color: #EEEEEE;");

        // Top Bar
        HBox topBar = new HBox();
        topBar.setStyle("-fx-background-color: white; -fx-padding: 15 30; -fx-alignment: CENTER_LEFT; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.03), 10, 0, 0, 4); -fx-border-color: #D1D1D1; -fx-border-width: 0 0 1 0;");
        Label title = new Label("Overview");
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
        sidebar.getChildren().add(createSidebarButton("📊  Dashboard", e -> showDashboard(), true));
        sidebar.getChildren().add(createSidebarButton("👥  User Management", e -> showManageUsers(), false));
        
        Separator sep = new Separator();
        sep.setStyle("-fx-padding: 10 0; -fx-opacity: 0.1;");
        sidebar.getChildren().add(sep);
        
        sidebar.getChildren().add(createSidebarButton("📤  Export Now", e -> handleExport(), false));
        sidebar.getChildren().add(createSidebarButton("🤖  Ask AI (Beta)", e -> handleOpenAiChat(), false));
        rootPane.setLeft(sidebar);

        // Center Content
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        
        dashboardContent = new VBox(30);
        dashboardContent.setPadding(new Insets(30));
        scroll.setContent(dashboardContent);
        rootPane.setCenter(scroll);

        // KPI Cards Row
        HBox kpiRow = new HBox(25);
        kpiRow.setAlignment(Pos.CENTER);
        
        totalDoctorsLabel = new Label("0");
        totalPatientsLabel = new Label("0");
        totalAppointmentsLabel = new Label("0");
        revenueLabel = new Label("$0.00");
        profitLabel = new Label("$0.00");

        kpiRow.getChildren().addAll(
            createKPICard("Total Doctors", totalDoctorsLabel, "👨‍⚕️", "rgba(84, 140, 168, 0.1)"),
            createKPICard("Total Patients", totalPatientsLabel, "🤒", "rgba(46, 196, 182, 0.1)"),
            createKPICard("Appointments", totalAppointmentsLabel, "📅", "rgba(255, 159, 28, 0.1)"),
            createKPICard("Total Revenue", revenueLabel, "💰", "rgba(42, 157, 143, 0.1)"),
            createKPICard("Total Profit", profitLabel, "📈", "rgba(231, 29, 54, 0.1)")
        );
        dashboardContent.getChildren().add(kpiRow);

        // Charts Row
        HBox chartsRow = new HBox(25);
        VBox.setVgrow(chartsRow, Priority.ALWAYS);

        // Pie Chart Box
        VBox pieBox = new VBox(15);
        HBox.setHgrow(pieBox, Priority.ALWAYS);
        pieBox.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 25; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 20, 0, 0, 8); -fx-border-color: #D1D1D1; -fx-border-width: 1;");
        Label pieTitle = new Label("Appointment Distribution");
        pieTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #334257;");
        statusPieChart = new PieChart();
        statusPieChart.setLegendVisible(false);
        VBox.setVgrow(statusPieChart, Priority.ALWAYS);
        pieLegendBox = new HBox(15);
        pieLegendBox.setAlignment(Pos.CENTER);
        pieBox.getChildren().addAll(pieTitle, statusPieChart, pieLegendBox);

        // Bar Chart Box
        VBox barBox = new VBox(15);
        HBox.setHgrow(barBox, Priority.ALWAYS);
        barBox.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 25; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 20, 0, 0, 8); -fx-border-color: #D1D1D1; -fx-border-width: 1;");
        Label barTitle = new Label("Doctor Specializations");
        barTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #334257;");
        fxglChartContainer = new StackPane();
        fxglChartContainer.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8;");
        VBox.setVgrow(fxglChartContainer, Priority.ALWAYS);
        barLegendBox = new HBox(15);
        barLegendBox.setAlignment(Pos.CENTER);
        barBox.getChildren().addAll(barTitle, fxglChartContainer, barLegendBox);

        chartsRow.getChildren().addAll(pieBox, barBox);
        dashboardContent.getChildren().add(chartsRow);

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

    private VBox createKPICard(String title, Label valueLabel, String icon, String iconBg) {
        VBox card = new VBox();
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 15, 0, 0, 5); -fx-border-color: #D1D1D1; -fx-border-width: 1;");
        
        HBox inner = new HBox(15);
        inner.setAlignment(Pos.CENTER_LEFT);
        
        StackPane iconPane = new StackPane(new Label(icon));
        iconPane.setStyle("-fx-background-color: " + iconBg + "; -fx-background-radius: 12; -fx-padding: 12; -fx-font-size: 20px;");
        
        VBox text = new VBox();
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: #476072; -fx-font-size: 14px; -fx-font-weight: 600;");
        valueLabel.setStyle("-fx-text-fill: #334257; -fx-font-size: 24px; -fx-font-weight: 800;");
        text.getChildren().addAll(t, valueLabel);
        
        inner.getChildren().addAll(iconPane, text);
        card.getChildren().add(inner);
        return card;
    }

    public void initialize() {
        if (!"admin".equals(Session.getInstance().getRole())) return;
        loadKPIs();
        loadPieChart();
        loadLineChart();
    }

    private void loadKPIs() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM doctors")) {
                if (rs.next()) totalDoctorsLabel.setText(String.valueOf(rs.getInt(1)));
            }

            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM patients")) {
                if (rs.next()) totalPatientsLabel.setText(String.valueOf(rs.getInt(1)));
            }

            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM appointments")) {
                if (rs.next()) totalAppointmentsLabel.setText(String.valueOf(rs.getInt(1)));
            }

            double revenue = 0.0;
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT SUM(total_amount) FROM sales")) {
                if (rs.next()) revenue += rs.getDouble(1);
            }
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT SUM(consultation_revenue) FROM appointments")) {
                if (rs.next()) revenue += rs.getDouble(1);
            }
            revenueLabel.setText("$" + String.format(java.util.Locale.US, "%.2f", revenue));

            double expenses = 0.0;
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT SUM(salary) FROM users WHERE is_active = 1")) {
                if (rs.next()) expenses += rs.getDouble(1);
            }
            // Add nurse salaries too
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT SUM(salary) FROM nurses WHERE is_active = 1")) {
                if (rs.next()) expenses += rs.getDouble(1);
            }
            
            double profit = revenue - expenses;
            profitLabel.setText("$" + String.format(java.util.Locale.US, "%.2f", profit));
            profitLabel.setStyle("-fx-text-fill: " + (profit >= 0 ? "#2A9D8F" : "#E71D36") + "; -fx-font-size: 24px; -fx-font-weight: 800;");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadPieChart() {
        int critical = 0, normal = 0, waiting = 0, completed = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT priority, status FROM appointments")) {
            while (rs.next()) {
                int p = rs.getInt("priority");
                String s = rs.getString("status");
                if (p >= 4) critical++; else normal++;
                if ("waiting".equals(s) || "in_progress".equals(s)) waiting++;
                if ("completed".equals(s)) completed++;
            }
        } catch (Exception e) { e.printStackTrace(); }

        PieChart.Data d1 = new PieChart.Data("Critical Patients", critical);
        PieChart.Data d2 = new PieChart.Data("Normal Patients", normal);
        PieChart.Data d3 = new PieChart.Data("All Waiting", waiting);
        PieChart.Data d4 = new PieChart.Data("All Completed", completed);

        PieChart.Data[] data = {d1, d2, d3, d4};
        String[] colors = {"#FF0000", "#FFA500", "#0000FF", "#008000"};
        statusPieChart.getData().addAll(data);

        for (int i = 0; i < data.length; i++) {
            PieChart.Data d = data[i];
            String color = colors[i];
            ToggleButton btn = new ToggleButton(d.getName());
            btn.setSelected(true);
            btn.setStyle("-fx-background-color: transparent; -fx-border-color: #D1D1D1; -fx-border-radius: 4; -fx-padding: 5 10; -fx-cursor: hand;");
            Circle dot = new Circle(5, Color.web(color));
            btn.setGraphic(dot);
            btn.setOnAction(e -> {
                if (btn.isSelected()) { if (!statusPieChart.getData().contains(d)) statusPieChart.getData().add(d); }
                else statusPieChart.getData().remove(d);
                applyPieChartColors(data, colors);
            });
            pieLegendBox.getChildren().add(btn);
        }
        applyPieChartColors(data, colors);
    }

    private void applyPieChartColors(PieChart.Data[] all, String[] colors) {
        Platform.runLater(() -> {
            for (int i = 0; i < all.length; i++) {
                if (statusPieChart.getData().contains(all[i]) && all[i].getNode() != null) {
                    all[i].getNode().setStyle("-fx-pie-color: " + colors[i] + ";");
                }
            }
        });
    }

    private void loadLineChart() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT s.name, COUNT(d.id) as count FROM specializations s JOIN doctors d ON s.id = d.specialization_id GROUP BY s.id, s.name")) {
            while (rs.next()) {
                String name = rs.getString("name");
                int count = rs.getInt("count");
                fullChartData.put(name, count);
                activeFilters.add(name);

                ToggleButton btn = new ToggleButton(name);
                btn.setSelected(true);
                btn.setStyle("-fx-background-color: transparent; -fx-border-color: #D1D1D1; -fx-border-radius: 4; -fx-padding: 5 10; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    if (btn.isSelected()) activeFilters.add(name); else activeFilters.remove(name);
                    refreshChart();
                });
                barLegendBox.getChildren().add(btn);
            }
            refreshChart();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void refreshChart() {
        Map<String, Integer> filtered = new LinkedHashMap<>();
        for (String s : activeFilters) filtered.put(s, fullChartData.get(s));
        utils.FXGLChartManager.renderSpecializationChart(fxglChartContainer, filtered);
    }

    public void handleExport() {
        new Thread(() -> {
            try {
                Platform.runLater(() -> AlertHelper.showInfo("Export Started", "Backing up database locally..."));
                String path = ExportService.exportDatabaseToExcel();
                Platform.runLater(() -> AlertHelper.showSuccess("Export Successful", "Saved locally to: " + path));
            } catch (Exception e) {
                Platform.runLater(() -> AlertHelper.showError("Export Failed", e.getMessage()));
            }
        }).start();
    }

    public void handleOpenAiChat() {
        try {
            Stage s = new Stage();
            s.setTitle("MediCore AI Assistant");
            s.setScene(new javafx.scene.Scene(new AiChatController().getView()));
            s.show();
        } catch (Exception e) { utils.AlertHelper.showError("Chat Error", e.getMessage()); }
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
