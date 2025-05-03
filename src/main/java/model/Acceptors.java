package model;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Handle multiple (concurrent) connection creation requests from client.
 * Include assignment of tasks to Queue.
 */

public class Acceptors {

    private static final Logger logger = Logger.getLogger(Acceptors.class.getName());


    private final ServerSocket serverSocket;
    private final int numberOfAcceptors;
    private final ExecutorService acceptors;


    public Acceptors(final ServerSocket serverSocket, final int numberOfAcceptors) {
        this.serverSocket = serverSocket;
        this.numberOfAcceptors = numberOfAcceptors;
        this.acceptors = Executors.newFixedThreadPool(numberOfAcceptors);
    }

    public void run(ExecutorService workers, Consumer<Socket> requestHandler) {
        for (int i = 0; i < numberOfAcceptors; i++) {
            acceptors.submit(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted() && !serverSocket.isClosed()) {
                        var clientSocket = serverSocket.accept();
                        logger.info("Acceptor-" + Thread.currentThread().getName() + " accept on port " + clientSocket.getPort());
                        workers.submit(new WorkerTask(clientSocket, requestHandler));
                    }
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        System.err.println("연결 수락 중 오류: " + e.getMessage());
                    }
                }
            });
        }
    }

    // todo 제거할지
    public void shutdown() {
        this.acceptors.shutdown();
        System.out.println("Acceptor threads shutdown.");
    }
}
