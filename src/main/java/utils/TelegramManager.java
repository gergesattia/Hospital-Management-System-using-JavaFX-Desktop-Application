package utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.UUID;

public class TelegramManager {
    private static final String TOKEN = "8620468124:AAFHBHMsHM4kpVABev0QWk5-eWXGl-e04jc";
    private static final String CHAT_ID = "-1003960914479";
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void sendMessage(String text) {
        try {
            String url = "https://api.telegram.org/bot" + TOKEN + "/sendMessage?chat_id=" + CHAT_ID + "&text=" +
                    java.net.URLEncoder.encode(text, "UTF-8") + "&parse_mode=HTML";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Telegram Error: " + e.getMessage());
        }
    }

    public static void sendPhoto(String caption, File photoFile) {
        if (!photoFile.exists())
            return;

        try {
            String boundary = "Boundary-" + UUID.randomUUID().toString();
            String url = "https://api.telegram.org/bot" + TOKEN + "/sendPhoto";

            byte[] body = createMultipartBody(boundary, caption, photoFile);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() != 200) {
                            System.err.println("Telegram Photo Failed: " + response.body());
                        }
                    });
        } catch (Exception e) {
            System.err.println("Telegram Photo Error: " + e.getMessage());
        }
    }

    private static byte[] createMultipartBody(String boundary, String caption, File file) throws IOException {
        String CRLF = "\r\n";
        StringBuilder sb = new StringBuilder();

        // Chat ID
        sb.append("--").append(boundary).append(CRLF);
        sb.append("Content-Disposition: form-data; name=\"chat_id\"").append(CRLF).append(CRLF);
        sb.append(CHAT_ID).append(CRLF);

        // Caption
        sb.append("--").append(boundary).append(CRLF);
        sb.append("Content-Disposition: form-data; name=\"caption\"").append(CRLF).append(CRLF);
        sb.append(caption).append(CRLF);

        // Parse Mode
        sb.append("--").append(boundary).append(CRLF);
        sb.append("Content-Disposition: form-data; name=\"parse_mode\"").append(CRLF).append(CRLF);
        sb.append("HTML").append(CRLF);

        // Photo
        sb.append("--").append(boundary).append(CRLF);
        sb.append("Content-Disposition: form-data; name=\"photo\"; filename=\"").append(file.getName()).append("\"")
                .append(CRLF);
        sb.append("Content-Type: image/png").append(CRLF).append(CRLF);

        byte[] header = sb.toString().getBytes("UTF-8");
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        byte[] footer = (CRLF + "--" + boundary + "--" + CRLF).getBytes("UTF-8");

        byte[] total = new byte[header.length + fileBytes.length + footer.length];
        System.arraycopy(header, 0, total, 0, header.length);
        System.arraycopy(fileBytes, 0, total, header.length, fileBytes.length);
        System.arraycopy(footer, 0, total, header.length + fileBytes.length, footer.length);

        return total;
    }
}
