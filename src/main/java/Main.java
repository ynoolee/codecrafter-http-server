import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");

        /* bind(bind to address) -> listen() -> accept() -> send() , recv() */
        try (var serverSocket = new ServerSocket(4221)) {
            serverSocket.setReuseAddress(true);

            Socket clientSocket = connectToServerSocket(serverSocket);

            // receive message
            final LineNumberReader reader = new LineNumberReader(new InputStreamReader(clientSocket.getInputStream()));
            final String requestLine = reader.readLine();

            // parse and branch
            final String path = extractPath(requestLine);

            final String okResponse = "HTTP/1.1 200 OK\r\n\r\n";
            final String notFoundResourceResponse = "HTTP/1.1 404 Not Found\r\n\r\n";


            if("/".equals(path)) {
                sendResponse(clientSocket, okResponse);
            } else {
                sendResponse(clientSocket, notFoundResourceResponse);
            }

            clientSocket.close();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
    private static String extractPath(String requestLine) {
        final List<String> parts = parse(requestLine, " ");

        return parts.get(1);
    }

    private static List<String> parse(String request, String delimiter) {
        return Arrays.stream(request.split(delimiter)).toList();
    }


    private static Socket connectToServerSocket(final ServerSocket serverSocket) throws IOException {
        Socket clientSocket;
        clientSocket = serverSocket.accept(); // Wait for connection from client.
        System.out.println("accepted new connection");
        return clientSocket;
    }

    private static void sendResponse(final Socket clientSocket, final String httpMessage) throws IOException {
        // my version 1 : using PrintStream
//            final PrintStream output = new PrintStream(clientSocket.getOutputStream());
//            output.println(httpMessage);
//            output.flush();
        // my version 2 : using BufferedOutputStream
        final BufferedOutputStream bufferedOut = new BufferedOutputStream(clientSocket.getOutputStream());
        bufferedOut.write(httpMessage.getBytes());
        bufferedOut.flush();
    }
}
