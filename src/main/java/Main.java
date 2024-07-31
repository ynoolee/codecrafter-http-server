public class Main {
    public static void main(String[] args) {

        // check program running args
        int count = 0;

        for (String arg : args) {
            System.out.printf("%dth arg : %s\n", count++, arg);
        }

        final HttpServer httpServer = new HttpServer(4221);

        if (args.length >= 2) {
            httpServer.runWithFileReadPath(args[1]);
        }
    }
}
