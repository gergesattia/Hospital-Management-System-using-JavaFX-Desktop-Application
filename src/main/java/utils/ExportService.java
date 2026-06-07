package utils;

import dao.DatabaseConnection;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ExportService {

    public static String exportDatabaseToExcel() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        String[] tables = {"users", "patients", "medicines", "appointments", "sales"};

        Connection conn = DatabaseConnection.getConnection();
        for (String tableName : tables) {
            Sheet sheet = workbook.createSheet(tableName);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName);
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Create Header Row
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 1; i <= columnCount; i++) {
                Cell cell = headerRow.createCell(i - 1);
                cell.setCellValue(metaData.getColumnName(i));
                cell.setCellStyle(headerStyle);
            }

            // Create Data Rows
            int rowNum = 1;
            while (rs.next()) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    if (value != null) {
                        row.createCell(i - 1).setCellValue(value.toString());
                    }
                }
            }

            // Auto-size columns
            for (int i = 0; i < columnCount; i++) {
                sheet.autoSizeColumn(i);
            }
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "MediCore_Backup_" + timestamp + ".xlsx";
        
        java.io.File backupDir = null;
        
        // 1. Try Google Drive for Desktop mounted virtual drive
        java.io.File googleDriveBackup = new java.io.File("G:\\My Drive\\backup");
        if (googleDriveBackup.exists() && googleDriveBackup.isDirectory()) {
            backupDir = googleDriveBackup;
        } else {
            java.io.File googleDriveMyDrive = new java.io.File("G:\\My Drive");
            if (googleDriveMyDrive.exists() && googleDriveMyDrive.isDirectory()) {
                java.io.File newBackupDir = new java.io.File(googleDriveMyDrive, "backup");
                if (newBackupDir.exists() || newBackupDir.mkdirs()) {
                    backupDir = newBackupDir;
                }
            }
        }
        
        // 2. Try the Desktop "New folder" (or "ملفجديد") synced folder
        if (backupDir == null) {
            String[] possibleSyncFolders = {
                "C:\\Users\\gerge\\Desktop\\New folder",
                "C:\\Users\\gerge\\Desktop\\ملفجديد",
                "C:\\Users\\gerge\\Desktop\\ملف جديد",
                "C:\\Users\\gerge\\ملفجديد",
                "C:\\Users\\gerge\\ملف جديد"
            };
            for (String folderPath : possibleSyncFolders) {
                java.io.File syncFolder = new java.io.File(folderPath);
                if (syncFolder.exists() && syncFolder.isDirectory()) {
                    java.io.File newBackupDir = new java.io.File(syncFolder, "backup");
                    if (newBackupDir.exists() || newBackupDir.mkdirs()) {
                        backupDir = newBackupDir;
                        break;
                    }
                }
            }
        }
        
        // 3. Fallback to local project directory
        java.io.File finalFile;
        if (backupDir != null) {
            finalFile = new java.io.File(backupDir, fileName);
        } else {
            finalFile = new java.io.File(fileName);
        }
        
        System.out.println("Saving Excel backup to: " + finalFile.getAbsolutePath());
        
        try (FileOutputStream fileOut = new FileOutputStream(finalFile)) {
            workbook.write(fileOut);
        }
        workbook.close();

        return finalFile.getAbsolutePath();
    }
}
