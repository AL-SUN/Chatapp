package Server;

import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;
import java.util.*;
import static Utils.ResourceLoader.loadProperties;


public class Server {

    private static String SAVE_PATH;

    public static final Map<String, SSLSocket> sockets = new LinkedHashMap<>();
    public static final String ADMIN = "System";
    private final int localPort;
    private SSLServerSocket server;
    private SSLServerSocket[] fileServers;
    private final String chatRoomName;


    public Server(int localPort, String chatRoomName) throws Exception {
        Properties properties = loadProperties();
        SAVE_PATH = properties.getProperty("server.tmpdir"); //TODO: Change this path in config.properties

        this.localPort = localPort;
        this.chatRoomName = chatRoomName;

        // Load server keystore
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream("server.keystore"), "123456".toCharArray());

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, "123456".toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

        SSLServerSocketFactory socketFactory = sslContext.getServerSocketFactory();
        server = (SSLServerSocket) socketFactory.createServerSocket(localPort);

        // Initialize file servers
        fileServers = new SSLServerSocket[5];
        for (int i = 0; i < fileServers.length; i++) {
            fileServers[i] = (SSLServerSocket) socketFactory.createServerSocket(localPort + i + 1);
        }

        startListen();
    }

    private void startListen() throws IOException {
        createClientHandlerThread(server, true);

        for (SSLServerSocket fileServer : fileServers) {
            createClientHandlerThread(fileServer, false);
        }
    }

    /**
     * Create a new thread to handle client connections
     * @param serverSocket the server socket to accept connections
     * @param isMainServer whether this server is the main server
     */
    private void createClientHandlerThread(SSLServerSocket serverSocket, boolean isMainServer) {
        new Thread(() -> {
            try {
                while (true) {
                    SSLSocket socket = (SSLSocket) serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String username = in.readLine();
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                    if (isMainServer) {
                        out.write(chatRoomName + "\r\n");
                        out.flush();
                        synchronized (sockets) {
                            if(!username.equals("Guest")) {
                                broadcast(ADMIN, "Welcome " + username);
                            }
                            sockets.put(getAddress(socket), socket);
                            ChatRoom.printStatus();
                        }
                    } else {
                        synchronized (sockets) {
                            sockets.put(getAddress(socket), socket);
                            ChatRoom.printStatus();
                        }
                    }

                    new Thread(new MsgTask(this, SAVE_PATH, socket, in, out, username)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public int currentClients() {
        return sockets.size();
    }

    public void shutdown() throws IOException, InterruptedException {
        if (server == null) return;
        broadcast(ADMIN, "The chatroom is about to close");
        Thread.sleep(1000);
        synchronized (sockets) {
            for (SSLSocket socket : sockets.values()) {
                socket.shutdownInput();
                socket.shutdownOutput();
                socket.close();
            }
            sockets.clear();
        }
        server.close();
    }

    public int broadcast(String from, String msg) throws IOException {
        int msgId= new DatabaseConnection().saveMsg(from, msg);
        ChatRoom.saveMsg(from + ": " + msg);
        PrintWriter out;
        synchronized (sockets) {
            for (SSLSocket socket : sockets.values()) {
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "GBK"));
                out.write(from + ": " + msg + "\r\n");
                out.flush();
            }
        }
        return msgId;
    }

    public void OfflineMsg(SSLSocket skt) throws IOException {
        PrintWriter out;
        synchronized (ChatRoom.msgList) {   
            for (String str : ChatRoom.msgList) {
                out = new PrintWriter(new OutputStreamWriter(skt.getOutputStream(), "GBK"));
                out.write(str + "\r\n");
                out.flush();
            }
        }
    }


    public String getAddress(SSLSocket socket) {
        return socket.getInetAddress() + ":" + socket.getPort();
    }

    public int getServerLocalPort() {
        return localPort;
    }

}