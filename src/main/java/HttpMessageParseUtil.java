import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class HttpMessageParseUtil {

    private static final Logger logger = Logger.getLogger(HttpMessageParseUtil.class.getName());

    public static HttpRequest readHttpRequestMessage(InputStream inputStream) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        HttpRequest request = createHttpRequestUntilBody(reader);

        if (request.hasEmptyBody()) {
            return request;
        }

        final int contentLength = request.valueOfKey(HttpHeader.CONTENT_LENGTH)
            .map(Integer::valueOf).orElse(0);

        logger.info("content length : " + contentLength);
        StringBuilder content = new StringBuilder();
        int contentSize = 0;
        while (contentSize < contentLength) {
            String readCharStr = String.valueOf((char) reader.read());
            int readCharSize = readCharStr.getBytes().length;
            content.append(readCharStr);
            contentSize += readCharSize;
        }

        return request.withBody(content.toString());
    }

    public static HttpRequest readHttpRequestMessage(CharBuffer buffer) throws IOException {
        HttpRequest request = createHttpRequestUntilBody(buffer);

        if (request.hasEmptyBody()) {
            return request;
        }

        final int contentLength = request.valueOfKey(HttpHeader.CONTENT_LENGTH)
            .map(Integer::valueOf).orElse(0);

        logger.info("content length : " + contentLength);
        StringBuilder content = new StringBuilder();
        int contentSize = 0;
        while (contentSize < contentLength) {
            String readCharStr = String.valueOf(buffer.get());
            int readCharSize = readCharStr.getBytes().length;
            content.append(readCharStr);
            contentSize += readCharSize;
        }

        return request.withBody(content.toString());
    }

    private static HttpRequest createHttpRequestUntilBody(final BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        int curIdx = 0;

        // read until header terminator from inputstream
        while (true) {
            char readChar = (char) reader.read();
            sb.append(readChar);
            curIdx++;
            // detect Header terminator
            if (isHeaderTermination(readChar, sb, curIdx)) {
                break;
            }
        }

        return createHttpRequest(sb.toString());
    }

    private static HttpRequest createHttpRequestUntilBody(CharBuffer buffer) throws IOException {
        StringBuilder sb = new StringBuilder();
        int curIdx = 0;

        while (true) {
            char readChar = (char) buffer.get();
            sb.append(readChar);
            curIdx++;
            if (isHeaderTermination(readChar, sb, curIdx)) {
                break;
            }
        }

        return createHttpRequest(sb.toString());
    }

    private static boolean isHeaderTermination(char readChar, StringBuilder sb, int curIdx) {
        if (readChar == '\n') {
            return curIdx >= 4
                && sb.charAt(curIdx - 2) == '\r'
                && sb.charAt(curIdx - 3) == '\n'
                && sb.charAt(curIdx - 4) == '\r';
        }
        return false;
    }

    private static HttpRequest createHttpRequest(final String startLineAndHeaders) {
        List<String> lines = Arrays.stream(startLineAndHeaders.split("\r\n")).toList();

        final StartLine startLine = createStartLine(lines); // first line is start-line
        final Headers headers = createHeaders(lines.subList(1, lines.size())); // The rest part is headers

        return HttpRequest.builder()
            .startLine(startLine)
            .headers(headers.deepCopy())
            .build();
    }

    private static Headers createHeaders(final List<String> headers) {
        Map<HttpHeader, String> map = new HashMap<>();

        headers.stream()
            .map(headerLine -> Arrays.stream(headerLine.split(":")).map(String::trim).toArray(String[]::new))
            .forEach(headerKeyValue -> HttpHeader.of(headerKeyValue[0]).map(key -> map.put(key, headerKeyValue[1])));
        return new Headers(map);
    }

    private static StartLine createStartLine(List<String> messageParts) {
        if (messageParts == null || messageParts.isEmpty()) {
            throw new RuntimeException("message 가 비어있습니다");
        }

        return StartLine.of(messageParts.get(0));
    }
}
