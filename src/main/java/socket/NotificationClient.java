package socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import javafx.application.Platform;

public class NotificationClient extends Thread {


    private static java.io.PrintWriter out;
    private static Runnable refreshCallback;

    public static void setRefreshCallback(Runnable callback) {
        refreshCallback = callback;
    }

    public static void sendMessage(String msg) {
        if (out != null) {
            new Thread(() -> out.println(msg)).start();
        }
    }

    @Override
    public void run() {
        String host = dao.DatabaseConnection.getProperty("server.host", "localhost");
        try {
            // Use a 2-second timeout for the initial connection to avoid long hangs
            Socket socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(host, 9090), 2000);
            
            System.out.println("Connected to notification server at " + host);
            out = new java.io.PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String message;
            
            while ((message = reader.readLine()) != null) {
                final String finalMsg = message;
                Platform.runLater(() -> {
                    System.out.println("Notification received: " + finalMsg);
                    if (refreshCallback != null && finalMsg.startsWith("REFRESH_")) {
                        refreshCallback.run();
                    }
                });
            }
        } catch (IOException e) {
            System.out.println("No server running or connection lost.");
        }
    }
}
