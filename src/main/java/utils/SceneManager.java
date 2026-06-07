package utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneManager {
    public static void switchTo(Stage stage, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(SceneManager.class.getResource(fxmlPath));
            switchTo(stage, root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void switchTo(Stage stage, Parent root) {
        boolean isMaximized = stage.isMaximized();
        
        if (stage.getScene() == null) {
            Scene scene = new Scene(root, 1280, 800);
            stage.setScene(scene);
            stage.setMinWidth(1100);
            stage.setMinHeight(750);
        } else {
            
            stage.getScene().setRoot(root);
        }
        
        if (!stage.isShowing()) {
            stage.show();
            stage.setMaximized(true);
        } else {
            stage.setMaximized(isMaximized);
        }
    }
}
