import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public record HttpResponse(

        String statusLine

        , Map<String, String> headers

        , String body
) {
    private final static String CRLF = "\r\n";

    public static HttpResponse of(String body, HttpHeader.ContentType contentType) {
        Map<String, String> map = new HashMap<>();
        map.put(HttpHeader.CONTENT_TYPE.getValue(), contentType.getDetailType() + CRLF);
        map.put(HttpHeader.CONTENT_LENGTH.getValue(), body.getBytes().length + CRLF);

        return new HttpResponse(CommonHttpResponse.OK_RESPONSE, map, body);
    }

    @Override
    public String toString() {

        final String header = this.headers.keySet().stream()
                .map(key -> key + ": " + this.headers.get(key))
                .collect(Collectors.joining());

        return this.statusLine + CRLF +
                header + CRLF +
                body;
    }
}
