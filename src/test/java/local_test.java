import Server.ChatRoom;
import client.QQLoginUI;
//import client.ClientUI;

public class local_test {
    public static void main(String[] args) {
//        ChatRoom.create("Ahhhhhhhhh!", 60001);
        ChatRoom.create("Ahhhhhhhhh", 9090); // TODO: match the port in config
        System.out.println("Server is running!");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new QQLoginUI(); // new UI, take a look!
        new QQLoginUI(); // new UI, take a look!

//       Server.ChatRoom.close();
    }
}