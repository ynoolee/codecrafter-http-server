public class Main {
    public static void main(String[] args) {
        int count = 0;
        for (String arg : args) {
            System.out.printf("%dth arg : %s\n", count++, arg);
        }
        final HttpServer httpServer = new HttpServer();
        httpServer.run(4221);
    }
}
