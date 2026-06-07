package controllers;

import dao.DatabaseConnection;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import utils.SceneManager;
import utils.Session;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReceptionistPrescriptionsController {

    /**
     * Represents one patient visit (medical_record_id).
     * Contains all medicines prescribed in that visit.
     */
    public static class VisitPrescriptionModel {
        public int medicalRecordId;
        public String patientName;
        public String doctorName;
        public String date;
        // All medicines for this visit (each element = one medicine row)
        public List<MedicineEntry> medicines = new ArrayList<>();

        public int getId()           { return medicalRecordId; }
        public String getPatientName() { return patientName; }
        public String getMedicinesSummary() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < medicines.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(medicines.get(i).name);
            }
            return sb.toString();
        }
        public String getDate()       { return date; }
    }

    public static class MedicineEntry {
        public String name;
        public String dosage;
        public String frequency;
        public int    duration;
        public String instructions;
    }

    // ─── UI fields ───────────────────────────────────────────────────────────
    private BorderPane rootPane;
    private TextField  searchField;
    private TableView<VisitPrescriptionModel> prescriptionsTable;
    private TextArea   documentArea;

    private TableColumn<VisitPrescriptionModel, Integer> idCol;
    private TableColumn<VisitPrescriptionModel, String>  patientCol;
    private TableColumn<VisitPrescriptionModel, String>  medicineCol;
    private TableColumn<VisitPrescriptionModel, String>  dateCol;

    private ObservableList<VisitPrescriptionModel> dataList = FXCollections.observableArrayList();

    // ─── Background poller ────────────────────────────────────────────────────
    private ScheduledExecutorService poller;

    // ─────────────────────────────────────────────────────────────────────────
    public Parent getView() {
        rootPane = new BorderPane();
        rootPane.setStyle("-fx-background-color: #EEEEEE;");

        // ── Top Bar ──────────────────────────────────────────────────────────
        HBox topBar = new HBox();
        topBar.setStyle("-fx-background-color: white; -fx-padding: 15 30; -fx-alignment: CENTER_LEFT; -fx-border-color: #D1D1D1; -fx-border-width: 0 0 1 0;");
        Label title = new Label("Reception – Prescriptions");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: #334257;");
        HBox.setHgrow(title, Priority.ALWAYS);
        title.setMaxWidth(Double.MAX_VALUE);

        Button signOut = new Button("Sign Out");
        signOut.setOnAction(e -> handleLogout());
        signOut.setStyle("-fx-background-color: white; -fx-text-fill: #548CA8; -fx-padding: 10 20; -fx-background-radius: 10; -fx-cursor: hand; -fx-border-color: #548CA8; -fx-border-width: 1.5; -fx-font-weight: 600;");

        topBar.getChildren().addAll(title, signOut);
        rootPane.setTop(topBar);

        // ── Sidebar ───────────────────────────────────────────────────────────
        VBox sidebar = new VBox(12);
        sidebar.setStyle("-fx-background-color: #334257; -fx-padding: 30 15; -fx-pref-width: 260;");
        Label sidebarTitle = new Label("MEDICORE");
        sidebarTitle.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: 800; -fx-padding: 0 0 20 5;");

        Button regBtn = new Button("📝  Registration");
        regBtn.setOnAction(e -> showManagePatients());
        regBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ADB5BD; -fx-font-size: 15px; -fx-font-weight: 500; -fx-alignment: CENTER_LEFT; -fx-padding: 12 25; -fx-max-width: 240; -fx-pref-width: 240; -fx-cursor: hand;");

        Button presBtn = new Button("📋  Prescriptions");
        presBtn.setOnAction(e -> showPrescriptions());
        presBtn.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: 600; -fx-alignment: CENTER_LEFT; -fx-padding: 12 25; -fx-max-width: 240; -fx-pref-width: 240; -fx-background-radius: 4; -fx-cursor: hand;");

        sidebar.getChildren().addAll(sidebarTitle, regBtn, presBtn);
        rootPane.setLeft(sidebar);

        // ── Centre: table + preview ───────────────────────────────────────────
        HBox mainContent = new HBox(30);
        mainContent.setPadding(new Insets(30));

        // ── Table section ─────────────────────────────────────────────────────
        VBox tableSection = new VBox(15);
        HBox.setHgrow(tableSection, Priority.ALWAYS);
        tableSection.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 25; -fx-border-color: #D1D1D1; -fx-border-width: 1;");

        HBox tableHeader = new HBox(10);
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        Label tableTitle = new Label("All Prescriptions  (auto-refresh every 5 s)");
        tableTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #334257;");
        HBox.setHgrow(tableTitle, Priority.ALWAYS);
        tableTitle.setMaxWidth(Double.MAX_VALUE);

        searchField = new TextField();
        searchField.setPromptText("Search patient or medicine…");
        searchField.setPrefWidth(250);
        searchField.setStyle("-fx-background-color: #F8F9FE; -fx-border-color: #D1D1D1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8;");

        Button refreshBtn = new Button("🔄");
        refreshBtn.setOnAction(e -> loadData());
        refreshBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #548CA8; -fx-text-fill: #548CA8; -fx-padding: 5 10; -fx-background-radius: 8; -fx-cursor: hand;");

        tableHeader.getChildren().addAll(tableTitle, searchField, refreshBtn);

        prescriptionsTable = new TableView<>();
        VBox.setVgrow(prescriptionsTable, Priority.ALWAYS);
        prescriptionsTable.setStyle("-fx-background-color: white; -fx-border-color: transparent;");
        prescriptionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        prescriptionsTable.setPlaceholder(new Label("No prescriptions found."));

        idCol         = new TableColumn<>("Ref #");       idCol.setPrefWidth(70);
        patientCol    = new TableColumn<>("Patient Name"); patientCol.setPrefWidth(180);
        medicineCol   = new TableColumn<>("Medicines");    medicineCol.setPrefWidth(200);
        dateCol       = new TableColumn<>("Date Issued");  dateCol.setPrefWidth(150);

        prescriptionsTable.getColumns().addAll(idCol, patientCol, medicineCol, dateCol);
        tableSection.getChildren().addAll(tableHeader, prescriptionsTable);

        // ── Preview section ───────────────────────────────────────────────────
        VBox previewSection = new VBox(20);
        previewSection.setMinWidth(350);
        previewSection.setMaxWidth(350);
        previewSection.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 25; -fx-border-color: #D1D1D1; -fx-border-width: 1;");

        Label previewTitle = new Label("Prescription Preview");
        previewTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #334257;");

        documentArea = new TextArea();
        VBox.setVgrow(documentArea, Priority.ALWAYS);
        documentArea.setWrapText(true);
        documentArea.setEditable(false);
        documentArea.setStyle("-fx-font-family: 'Consolas', 'Courier New'; -fx-background-color: #F8F9FE; -fx-font-size: 13px; -fx-control-inner-background: #F8F9FE; -fx-border-color: #EDF2F7; -fx-background-radius: 8; -fx-border-radius: 8; -fx-padding: 10;");

        Button printBtn = new Button("🖨  Print Prescription");
        printBtn.setOnAction(e -> handlePrint());
        printBtn.setMaxWidth(Double.MAX_VALUE);
        printBtn.setStyle("-fx-background-color: #548CA8; -fx-text-fill: white; -fx-font-weight: 800; -fx-padding: 15; -fx-background-radius: 10; -fx-cursor: hand;");

        Button emailBtn = new Button("✉  Email to Patient");
        emailBtn.setMaxWidth(Double.MAX_VALUE);
        emailBtn.setStyle("-fx-background-color: white; -fx-text-fill: #476072; -fx-border-color: #D1D1D1; -fx-border-width: 1.5; -fx-padding: 12; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-weight: 600;");

        previewSection.getChildren().addAll(previewTitle, documentArea, printBtn, emailBtn);

        mainContent.getChildren().addAll(tableSection, previewSection);
        rootPane.setCenter(mainContent);

        initialize();
        return rootPane;
    }

    // ─────────────────────────────────────────────────────────────────────────
    public void initialize() {
        if (!"receptionist".equals(Session.getInstance().getRole())) return;

        idCol.setCellValueFactory(
                c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        patientCol.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getPatientName()));
        medicineCol.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getMedicinesSummary()));
        dateCol.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getDate()));

        prescriptionsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSel, newSel) -> {
                    if (newSel != null) generateDocument(newSel);
                });

        loadData();
        setupSearch();
        startAutoRefresh();

        // Stop poller when the scene is torn down
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) stopAutoRefresh();
        });
    }

    // ─── Data loading ─────────────────────────────────────────────────────────
    /**
     * Loads one row per visit (medical_record_id).
     * All medicines for a visit are stored in VisitPrescriptionModel.medicines.
     *
     * SQL: we order by medical_record_id then prescription id so we can
     * iterate once and group in Java.
     */
    public void loadData() {
        // remember selected visit so we can restore it after refresh
        VisitPrescriptionModel selected = prescriptionsTable.getSelectionModel().getSelectedItem();
        int selectedMrId = (selected != null) ? selected.medicalRecordId : -1;

        String sql =
            "SELECT pr.id AS pr_id, pr.medical_record_id, " +
            "       pr.medicine_name, pr.dosage, pr.frequency, " +
            "       pr.duration_days, pr.instructions, pr.created_at, " +
            "       p.full_name AS p_name, u.full_name AS d_name " +
            "FROM prescriptions pr " +
            "JOIN medical_records mr ON pr.medical_record_id = mr.id " +
            "JOIN appointments   a  ON mr.appointment_id    = a.id " +
            "JOIN patients       p  ON a.patient_id         = p.id " +
            "JOIN doctors        d  ON mr.doctor_id         = d.id " +
            "JOIN users          u  ON d.user_id            = u.id " +
            "ORDER BY pr.medical_record_id DESC, pr.id ASC";

        List<VisitPrescriptionModel> freshList = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {

            VisitPrescriptionModel current = null;

            while (rs.next()) {
                int mrId = rs.getInt("medical_record_id");

                // New visit → create a new group row
                if (current == null || current.medicalRecordId != mrId) {
                    current = new VisitPrescriptionModel();
                    current.medicalRecordId = mrId;
                    current.patientName     = rs.getString("p_name");
                    current.doctorName      = rs.getString("d_name");
                    current.date            = rs.getString("created_at");
                    freshList.add(current);
                }

                // Add medicine entry to this visit
                MedicineEntry med = new MedicineEntry();
                med.name         = rs.getString("medicine_name");
                med.dosage       = rs.getString("dosage");
                med.frequency    = rs.getString("frequency");
                med.duration     = rs.getInt("duration_days");
                med.instructions = rs.getString("instructions");
                current.medicines.add(med);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Update UI on JavaFX thread
        final List<VisitPrescriptionModel> result = freshList;
        final int restoreMrId = selectedMrId;
        Platform.runLater(() -> {
            dataList.setAll(result);

            // Restore selection if the same visit is still in the list
            if (restoreMrId != -1) {
                for (int i = 0; i < dataList.size(); i++) {
                    if (dataList.get(i).medicalRecordId == restoreMrId) {
                        prescriptionsTable.getSelectionModel().select(i);
                        prescriptionsTable.scrollTo(i);
                        break;
                    }
                }
            }
        });
    }

    // ─── Search ───────────────────────────────────────────────────────────────
    private void setupSearch() {
        FilteredList<VisitPrescriptionModel> filteredData = new FilteredList<>(dataList, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) ->
            filteredData.setPredicate(vm -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                if (vm.patientName.toLowerCase().contains(lower)) return true;
                // also search medicine names within the visit
                for (MedicineEntry m : vm.medicines) {
                    if (m.name.toLowerCase().contains(lower)) return true;
                }
                return false;
            })
        );
        prescriptionsTable.setItems(filteredData);
    }

    // ─── Auto-refresh poller ─────────────────────────────────────────────────
    private void startAutoRefresh() {
        poller = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "prescriptions-poller");
            t.setDaemon(true);
            return t;
        });
        poller.scheduleAtFixedRate(this::loadData, 5, 5, TimeUnit.SECONDS);
    }

    private void stopAutoRefresh() {
        if (poller != null && !poller.isShutdown()) {
            poller.shutdownNow();
        }
    }

    // ─── Preview generation ───────────────────────────────────────────────────
    private void generateDocument(VisitPrescriptionModel vm) {
        StringBuilder sb = new StringBuilder();
        sb.append("====================================\n");
        sb.append("        MEDICORE HOSPITAL          \n");
        sb.append("        OFFICIAL PRESCRIPTION      \n");
        sb.append("====================================\n\n");
        sb.append("Date   : ").append(vm.date).append("\n");
        sb.append("Doctor : Dr. ").append(vm.doctorName).append("\n");
        sb.append("Patient: ").append(vm.patientName).append("\n");
        sb.append("------------------------------------\n\n");

        int idx = 1;
        for (MedicineEntry med : vm.medicines) {
            sb.append("Rx ").append(idx++).append(":\n");
            sb.append("  Medicine   : ").append(med.name).append("\n");
            sb.append("  Dosage     : ").append(med.dosage).append("\n");
            sb.append("  Frequency  : ").append(med.frequency).append("\n");
            sb.append("  Duration   : ").append(med.duration).append(" days\n");
            if (med.instructions != null && !med.instructions.isBlank()) {
                sb.append("  Notes      : ").append(med.instructions).append("\n");
            }
            sb.append("\n");
        }

        sb.append("====================================\n");
        sb.append("        Physician Signature        \n");

        documentArea.setText(sb.toString());
    }

    // ─── Print ────────────────────────────────────────────────────────────────
    public void handlePrint() {
        if (documentArea.getText().isEmpty()) return;

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(rootPane.getScene().getWindow())) {
            Text printableText = new Text(documentArea.getText());
            printableText.setFont(Font.font("Courier New", 12));
            boolean success = job.printPage(printableText);
            if (success) job.endJob();
        }
    }

    // ─── Navigation ───────────────────────────────────────────────────────────
    public void showManagePatients() {
        stopAutoRefresh();
        SceneManager.switchTo((Stage) rootPane.getScene().getWindow(),
                new ReceptionistDashboardController().getView());
    }

    public void showPrescriptions() {
        stopAutoRefresh();
        SceneManager.switchTo((Stage) rootPane.getScene().getWindow(),
                new ReceptionistPrescriptionsController().getView());
    }

    public void handleLogout() {
        stopAutoRefresh();
        Session.getInstance().clear();
        SceneManager.switchTo((Stage) rootPane.getScene().getWindow(),
                new LoginController().getView());
    }
}
