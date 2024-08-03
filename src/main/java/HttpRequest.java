import java.util.Optional;

public class HttpRequest {
    private final StartLine startLine;
    private final Headers headers;
    private final String body;

    private HttpRequest(Builder builder) {
        this.startLine = builder.startLine;
        this.headers = builder.headers;
        this.body = builder.body;
    }

    public boolean hasEmptyBody() {
        return headers.valueOfKey(HttpHeader.CONTENT_LENGTH).isEmpty();
    }

    public Optional<String> valueOfKey(HttpHeader header) {
        return headers.valueOfKey(header);
    }

    public HttpRequest withBody(String newBody) {
        return new Builder(this)
                .body(newBody)
                .build();
    }

    public StartLine getStartLine() {
        return startLine;
    }

    public String getBody() {
        return body;
    }

    public static class Builder {
        private StartLine startLine;
        private Headers headers;
        private String body;

        public Builder() {}

        private Builder(HttpRequest request) {
            this.startLine = request.startLine;
            this.headers = request.headers.deepCopy();
            this.body = request.body;
        }

        public Builder startLine(StartLine startLine) {
            this.startLine = startLine;
            return this;
        }

        public Builder headers(Headers headers) {
            this.headers = headers;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public HttpRequest build() {
            return new HttpRequest(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
