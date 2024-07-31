import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class HttpServer {

    private final int port;

    private final String parentAbsolutePath;

    private static final String OK_RESPONSE = "HTTP/1.1 200 OK\r\n\r\n";
    private static final String NOT_FOUND_RESOURCE_RESPONSE = "HTTP/1.1 404 Not Found\r\n\r\n";

    public HttpServer(final int port, final String parentAbsolutePath) {
        this.port = port;
        this.parentAbsolutePath = parentAbsolutePath;
    }

    public void run() {

        try (ServerSocket serverSocket = new ServerSocket(this.port)) {
            System.out.println("HTTP server started on port " + port);

            final int threadCount = 10;

            runConcurrentlyBy(threadCount, serverSocket);

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private void runConcurrentlyBy(int threadCount, ServerSocket serverSocket) {
        final ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {

            CompletableFuture.runAsync(() -> {
                try {
                    acceptAndRespond(serverSocket);
                } catch (IOException e) {
                    System.out.println("Error");
                }
            }, threadPool).whenComplete((a, b) -> {
                System.out.printf("%s thread response complete%n", Thread.currentThread().getName());
            }).exceptionally((ex) -> {
                System.out.println(Arrays.toString(ex.getStackTrace()));
                System.out.printf("%s thread exception :%s - %s%n"
                        , Thread.currentThread().getName(), ex.getClass().getName(), ex.getMessage());
                return null;
            });
        }
    }

    private void acceptAndRespond(final ServerSocket serverSocket) throws IOException {
        try (final Socket clientSocket = serverSocket.accept();
             final InputStream inputStream = clientSocket.getInputStream();
             final OutputStream outputStream = clientSocket.getOutputStream();
        ) {
            final BufferedOutputStream bufferedStream = new BufferedOutputStream(outputStream);

            final String message = readHttpMessageNotContainingBody(inputStream);

            List<String> parts = parse(message, "\r\n");

            final StartLine startLine = createStartLine(parts);
            final Headers headers = createHeaders(parts.subList(1, parts.size()));

            sendResponse(startLine, headers, bufferedStream);

            bufferedStream.flush();
        }
        System.out.println("Close HTTP connection");
    }

    private Headers createHeaders(final List<String> headers) {
        Map<String, String> map = new HashMap<>();

        for (String header : headers) {
            String[] part = Arrays.stream(header.split(":")).map(String::trim).toArray(String[]::new);
            map.put(part[0], part[1]);
        }
        return new Headers(map);
    }

    private void sendResponse(
            final StartLine startLine
            , final Headers headers
            , final BufferedOutputStream bufferedStream
    ) throws IOException {
        final String path = startLine.extractPath();
        if ("/".equals(path)) {
            bufferedStream.write(OK_RESPONSE.getBytes());
        } else if (path.contains("/echo/")) {
            final HttpResponse response = HttpResponse.ofPlainText(startLine.extractResourceId());
            bufferedStream.write(response.toString().getBytes());
        } else if (path.contains("/user-agent")) {
            final HttpResponse response = HttpResponse.ofPlainText(headers.headerValue("User-Agent").orElseThrow(() -> new RuntimeException("user-agent 에 값이 없습니다")));
            bufferedStream.write(response.toString().getBytes());
        } else if (path.contains("/files/")) {
            final var absoluteFilePath = this.parentAbsolutePath + startLine.extractResourceId();

            System.out.println("absolutepath " + absoluteFilePath);
            try {
                final String fileContent = readFromFile(absoluteFilePath);
                bufferedStream.write(HttpResponse.ofFile(fileContent).toString().getBytes());
            } catch (InvalidPathException ex) {
                bufferedStream.write(NOT_FOUND_RESOURCE_RESPONSE.getBytes());
            }
        } else {
            bufferedStream.write(NOT_FOUND_RESOURCE_RESPONSE.getBytes());
        }
    }


    private static String readFromFile(String path) {
        final var filePath = Paths.get(path);
        try (final var bufferedFileReader = Files.newBufferedReader(filePath)) {
            var readLines = bufferedFileReader.lines();
            return readLines
                    .peek(line -> System.out.println("LINE : " + line))
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * In this case where Http Message does not contain any message body,
     * Just detecting Header terminator ( CRLF Followed by CRLF )
     * is only thing reading Http message.
     */
    private static String readHttpMessageNotContainingBody(InputStream inputStream) throws IOException {
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

        char[] finalMessage = truncateAndCreateArray(tempMessageContainer, curIdx - 4);

        return new String(finalMessage);
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

        return new StartLine(messageParts.get(0));
    }
}
