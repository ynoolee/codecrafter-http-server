import java.util.Arrays;
import java.util.List;

public record StartLine(

        String entireLine,

        List<String> parts,

        HttpMethod method
) {

    public static StartLine of(final String entireLine) {
        final List<String> splitStartLine = parse(entireLine, " ");

        return new StartLine(entireLine, splitStartLine, findMethod(splitStartLine));
    }

    private static HttpMethod findMethod(List<String> parts) {
        if (parts == null || parts.isEmpty()) {
            throw new RuntimeException("HTTP 1/1 을 만족하는 요청이 아닙니다");
        }
        return HttpMethod.valueOf(parts.get(0));
    }

    public String extractPath() {
        if (parts == null || parts.isEmpty() || parts.size() < 2) {
            throw new RuntimeException("target 이 비어있습니다");
        }

        return parts.get(1);
    }

    public String extractResourceId() {
        String path = extractPath();

        final String[] parts = path.split("/");
        return parts[2];
    }

    private static List<String> parse(String request, String delimiter) {
        return Arrays.stream(request.split(delimiter)).toList();
    }

}
