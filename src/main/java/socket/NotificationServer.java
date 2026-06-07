package socket;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class NotificationServer extends Thread {
    private static final int PORT = 9090;
    private List<PrintWriter> clientWriters = new ArrayList<>();

    public NotificationServer() {
        // Just for starting the thread
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Notification Server listening on port " + PORT);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler extends Thread {
        private Socket socket;
        private java.io.BufferedReader reader;
        private PrintWriter writer;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                reader = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);
                synchronized (clientWriters) {
                    clientWriters.add(writer);
                }

                String msg;
                while ((msg = reader.readLine()) != null) {
                    broadcastMessage(msg);
                }
            } catch (IOException e) {
                // Connection lost
            } finally {
                synchronized (clientWriters) {
                    clientWriters.remove(writer);
                }
            }
        }
    }

    public void broadcastMessage(String message) {
        synchronized (clientWriters) {
            for (PrintWriter writer : clientWriters) {
                writer.println(message);
            }
        }
    }
}
