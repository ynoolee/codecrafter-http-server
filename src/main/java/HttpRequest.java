import java.util.Optional;

public class HttpRequest {

    private final StartLine startLine;
    private final Headers headers;
    private final String requestBody;

    public HttpRequest(final StartLine startLine, final Headers headers, final String requestBody) {
        this.startLine = startLine;
        this.headers = headers;
        this.requestBody = requestBody;
    }

    public boolean hasMessageBody() {
        return headers.headerValue(HttpHeader.CONTENT_LENGTH_HEADER).isEmpty();
    }

    public Optional<String> headerValue(HttpHeader header) {
        return this.headers.headerValue(header);
    }

    public HttpRequest createWithNewBody(String body) {
        return HttpRequest.HttpRequestBuilder.builder()
                .startLine(startLine)
                .headers(headers.deepCopy())
                .responseBody(body)
                .build();
    }

    public StartLine getStartLine() {
        return startLine;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public static class HttpRequestBuilder {
        private StartLine startLine;
        private Headers headers;
        private String responseBody;

        private HttpRequestBuilder(final StartLine startLine, final Headers headers, final String responseBody) {
            this.startLine = startLine;
            this.headers = headers;
            this.responseBody = responseBody;
        }

        public static HttpRequestBuilder builder() {
            return new HttpRequestBuilder(null, null, null);
        }

        public HttpRequestBuilder startLine(StartLine startLine) {
            return new HttpRequestBuilder(startLine, this.headers, this.responseBody);
        }

        public HttpRequestBuilder headers(Headers headers) {
            return new HttpRequestBuilder(this.startLine, headers.deepCopy(), this.responseBody);
        }

        public HttpRequestBuilder responseBody(String responseBody) {
            return new HttpRequestBuilder(this.startLine, this.headers, responseBody);
        }

        public HttpRequest build() {
            return new HttpRequest(startLine, headers, responseBody);
        }
    }
}
