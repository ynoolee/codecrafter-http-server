import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpMessageParseUtil {

    public static HttpRequest readHttpRequestMessage(InputStream inputStream) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        final char[] tempMessageContainer = new char[4096];
        int curIdx = 0;

        // read until header terminator from inputstream
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

        if (request.hasMessageBody()) {
            return request;
        }

        final Integer contentLength =
                request.headerValue(HttpHeader.CONTENT_LENGTH_HEADER)
                        .map(Integer::valueOf).orElse(0);
        char[] contents = new char[contentLength];
        for (int i = 0; i < contentLength; i++) {
            contents[i] = (char) reader.read();
        }

        return request.createWithNewBody(String.valueOf(contents));
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


    private static Headers createHeaders(final List<String> headers) {
        Map<HttpHeader, String> map = new HashMap<>();

        for (String header : headers) {
            String[] part = Arrays.stream(header.split(":")).map(String::trim).toArray(String[]::new);
            HttpHeader.of(part[0]).map(key -> map.put(key, part[1]));
        }
        return new Headers(map);
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
