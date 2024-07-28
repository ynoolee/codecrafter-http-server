import java.util.Map;
import java.util.Optional;

public record Headers(
        Map<String, String> headers
) {

    public Headers(final Map<String, String> headers) {
        this.headers = Map.copyOf(headers);
    }

    public Optional<String> headerValue(String headerKey) {
        return Optional.ofNullable(headers.get(headerKey));
    }

    public enum Header {
        USER_AGENT("User-Agent");

        private final String value;

        Header(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
