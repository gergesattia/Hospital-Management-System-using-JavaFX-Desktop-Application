package utils;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.JavaFXFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import javafx.scene.image.Image;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CameraManager {
    private OpenCVFrameGrabber grabber;
    private ScheduledExecutorService timer;
    private JavaFXFrameConverter converter;
    private boolean active = false;

    public CameraManager() {
        this.grabber = new OpenCVFrameGrabber(0);
        this.converter = new JavaFXFrameConverter();
    }

    public void startCamera(ImageView view) {
        if (active) return;
        try {
            grabber.start();
            active = true;
            timer = Executors.newSingleThreadScheduledExecutor();
            timer.scheduleAtFixedRate(() -> {
                try {
                    Frame frame = grabber.grab();
                    if (frame != null) {
                        Image image = converter.convert(frame);
                        Platform.runLater(() -> view.setImage(image));
                    }
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }, 0, 33, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            System.err.println("Could not start camera: " + e.getMessage());
        }
    }

    public void stopCamera() {
        if (!active) return;
        try {
            active = false;
            if (timer != null) timer.shutdown();
            if (grabber != null) {
                grabber.stop();
                grabber.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Image grabCurrentFrame() {
        try {
            Frame frame = grabber.grab();
            if (frame != null) return converter.convert(frame);
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public boolean isActive() {
        return active;
    }
}
