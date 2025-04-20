public abstract class HttpServer {
    protected final int port;
    protected final String parentAbsolutePath;

    protected HttpServer(final int port, final String parentAbsolutePath) {
        this.port = port;
        this.parentAbsolutePath = parentAbsolutePath;
    }

    public abstract void run();
}
