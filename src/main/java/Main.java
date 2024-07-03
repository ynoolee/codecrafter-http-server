import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");

        // Uncomment this block to pass the first stage
        //
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        //
        try {
            serverSocket = new ServerSocket(4221);
            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors

            /* bind(bind to address) -> liste() -> accept() -> send() , recv() */
            serverSocket.setReuseAddress(true);

            clientSocket = connect(serverSocket);

            final String httpMessage = "HTTP/1.1 200 OK\\r\\n\\r\\n\n";
            final PrintStream output = new PrintStream(clientSocket.getOutputStream());
            output.println(httpMessage);

            clientSocket.close();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static Socket connect(final ServerSocket serverSocket) throws IOException {
        Socket clientSocket;
        clientSocket = serverSocket.accept(); // Wait for connection from client.
        System.out.println("accepted new connection");
        return clientSocket;
    }
}
