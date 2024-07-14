import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");

        /* bind(bind to address) -> listen() -> accept() -> send() , recv() */
        try (var serverSocket = new ServerSocket(4221)) {
            serverSocket.setReuseAddress(true);

            Socket clientSocket = connectToServerSocket(serverSocket);

            final String httpMessage = "HTTP/1.1 200 OK\r\n\r\n";

            sendMessage(clientSocket, httpMessage);

            clientSocket.close();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static Socket connectToServerSocket(final ServerSocket serverSocket) throws IOException {
        Socket clientSocket;
        clientSocket = serverSocket.accept(); // Wait for connection from client.
        System.out.println("accepted new connection");
        return clientSocket;
    }

    private static void sendMessage(final Socket clientSocket, final String httpMessage) throws IOException {
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
