import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum HttpHeader {

    USER_AGENT("User-Agent")
    , CONTENT_TYPE("Content-Type")
    , CONTENT_LENGTH("Content-Length")
    ;

    private static final Map<String, HttpHeader> headerMap = new HashMap<>();

    static {
        Arrays.stream(HttpHeader.values())
                .forEach(key -> headerMap.put(key.getValue(), key));
    }

    private final String value;

    HttpHeader(final String value) {
        this.value = value;
    }

    public static Optional<HttpHeader> of(String headerKey) {
        return Optional.ofNullable(headerMap.get(headerKey));
    }

    public String getValue() {
        return value;
    }

    public enum ContentType {

        TEXT_PLAIN("text/plain")
        , BINARY_DATE("application/octet-stream")
        ;

        private final String detailType;

        ContentType(final String detailType) {
            this.detailType = detailType;
        }

        public String getDetailType() {
            return detailType;
        }
    }
}
