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
                final char[] tempMessageContainer = new char[4096];
                final char[] finalMessage;
                int curIdx = 0;

                while (true) {
                    char readChar = (char) reader.read();

                    tempMessageContainer[curIdx++] = readChar;

                    if (readChar == '\n') {
                        if (curIdx >= 4
                                && tempMessageContainer[curIdx - 2] == '\r'
                                && tempMessageContainer[curIdx - 3] == '\n'
                                && tempMessageContainer[curIdx - 4] == '\r') {
                            break;
                        }
                    }
                }
                finalMessage = new char[curIdx - 1];

                System.arraycopy(tempMessageContainer, 0, finalMessage, 0, finalMessage.length);
                System.out.println(finalMessage);
            }
            System.out.println("Turn off HTTP Server");
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
