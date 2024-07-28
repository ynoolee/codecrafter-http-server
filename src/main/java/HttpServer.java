import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {

    public static void main(String[] args) {
        int port = 8080;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("HTTP server started on port " + port);

            try (final Socket clientSocket = serverSocket.accept();
                 final InputStream inputStream = clientSocket.getInputStream()
            ) {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                while (true) {
                    String readLine = reader.readLine();

                    if (readLine == null) {
                        break;
                    }
                    System.out.println(readLine);
                }
            }
            System.out.println("Turn off HTTP Server");
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
