import java.io.FileWriter;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        int count = 0;
        for (String arg : args) {
            System.out.printf("%dth arg : %s\n", count++, arg);
        }
        try (final FileWriter fileWriter = new FileWriter(args[1]);) {
            fileWriter.write("HEllo world!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final HttpServer httpServer = new HttpServer();
        httpServer.run(4221);
    }
}
