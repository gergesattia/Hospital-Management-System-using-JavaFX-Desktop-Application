package utils.ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.ArrayList;

public class FaceEngine {
    private static final String BRIDGE_URL = "http://127.0.0.1:5005";
    private final HttpClient client;
    private final Gson gson;

    public FaceEngine() {
        this.client = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    // Constructor with path (for compatibility with old code, though not used now)
    public FaceEngine(String modelPath) {
        this();
    }

    public VerifyResult verify(String username, Image frame) {
        try {
            String b64 = imageToBase64(frame);
            JsonObject json = new JsonObject();
            json.addProperty("username", username);
            json.addProperty("image", b64);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BRIDGE_URL + "/verify"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(json)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject res = gson.fromJson(response.body(), JsonObject.class);
                boolean match = res.get("match").getAsBoolean();
                double distance = res.get("distance").getAsDouble();
                String status = res.get("status").getAsString();
                return new VerifyResult(match, distance, status);
            }
        } catch (Exception e) {
            System.err.println("AI Bridge Error: " + e.getMessage());
        }
        return new VerifyResult(false, 1.0, "error");
    }

    public boolean enroll(String username, List<Image> frames) {
        try {
            List<String> b64List = new ArrayList<>();
            for (Image img : frames) {
                b64List.add(imageToBase64(img));
            }

            JsonObject json = new JsonObject();
            json.addProperty("username", username);
            json.add("images", gson.toJsonTree(b64List));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BRIDGE_URL + "/enroll"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(json)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject res = gson.fromJson(response.body(), JsonObject.class);
                return res.get("success").getAsBoolean();
            }
        } catch (Exception e) {
            System.err.println("AI Bridge Error: " + e.getMessage());
        }
        return false;
    }

    private String imageToBase64(Image img) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", out);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }

    // Compatibility methods for existing code
    public float[] generateEmbedding(Image img) { return new float[512]; }
    public float calculateSimilarity(float[] v1, float[] v2) { return 0; }
    public boolean isMatch(float[] live, List<float[]> database, float threshold) { return false; }
    public void close() {}
}
