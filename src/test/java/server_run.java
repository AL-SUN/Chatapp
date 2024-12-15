import Server.ChatRoom;
//import client.ClientUI;

public class server_run {
    public static void main(String[] args) {
//        ChatRoom.create("Ahhhhhhhhh!", 60001);
        ChatRoom.create("Ahhhhhhhhh", 9090); // TODO: test in local
        System.out.println("Server is running!");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //ClientUI c1 = new ClientUI();
        //c1.createClient("cjcj", "127.0.0.1", 9090);
        //try {
        //   Thread.sleep(3000);
       // } catch (InterruptedException e) {
        //    e.printStackTrace();
       // }

      // ClientUI c2 = new ClientUI();
      // c2.createClient("djdj", "127.0.0.1", 9090);

       //Server.ChatRoom.close();
    }
}