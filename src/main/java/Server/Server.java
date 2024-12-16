package Server;

import java.io.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import static Utils.ResourceLoader.loadProperties;


public class Server {

    private static String SAVE_PATH;

    public static final Map<String, Socket> sockets = new LinkedHashMap<>();
    public static final String ADMIN = "System";
    private final int localPort;
    private ServerSocket server;
    private ServerSocket[] fileServers;
    private final String chatRoomName;


    public Server(int localPort, String chatRoomName) throws IOException {
        Properties properties = loadProperties();
        SAVE_PATH = properties.getProperty("server.filepath"); //TODO: Change this path in config.properties

        this.localPort = localPort;
        this.chatRoomName = chatRoomName;
        startListen();
    }

    private void startListen() throws IOException {
        server = new ServerSocket(localPort);
        createClientHandlerThread(server, true);

        // Create 5 file servers
        fileServers = new ServerSocket[5];
        for (int i = 0; i < fileServers.length; i++) {
            fileServers[i] = new ServerSocket(localPort + i + 1);
            createClientHandlerThread(fileServers[i], false);
        }
    }

    /**
     * Create a new thread to handle client connections
     * @param serverSocket the server socket to accept connections
     * @param isMainServer whether this server is the main server
     */
    private void createClientHandlerThread(ServerSocket serverSocket, boolean isMainServer) {
        new Thread(() -> {
            try {
                while (true) {
                    Socket socket = serverSocket.accept();
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
            for (Socket socket : sockets.values()) {
                socket.shutdownInput();
                socket.shutdownOutput();
                socket.close();
            }
            sockets.clear();
        }
        server.close();
    }

    public void broadcast(String from, String msg) throws IOException {
        new DatabaseConnection().saveMsg(from, msg);
        ChatRoom.saveMsg(from + ": " + msg);
        PrintWriter out;
        synchronized (sockets) {
            for (Socket socket : sockets.values()) {
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "GBK"));
                out.write(from + ": " + msg + "\r\n");
                out.flush();
            }
        }
    }

    public void OfflineMsg(Socket skt) throws IOException {
        PrintWriter out;
        synchronized (ChatRoom.msgList) {   
            for (String str : ChatRoom.msgList) {
                out = new PrintWriter(new OutputStreamWriter(skt.getOutputStream(), "GBK"));
                out.write(str + "\r\n");
                out.flush();
            }
        }
    }


    public String getAddress(Socket socket) {
        return socket.getInetAddress() + ":" + socket.getPort();
    }

    public int getServerLocalPort() {
        return localPort;
    }

}