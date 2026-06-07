package utils;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

public class GoogleDriveService {
    private static final String APPLICATION_NAME = "MediCore ERP";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);
    private static final String CREDENTIALS_FILE_PATH = "tokens/client_secret_new.json"; // سيتم تعديل هذا لاحقاً
    private static final String BACKUP_FOLDER_ID = "1mapJrANT4qMpfoI5Bh--1ap3ylG9ZhUR";

    private static Credential getCredentials(final HttpTransport HTTP_TRANSPORT) throws Exception {
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, 
                new InputStreamReader(new FileInputStream(CREDENTIALS_FILE_PATH)));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(9999).build();
        try {
            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
            System.out.println("[GoogleDrive] Successfully authenticated with Google Drive");
            return credential;
        } catch (Exception e) {
            System.err.println("[GoogleDrive] Authentication failed: " + e.getMessage());
            // Delete invalid credentials to force re-auth
            java.io.File tokensDir = new java.io.File(TOKENS_DIRECTORY_PATH);
            if (tokensDir.exists()) {
                java.io.File[] files = tokensDir.listFiles();
                if (files != null) {
                    for (java.io.File f : files) {
                        if (f.getName().contains("StoredCredential")) {
                            f.delete();
                            System.out.println("[GoogleDrive] Deleted expired credentials");
                        }
                    }
                }
            }
            throw new Exception("Please open http://localhost:9999 to authorize Google Drive access", e);
        }
    }

    public static String uploadFile(String filePath, String fileName) throws Exception {
        try {
            System.out.println("[GoogleDrive] Starting upload for: " + fileName);
            
            final HttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            // Verify folder exists, if not create it or use root
            String targetFolderId = BACKUP_FOLDER_ID;
            try {
                FileList result = service.files().list()
                        .setQ("'" + BACKUP_FOLDER_ID + "' in parents")
                        .setPageSize(1)
                        .setFields("files(id)")
                        .execute();
                System.out.println("[GoogleDrive] Folder verified: " + BACKUP_FOLDER_ID);
            } catch (Exception e) {
                System.out.println("[GoogleDrive] Backup folder not accessible, uploading to root");
                targetFolderId = "root";
            }

            File fileMetadata = new File();
            fileMetadata.setName(fileName);
            fileMetadata.setParents(Collections.singletonList(targetFolderId));
            java.io.File filePathSource = new java.io.File(filePath);
            FileContent mediaContent = new FileContent("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", filePathSource);
            
            System.out.println("[GoogleDrive] Uploading file size: " + filePathSource.length() + " bytes");
            File file = service.files().create(fileMetadata, mediaContent)
                    .setFields("id, webViewLink")
                    .execute();
            
            System.out.println("[GoogleDrive] File uploaded successfully: " + file.getId());
            return file.getId();
        } catch (Exception e) {
            System.err.println("[GoogleDrive] Upload error: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("Google Drive upload failed: " + e.getMessage(), e);
        }
    }
}
