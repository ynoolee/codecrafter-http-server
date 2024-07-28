import java.util.Arrays;
import java.util.List;

public record StartLine(

        String entireLine
) {

    public String extractPath() {
        final List<String> splitStartLine = parse(this.entireLine, " ");

        if (splitStartLine == null || splitStartLine.isEmpty() || splitStartLine.size() < 2) {
            throw new RuntimeException("target 이 비어있습니다");
        }

        return splitStartLine.get(1);
    }

    public String extractResourceId() {
        String path = extractPath();

        final String[] parts = path.split("/");
        return parts[2];
    }

    private List<String> parse(String request, String delimiter) {
        return Arrays.stream(request.split(delimiter)).toList();
    }

}
