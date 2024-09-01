import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class NioHttpServer {

    private static final Logger logger = Logger.getLogger(HttpServer.class.getName());
    private final String parentAbsolutePath;

    private final int port;

    public NioHttpServer(final int port, final String parentAbsolutePath) {
        this.parentAbsolutePath = parentAbsolutePath;
        this.port = port;
    }

    private void registerChannelOfEvent(Selector selector, int waitingEvent) throws IOException {
        var channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        channel.bind(new InetSocketAddress(this.port));
        channel.register(selector, waitingEvent);
        logger.info("Register ServerSocketChannel on port " + this.port);
    }

    public void run() {
        // Selector 생성
        try (
            final var selector = Selector.open();
        ) {
            // == 다수의 Channel 을 생성 및 Selector 에 등록 ===
            // non-blocking ServerSocketChannel 을 생성
            // ServerSocketChannel 을 Selector 의 관리 대상으로 등록
            registerChannelOfEvent(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                // Selector 의 select 를 호출 (blocking)
                selector.select();
                logger.info("There's some channel to be ready");
                var selectedKeys = selector.selectedKeys().iterator();
                // Selector 로부터 selectedKey set 을 가져옴
                while (selectedKeys.hasNext()) {
                    var selectedKey = selectedKeys.next();
                    // ready channel 들 중 acceptable 상태인 Channel 을 가져옴
                    // accept 가능한 serverSocket ( 즉, 어떤 connection 요청을 받은 ServerSocket)
                    if (selectedKey.isAcceptable()) {
                        // Channel 로부터 client SocketChannel 을 생성함
                        // SocketChannel 은 READ operation 에 대한 모니터링을 위해 Selector 에 등록함
                        // ( Write operation 에 대해서는 등록하지 않아도 됨 )
                        var serverSocketChannel = (ServerSocketChannel) selectedKey.channel();
                        var clientSocketChannel = serverSocketChannel.accept();
                        clientSocketChannel.configureBlocking(false);
                        clientSocketChannel.register(selector, SelectionKey.OP_READ);
                        logger.info("accept new connection");
                    }

                    if (selectedKey.isReadable()) {
                        // SocketChannel 을 사용해 이전에 일반 Socket 을 사용해서 처리하던 작업들을 해야 함 (Buffer 를 사용해서)
                        // Buffer 를 사용할 때는 flip 을 통해 mode 를 바꿔가며 데이터를 read/write 해야 함
                        //todo: ByteBuffer 를 계속해서 재사용 / 매 번 생성
                        var clientSocket = (SocketChannel) selectedKey.channel();
                        var buffer = ByteBuffer.allocate(1024);

                        clientSocket.read(buffer);

                        buffer.flip();
                        try (
                            InputStream is = new ByteArrayInputStream(buffer.array(), buffer.position(), buffer.remaining());
                        ) {
                            final HttpRequest message = HttpMessageParseUtil.readHttpRequestMessage(is);

                            buffer.flip();
                            buffer.clear();
                            sendResponse(message, buffer);
                            clientSocket.write(buffer);
                        }
                        clientSocket.close();
                    }
                    selectedKeys.remove();
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendResponse(
        final HttpRequest request
        , final ByteBuffer buffer
    ) throws IOException {
        final StartLine startLine = request.getStartLine();
        final String path = startLine.extractPath();
        final HttpMethod method = startLine.method();

        if (HttpMethod.POST.equals(method)) {
            hanldePostMethod(request, buffer, path);
        } else if (HttpMethod.GET.equals(method)) {
            handleGetMethod(buffer, path, request);
        }
        buffer.flip();
    }

    private void handleGetMethod(final ByteBuffer output, final String path, HttpRequest request) throws IOException {
        final StartLine startLine = request.getStartLine();
        if ("/".equals(path)) {
            output.put(CommonHttpResponse.OK_RESPONSE.getBytes());
        } else if (path.contains("/echo/")) {
            final HttpResponse response = HttpResponse.of(startLine.extractResourceId(), HttpHeader.ContentType.TEXT_PLAIN);
            output.put(response.toString().getBytes());
        } else if (path.contains("/user-agent")) {
            final HttpResponse response =
                HttpResponse.of(
                    request.valueOfKey(HttpHeader.USER_AGENT).orElseThrow(() -> new RuntimeException("user-agent 에 값이 없습니다"))
                    , HttpHeader.ContentType.BINARY_DATE
                );
            output.put(response.toString().getBytes());
        } else if (path.contains("/files/")) {
            writeFileToResponse(output, this.parentAbsolutePath + startLine.extractResourceId());
        } else {
            output.put(CommonHttpResponse.NOT_FOUND_RESOURCE_RESPONSE.getBytes());
        }
    }

    private void writeFileToResponse(final ByteBuffer output, final String absoluteFilePath) throws IOException {
        try {
            final String fileContent = readFromFile(absoluteFilePath);
            output.put(HttpResponse.of(fileContent, HttpHeader.ContentType.BINARY_DATE).toString().getBytes());
        } catch (Exception ex) {
            output.put(CommonHttpResponse.NOT_FOUND_RESOURCE_RESPONSE.getBytes());
        }
    }

    private void hanldePostMethod(final HttpRequest request, final ByteBuffer output, final String path) throws IOException {
        final StartLine startLine = request.getStartLine();
        if (path.contains("/files/")) {
            final String requestBody = request.getBody();
            logger.info("Read RequestBody : " + requestBody);
            final String resourceId = startLine.extractResourceId();
            final String absolutePath = this.parentAbsolutePath + resourceId;
            try (final FileWriter writer = new FileWriter(absolutePath)) {
                logger.info("File path :" + absolutePath);
                writer.write(requestBody);
                writer.flush();
            }
            logger.info("Complete Writing file");
            output.put(CommonHttpResponse.CREATED.getBytes());
        } else {
            output.put(CommonHttpResponse.NOT_FOUND_RESOURCE_RESPONSE.getBytes());
        }
    }

    private static String readFromFile(String path) {
        final var filePath = Paths.get(path);
        try {
            final var contents = Files.readAllBytes(filePath);
            return new String(contents);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
