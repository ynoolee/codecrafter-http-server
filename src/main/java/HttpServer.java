import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class HttpServer {

    private static final String OK_RESPONSE = "HTTP/1.1 200 OK\r\n\r\n";
    private static final String NOT_FOUND_RESOURCE_RESPONSE = "HTTP/1.1 404 Not Found\r\n\r\n";

    public void run(int port) {

        try (ServerSocket serverSocket = new ServerSocket(port)) {
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

            // todo : Have to search another way for main thread to wait all jobs are completed
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
            final BufferedOutputStream bufferedStream = new BufferedOutputStream(outputStream);

            final String message = readHttpMessageNotContainingBody(inputStream);

            List<String> parts = parse(message, "\r\n");

            final StartLine startLine = createStartLine(parts);
            final Headers headers = createHeaders(parts.subList(1, parts.size()));

            sendResponse(startLine, headers, bufferedStream);

            bufferedStream.flush();
        }
        System.out.println("Turn off HTTP Server");
    }

    private Headers createHeaders(final List<String> headers) {
        Map<String, String> map = new HashMap<>();

        for (String header : headers) {
            String[] part = Arrays.stream(header.split(":")).map(String::trim).toArray(String[]::new);
            map.put(part[0], part[1]);
        }
        return new Headers(map);
    }

    private static void sendResponse(
            final StartLine startLine
            , final Headers headers
            , final BufferedOutputStream bufferedStream
    ) throws IOException {
        if ("/".equals(startLine.extractPath())) {
            bufferedStream.write(OK_RESPONSE.getBytes());
        } else if (startLine.extractPath().contains("/echo/")) {
            final HttpResponse response = HttpResponse.of(startLine.extractResourceId());
            bufferedStream.write(response.toString().getBytes());
        } else if (startLine.extractPath().contains("/user-agent")) {
            final HttpResponse response = HttpResponse.of(headers.headerValue("User-Agent").orElseThrow(() -> new RuntimeException("user-agent 에 값이 없습니다")));
            bufferedStream.write(response.toString().getBytes());
        } else {
            bufferedStream.write(NOT_FOUND_RESOURCE_RESPONSE.getBytes());
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
