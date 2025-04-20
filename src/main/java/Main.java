public class Main {

    public static void main(String[] args) {

        // check program running args
        int count = 0;

        for (String arg : args) {
            System.out.printf("%dth arg : %s\n", count++, arg);
        }

        String absoluteParentPath = null;
        if (args.length >= 2) {
            absoluteParentPath = args[1];
        }

        var httpServer = new MultiThreadedHttpServer(4221, absoluteParentPath);
        httpServer.run();
    }
}
