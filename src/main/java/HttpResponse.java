import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public record HttpResponse(

        String statusLine

        , Map<String, String> headers

        , String body
) {
    private final static String CRLF = "\r\n";

    private final static String OK = "HTTP/1.1 200 OK";

    private final static String CONTENT_TYPE_HEADER = "Content-Type";

    private final static String CONTENT_LENGTH_HEADER = "Content-Length";

    public static HttpResponse of(String body) {

        final Map<String, String> map = new HashMap<>();
        map.put(CONTENT_TYPE_HEADER, CONTENT_TYPE.TEXT_PLAIN.value + CRLF);
        map.put(CONTENT_LENGTH_HEADER, String.valueOf(body.getBytes().length) + CRLF);

        return new HttpResponse(OK, map, body);
    }

    private enum CONTENT_TYPE {

        TEXT_PLAIN("text/plain")
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
