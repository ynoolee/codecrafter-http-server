import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

record Headers(
        Map<HttpHeader, String> headers
) {

    public Headers(final Map<HttpHeader, String> headers) {
        this.headers = Map.copyOf(headers);
    }

    public Headers deepCopy() {
        final Map<HttpHeader, String> newMap = new HashMap<>();
        headers.keySet().forEach(key -> newMap.put(key, headers.get(key)));

        return new Headers(newMap);
    }

    public Optional<String> valueOfKey(HttpHeader header) {
        return Optional.ofNullable(headers.get(header));
    }
}
