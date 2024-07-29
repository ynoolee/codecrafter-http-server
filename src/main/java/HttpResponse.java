import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public record HttpResponse<T>(

        String statusLine

        , Map<String, String> headers

        , T body
) {
    private final static String CRLF = "\r\n";

    private final static String OK = "HTTP/1.1 200 OK";

    private final static String CONTENT_TYPE_HEADER = "Content-Type";

    private final static String CONTENT_LENGTH_HEADER = "Content-Length";

    // todo : 일단은 file data 도 stream 으로 출력
    public static HttpResponse<String> ofPlainText(String body) {

        final Map<String, String> map = new HashMap<>();
        map.put(CONTENT_TYPE_HEADER, CONTENT_TYPE.TEXT_PLAIN.value + CRLF);
        map.put(CONTENT_LENGTH_HEADER, String.valueOf(body.getBytes().length) + CRLF);

        return new HttpResponse<String>(OK, map, body);
    }

    public static HttpResponse<Byte[]> ofBinary(byte[] body) {
        final Map<String, String> map = new HashMap<>();
        map.put(CONTENT_TYPE_HEADER, CONTENT_TYPE.BINARY_DATE.value + CRLF);
        map.put(CONTENT_LENGTH_HEADER, body.length + CRLF);

        // convert byte[] -> Byte[]
        final Byte[] bytes = new Byte[body.length];
        Arrays.setAll(bytes, e -> body[e]);

        return new HttpResponse<>(OK, map, bytes);
    }

    private enum CONTENT_TYPE {

        TEXT_PLAIN("text/plain")
        , BINARY_DATE("application/octet-stream")
        ;

        private final String value;

        CONTENT_TYPE(final String value) {
            this.value = value;
        }
    }

    @Override
    public String toString() {

        final String header = this.headers.keySet().stream()
                .map(key -> key + ": " + this.headers.get(key))
                .collect(Collectors.joining());

        return new StringBuilder(this.statusLine)
                .append(CRLF)
                .append(header)
                .append(CRLF)
                .append(body)
                .toString();
    }
}
