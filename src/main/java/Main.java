public class Main {
    public static void main(String[] args) {
        for (String arg : args) {
            System.out.println("arg : "+ arg);
        }
        final HttpServer httpServer = new HttpServer();
        httpServer.run(4221);
    }
}
