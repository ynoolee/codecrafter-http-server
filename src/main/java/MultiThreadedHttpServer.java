import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class MultiThreadedHttpServer extends HttpServer {

    private static final Logger logger = Logger.getLogger(MultiThreadedHttpServer.class.getName());

    public MultiThreadedHttpServer(final int port, final String parentAbsolutePath) {
        super(port, parentAbsolutePath);
    }

    @Override
    public void run() {
        var taskCount = 5000;
        var acceptorCount = 2;
        var acceptors = Executors.newFixedThreadPool(acceptorCount);
        var workers = Executors.newVirtualThreadPerTaskExecutor();

        try (var serverSocket = new ServerSocket(this.port);
        ) {
            serverSocket.setReuseAddress(true);
            logger.info("HTTP server started on port " + port);

            for (int i = 0; i < acceptorCount; i++) {
                acceptors.submit(() -> {
                    try {
                        while (!Thread.currentThread().isInterrupted() && !serverSocket.isClosed()) {
                            Socket clientSocket = null;
                            try {
                                // 최대 연결 수 제한
                                // 소켓 수락 (try-with-resources로 감싸지 않음)
                                clientSocket = serverSocket.accept();

                                // 이 소켓 객체를 final로 캡처하여 작업자 스레드로 전달
                                final Socket socketToProcess = clientSocket;

                                // 작업자 스레드에 소켓 처리 위임 (소켓 닫기까지 담당)
                                workers.submit(() -> receiveAndRespond(socketToProcess));

                                // 책임이 작업자 스레드로 이전되었으므로 acceptor에서는 null로 설정
                                clientSocket = null;
                            } catch (IOException e) {
                                if (!serverSocket.isClosed()) {
                                    System.err.println("연결 수락 중 오류: " + e.getMessage());
                                }
                            } finally {
                                // 예외 발생 시 소켓이 여전히 열려 있다면 닫기
                                if (clientSocket != null) {
                                    try {
                                        clientSocket.close();
                                    } catch (IOException e) {
                                        System.err.println("소켓 닫기 실패: " + e.getMessage());
                                    }
                                }
                            }
                        }
                    } finally {
                        System.out.println("Acceptor 스레드 종료");
                    }
                });
            }
            Thread.currentThread().join(); // 현재 스레드가 종료될 때까지 대기
        } catch (IOException ex) {

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void receiveAndRespond(final Socket clientSocket) {
        try (
            final InputStream inputStream = clientSocket.getInputStream();
            final OutputStream outputStream = clientSocket.getOutputStream();
        ) {
            logger.info("receiveAndRespond - port : " + clientSocket.getLocalPort());
            receiveAndRespondData(inputStream, outputStream);
        } catch (IOException exception) {
            logger.info("Error during request processing");
        }
        logger.info("Close HTTP connection");
    }

    private void receiveAndRespondData(final InputStream inputStream, final OutputStream outputStream) throws IOException {
        final HttpRequest message = HttpMessageParseUtil.readHttpRequestMessage(inputStream);

        sendResponse(message, new BufferedOutputStream(outputStream));
    }

    private void sendResponse(
        final HttpRequest request
        , final OutputStream output
    ) throws IOException {
        final StartLine startLine = request.getStartLine();
        final String path = startLine.extractPath();
        final HttpMethod method = startLine.method();

        if (HttpMethod.POST.equals(method)) {
            hanldePostMethod(request, output, path);
        } else if (HttpMethod.GET.equals(method)) {
            handleGetMethod(output, path, request);
        }

        output.flush();
    }

    private void handleGetMethod(final OutputStream output, final String path, HttpRequest request) throws IOException {
        final StartLine startLine = request.getStartLine();
        if ("/".equals(path)) {
            output.write(CommonHttpResponse.OK_RESPONSE.getBytes());
        } else if (path.contains("/echo/")) {
            final HttpResponse response = HttpResponse.of(startLine.extractResourceId(), HttpHeader.ContentType.TEXT_PLAIN);
            output.write(response.toString().getBytes());
        } else if (path.contains("/user-agent")) {
            final HttpResponse response =
                HttpResponse.of(
                    request.valueOfKey(HttpHeader.USER_AGENT).orElseThrow(() -> new RuntimeException("user-agent 에 값이 없습니다"))
                    , HttpHeader.ContentType.BINARY_DATE
                );
            output.write(response.toString().getBytes());
        } else if (path.contains("/files/")) {
            writeFileToResponse(output, this.parentAbsolutePath + startLine.extractResourceId());
        } else {
            output.write(CommonHttpResponse.NOT_FOUND_RESOURCE_RESPONSE.getBytes());
        }
    }

    private void writeFileToResponse(final OutputStream output, final String absoluteFilePath) throws IOException {
        try {
            final String fileContent = readFromFile(absoluteFilePath);
            output.write(HttpResponse.of(fileContent, HttpHeader.ContentType.BINARY_DATE).toString().getBytes());
        } catch (Exception ex) {
            output.write(CommonHttpResponse.NOT_FOUND_RESOURCE_RESPONSE.getBytes());
        }
    }

    private void hanldePostMethod(final HttpRequest request, final OutputStream output, final String path) throws IOException {
        final StartLine startLine = request.getStartLine();
        if (path.contains("/files/")) {
            final String requestBody = request.getBody();
            logger.info("Read RequestBody : " + requestBody);
            final String resourceId = startLine.extractResourceId();
            final String absolutePath = this.parentAbsolutePath + resourceId;
            try (final FileWriter writer = new FileWriter(absolutePath)) {
                logger.info("File path :" + absolutePath);
                writer.write(requestBody);
                writer.flush();
            }
            logger.info("Complete Writing file");
            output.write(CommonHttpResponse.CREATED.getBytes());
        } else {
            output.write(CommonHttpResponse.NOT_FOUND_RESOURCE_RESPONSE.getBytes());
        }
    }

    private static String readFromFile(String path) {
        final var filePath = Paths.get(path);
        try {
            final var contents = Files.readAllBytes(filePath);
            return new String(contents);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
