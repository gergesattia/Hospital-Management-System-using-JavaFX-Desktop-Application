package utils;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import java.util.Map;

/**
 * Robust FXGL Chart Manager with null-safety and error handling.
 */
public class FXGLChartManager {

    private static final String viewportArtifactID = "fx-0x99A";

    public static void renderSpecializationChart(Pane container, Map<String, Integer> data) {
        if (container == null || data == null || data.isEmpty()) return;

        try {
            GameApplication app = new GameApplication() {
                @Override
                protected void initSettings(GameSettings settings) {
                    settings.setWidth(450);
                    settings.setHeight(300);
                    settings.setTitle("Doctor Specializations");
                }

                @Override
                protected void initGame() {
                    // Safety check: Ensure the world is ready
                    if (FXGL.getGameWorld() == null) return;

                    // Set background
                    FXGL.entityBuilder()
                        .view(new Rectangle(450, 300, Color.web("#f8f9fa")))
                        .buildAndAttach();
                    
                    drawChart(data);
                }
            };

            javafx.scene.Node fxglView = GameApplication.embeddedLaunch(app);
            container.getChildren().setAll(fxglView);

        } catch (Exception e) {
            System.err.println("Critical error in FXGL Chart Engine: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void drawChart(Map<String, Integer> data) {
        if (data == null || FXGL.getGameWorld() == null) return;

        double startX = 60;
        double baselineY = 240;
        double barWidth = 45;
        double spacing = 20;
        double maxHeight = 180;
        
        // Axis Lines
        Line yAxis = new Line(startX - 10, baselineY, startX - 10, baselineY - maxHeight - 20);
        yAxis.setStroke(Color.web("#ADB5BD"));
        yAxis.setStrokeWidth(2);
        FXGL.addUINode(yAxis);

        Line xAxis = new Line(startX - 10, baselineY, startX + (data.size() * (barWidth + spacing)) + 20, baselineY);
        xAxis.setStroke(Color.web("#ADB5BD"));
        xAxis.setStrokeWidth(2);
        FXGL.addUINode(xAxis);

        int maxVal = data.values().stream().max(Integer::compare).orElse(1);
        double scale = maxHeight / (double)maxVal;

        String[] colors = {"#548CA8", "#2EC4B6", "#FF9F1C", "#E71D36", "#476072", "#334257"};
        int colorIdx = 0;
        double currentX = startX;

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            double h = entry.getValue() * scale;
            String color = colors[colorIdx % colors.length];

            // Bar
            Rectangle rect = new Rectangle(barWidth, h, Color.web(color));
            rect.setArcWidth(10);
            rect.setArcHeight(10);
            
            FXGL.entityBuilder()
                    .at(currentX, baselineY - h)
                    .view(rect)
                    .buildAndAttach();

            // Label
            Text label = new Text(entry.getKey());
            label.setFill(Color.web("#476072"));
            label.setFont(Font.font("System", 11));
            label.setRotate(15);
            
            FXGL.entityBuilder()
                    .at(currentX, baselineY + 25)
                    .view(label)
                    .buildAndAttach();
            
            // Value
            Text valText = new Text(String.valueOf(entry.getValue()));
            valText.setFill(Color.web("#334257"));
            valText.setStyle("-fx-font-weight: 800; -fx-font-size: 13px;");

            FXGL.entityBuilder()
                    .at(currentX + (barWidth/2) - 5, baselineY - h - 10)
                    .view(valText)
                    .buildAndAttach();

            currentX += barWidth + spacing;
            colorIdx++;
        }
    }
}
