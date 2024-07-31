import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class HttpServer {

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
            System.out.printf("Thread %s Is ServerSocket closed just after creating clientSocket? %s\n",
                    Thread.currentThread().getName(), serverSocket.isClosed()
            );
            final BufferedOutputStream bufferedStream = new BufferedOutputStream(outputStream);

            final HttpRequest message = readHttpRequestMessage(inputStream);

            sendResponse(message, bufferedStream);

            bufferedStream.flush();
        }
        System.out.println("Close HTTP connection");
    }

    private static Headers createHeaders(final List<String> headers) {
        Map<String, String> map = new HashMap<>();

        for (String header : headers) {
            String[] part = Arrays.stream(header.split(":")).map(String::trim).toArray(String[]::new);
            map.put(part[0], part[1]);
        }
        return new Headers(map);
    }

    private void sendResponse(
            final HttpRequest request
            , final BufferedOutputStream bufferedStream
    ) throws IOException {
        final StartLine startLine = request.startLine();
        final Headers headers = request.headers();
        final String path = startLine.extractPath();
        final HttpMethod method = startLine.method();

        if (HttpMethod.POST.equals(method)) {
            hanldePostMethod(request, bufferedStream, path, startLine);
        } else if (HttpMethod.GET.equals(method)) {
            handleGetMethod(bufferedStream, path, startLine, headers);
        } else {
            bufferedStream.write(CommonHttpResponse.NOT_FOUND_RESOURCE_RESPONSE.getBytes());
        }
    }

    private void handleGetMethod(final BufferedOutputStream bufferedStream, final String path, final StartLine startLine, final Headers headers) throws IOException {
        if ("/".equals(path)) {
            bufferedStream.write(CommonHttpResponse.OK_RESPONSE.getBytes());
        } else if (path.contains("/echo/")) {
            final HttpResponse response = HttpResponse.ofPlainText(startLine.extractResourceId());
            bufferedStream.write(response.toString().getBytes());
        } else if (path.contains("/user-agent")) {
            final HttpResponse response = HttpResponse.ofPlainText(headers.headerValue("User-Agent").orElseThrow(() -> new RuntimeException("user-agent 에 값이 없습니다")));
            bufferedStream.write(response.toString().getBytes());
        } else if (path.contains("/files/")) {
            writeFileToResponse(bufferedStream, this.parentAbsolutePath + startLine.extractResourceId());
        }
    }

    private void writeFileToResponse(final BufferedOutputStream bufferedStream, final String absoluteFilePath) throws IOException {
        try {
            final String fileContent = readFromFile(absoluteFilePath);
            bufferedStream.write(HttpResponse.ofFile(fileContent).toString().getBytes());
        } catch (Exception ex) {
            bufferedStream.write(CommonHttpResponse.NOT_FOUND_RESOURCE_RESPONSE.getBytes());
        }
    }

    private void hanldePostMethod(final HttpRequest request, final BufferedOutputStream bufferedStream, final String path, final StartLine startLine) throws IOException {
        if (path.contains("/files/")) {
            final String requestBody = request.requestBody();
            final String resourceId = startLine.extractResourceId();
            final String absolutePath = this.parentAbsolutePath + resourceId;
            try (final FileWriter writer = new FileWriter(absolutePath);) {
                writer.write(requestBody);
                writer.flush();
            }

            bufferedStream.write(CommonHttpResponse.CREATED.getBytes());
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

    /**
     * In this case where Http Message does not contain any message body,
     * Just detecting Header terminator ( CRLF Followed by CRLF )
     * is only thing reading Http message.
     */
    private static HttpRequest readHttpRequestMessage(InputStream inputStream) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        final char[] tempMessageContainer = new char[4096];
        int curIdx = 0;

        while (true) {
            char readChar = (char) reader.read();

            tempMessageContainer[curIdx++] = readChar;

            // detect Header terminator
            if (readChar == '\n') {
                if (curIdx >= 4
                        && tempMessageContainer[curIdx - 2] == '\r'
                        && tempMessageContainer[curIdx - 3] == '\n'
                        && tempMessageContainer[curIdx - 4] == '\r'
                ) {
                    break;
                }
            }
        }

        HttpRequest request = createHttpRequest(tempMessageContainer, curIdx);

        if (request.headers().headerValue(Headers.Header.CONTENT_LENGTH_HEADER.getValue()).isEmpty()) {
            return request;
        }
        final Integer contentLength = request.headers().headerValue(Headers.Header.CONTENT_LENGTH_HEADER.getValue()).map(value -> Integer.valueOf(value)).orElse(0);
        char[] contents = new char[contentLength];
        for (int i = 0; i < contentLength; i++) {
            contents[i] = (char) reader.read();
        }

        return HttpRequest.HttpRequestBuilder.builder().startLine(request.startLine()).headers(request.headers().deepCopy()).responseBody(new String(contents)).build();
    }

    private static HttpRequest createHttpRequest(final char[] startLineAndHeaders, final int curIdx) {
        char[] trucatedStartLineAndHeaders = truncateAndCreateArray(startLineAndHeaders, curIdx - 4);

        List<String> parts = parse(new String(trucatedStartLineAndHeaders), "\r\n");

        final StartLine startLine = createStartLine(parts);
        final Headers headers = createHeaders(parts.subList(1, parts.size()));


        return HttpRequest.HttpRequestBuilder.builder()
                .startLine(startLine)
                .headers(headers.deepCopy())
                .build();
    }

    private static char[] truncateAndCreateArray(char[] array, int length) {
        final char[] result = new char[length];

        System.arraycopy(array, 0, result, 0, result.length);

        return result;
    }

    private static List<String> parse(String request, String delimiter) {
        return Arrays.stream(request.split(delimiter)).toList();
    }

    private static StartLine createStartLine(List<String> messageParts) {
        if (messageParts == null || messageParts.isEmpty()) {
            throw new RuntimeException("message 가 비어있습니다");
        }

        return StartLine.of(messageParts.get(0));
    }
}
