import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class HttpServer {

    private static final Logger logger = Logger.getLogger(HttpServer.class.getName());
    private final int port;
    private final String parentAbsolutePath;


    public HttpServer(final int port, final String parentAbsolutePath) {
        this.port = port;
        this.parentAbsolutePath = parentAbsolutePath;
    }

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(this.port)) {
            serverSocket.setReuseAddress(true);
            System.out.println("HTTP server started on port " + port);

            final int threadCount = 10;

            List<CompletableFuture<Void>> results = new ArrayList<>(threadCount);
            for (int i = 0; i < threadCount; i++) {
                results.add(CompletableFuture.runAsync(() -> {
                    try {
                        acceptAndRespond(serverSocket);
                    } catch (IOException e) {
                        System.out.println("Error");
                    }
                }));
            }

            for (CompletableFuture<Void> result : results) {
                result.get();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void acceptAndRespond(final ServerSocket serverSocket) throws IOException {
        try (final Socket clientSocket = serverSocket.accept();
             final InputStream inputStream = clientSocket.getInputStream();
             final OutputStream outputStream = clientSocket.getOutputStream();
        ) {
            final BufferedOutputStream output = new BufferedOutputStream(outputStream);

            final HttpRequest message = HttpMessageParseUtil.readHttpRequestMessage(inputStream);

            sendResponse(message, output);

            output.flush();
            output.close();
        }
        logger.info("Close HTTP connection");
    }

    private void sendResponse(
            final HttpRequest request
            , final BufferedOutputStream output
    ) throws IOException {
        final StartLine startLine = request.getStartLine();
        final String path = startLine.extractPath();
        final HttpMethod method = startLine.method();

        if (HttpMethod.POST.equals(method)) {
            hanldePostMethod(request, output, path);
        } else if (HttpMethod.GET.equals(method)) {
            handleGetMethod(output, path, request);
        }
    }

    private void handleGetMethod(final BufferedOutputStream output, final String path, HttpRequest request) throws IOException {
        final StartLine startLine = request.getStartLine();
        if ("/".equals(path)) {
            output.write(CommonHttpResponse.OK_RESPONSE.getBytes());
        } else if (path.contains("/echo/")) {
            final HttpResponse response = HttpResponse.ofPlainText(startLine.extractResourceId());
            output.write(response.toString().getBytes());
        } else if (path.contains("/user-agent")) {
            final HttpResponse response = HttpResponse.ofPlainText(request.valueOfKey(HttpHeader.USER_AGENT).orElseThrow(() -> new RuntimeException("user-agent 에 값이 없습니다")));
            output.write(response.toString().getBytes());
        } else if (path.contains("/files/")) {
            writeFileToResponse(output, this.parentAbsolutePath + startLine.extractResourceId());
        } else {
            output.write(CommonHttpResponse.NOT_FOUND_RESOURCE_RESPONSE.getBytes());
        }
    }

    private void writeFileToResponse(final BufferedOutputStream output, final String absoluteFilePath) throws IOException {
        try {
            final String fileContent = readFromFile(absoluteFilePath);
            output.write(HttpResponse.ofFile(fileContent).toString().getBytes());
        } catch (Exception ex) {
            output.write(CommonHttpResponse.NOT_FOUND_RESOURCE_RESPONSE.getBytes());
        }
    }

    private void hanldePostMethod(final HttpRequest request, final BufferedOutputStream output, final String path) throws IOException {
        final StartLine startLine = request.getStartLine();
        if (path.contains("/files/")) {
            final String requestBody = request.getBody();
            logger.info("Read RequestBody : " + requestBody);
            final String resourceId = startLine.extractResourceId();
            final String absolutePath = this.parentAbsolutePath + resourceId;
            try (final FileWriter writer = new FileWriter(absolutePath)) {
                logger.info("File path :"+absolutePath);
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
            final byte[] contents = Files.readAllBytes(filePath);
            return new String(contents);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
