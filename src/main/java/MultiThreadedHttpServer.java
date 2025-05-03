import model.Acceptors;

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
        var workers = Executors.newFixedThreadPool(10); // var workers = Executors.newVirtualThreadPerTaskExecutor()

        try (var serverSocket = new ServerSocket(this.port)) {
            serverSocket.setReuseAddress(true);
            logger.info(" - HTTP server started on port " + port);

            var acceptors = new Acceptors(serverSocket, 1);
            acceptors.run(workers, this::receiveAndRespond);

            Thread.currentThread().join(); // 현재 스레드가 종료될 때까지 대기
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void receiveAndRespond(final Socket clientSocket) {
        try (
            final InputStream inputStream = clientSocket.getInputStream();
            final OutputStream outputStream = clientSocket.getOutputStream();
        ) {
            receiveAndRespondData(inputStream, outputStream);
        } catch (IOException exception) {
            logger.info(" - Error during request processing");
        }
        logger.info(" - Close HTTP connection");
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
            logger.info(" - Read RequestBody : " + requestBody);
            final String resourceId = startLine.extractResourceId();
            final String absolutePath = this.parentAbsolutePath + resourceId;
            try (final FileWriter writer = new FileWriter(absolutePath)) {
                logger.info(" - File path :" + absolutePath);
                writer.write(requestBody);
                writer.flush();
            }
            logger.info(" - Complete Writing file");
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
