import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record Headers(
        Map<String, String> headers
) {

    public Headers(final Map<String, String> headers) {
        this.headers = Map.copyOf(headers);
    }

    public Headers deepCopy() {
        final Map<String, String> newMap = new HashMap<>();
        headers.keySet().forEach(key -> newMap.put(key, headers.get(key)));

        return new Headers(newMap);
    }

    public Optional<String> headerValue(String headerKey) {
        return Optional.ofNullable(headers.get(headerKey));
    }

    public enum Header {

        USER_AGENT("User-Agent")
        , CONTENT_TYPE_HEADER("Content-Type")
        , CONTENT_LENGTH_HEADER("Content-Length")
        ;

        private final String value;

        Header(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
