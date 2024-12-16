package Server;

import java.io.IOException;
import java.util.*;

public class ChatRoom {
    public static String name;
    private static int localPort;
    public static List<String> msgList;
    private static Server server;

    public static void create(String name, int localPort) {
        ChatRoom.name = name;
        ChatRoom.localPort = localPort;
        ChatRoom.msgList = new LinkedList<>();
        try {
            server = new Server(localPort, name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void close() {
        try {
            server.shutdown();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void printStatus() {
        if (server == null) {
            System.out.println("Error: Failed to create a chatroom!");
            return;
        }
        System.out.println();
        System.out.println("Chatroom: "+ name);
        System.out.println("Concurrent Users: " + server.currentClients() + " | Port: " + localPort);
        System.out.println("----------------------------");
        for (String msg : msgList) System.out.println(msg);
    }

    public static void saveMsg(String msg) {
        msgList.add(msg);
    }
}