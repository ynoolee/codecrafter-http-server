public class Main {
    public static void main(String[] args) {

        // check program running args
        int count = 0;

        for (String arg : args) {
            System.out.printf("%dth arg : %s\n", count++, arg);
        }

        if (args.length >= 2) {
            final HttpServer httpServer = new HttpServer(4221, args[1]);
            httpServer.run();
        } else {
            System.out.println("Any Parent directory path information has been passed");
        }
    }
}
