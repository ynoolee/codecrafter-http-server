package model;

import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class WorkerTask implements Runnable {
    private static final Logger logger = Logger.getLogger(WorkerTask.class.getName());

    private final Socket clientSocket;
    private final Consumer<Socket> httpRequestProcessAndResponder;

    public WorkerTask(final Socket clientSocket, final Consumer<Socket> httpRequestProcessAndResponder) {
        this.clientSocket = clientSocket;
        this.httpRequestProcessAndResponder = httpRequestProcessAndResponder;
    }

    @Override
    public void run() {
        try {
            logger.info(Thread.currentThread().getName() + " start to handle");
            httpRequestProcessAndResponder.accept(clientSocket);
        } catch (Exception ex) {
            logger.info(Thread.currentThread().getName() + "- Error during request processing: " + ex.getMessage());
        } finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                logger.warning("소켓 닫기 실패: " + e.getMessage());
            }
        }
    }
}
