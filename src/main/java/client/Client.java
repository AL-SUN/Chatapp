package client;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class Client {
    private final String ip;
    private final int port;
    private final ChatroomUI UI;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private DataOutputStream dos;
    private String savePath;
    private Integer file_num;
    private String Client_username;
    public String chatRoomName;

    public Client(String username, String ip, int port, ChatroomUI UI) throws IOException {
        this.ip = ip;
        this.port = port;
        this.UI = UI;
        this.file_num = 0;
        this.Client_username = username;
        connect(username);
    }

    public void connect(String username) throws IOException {
        socket = new Socket(ip, port);

        if (!socket.getTcpNoDelay()) socket.setTcpNoDelay(true);

        if (!socket.getKeepAlive()) socket.setKeepAlive(true);

        if (!socket.getOOBInline()) socket.setOOBInline(true);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "GBK"));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "GBK"));
        dos = new DataOutputStream(socket.getOutputStream());
        speak(username);
        chatRoomName = in.readLine();
        new Thread(() -> {
            boolean isFile = false;
            try {
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                while (true) {
                    String str = in.readLine();
                    if (str == null) break;
                    if ("download".equals(str)){
                        String fileName = dis.readUTF();
                        long fileLen = dis.readLong();
                        File directory = new File(savePath);
                        File file = new File(directory.getAbsolutePath() + File.separatorChar + fileName);
                        FileOutputStream fos = new FileOutputStream(file);
                        byte[] bytes = new byte[1024];
                        int total = 0;
                        int length;
                        while (total < fileLen && (length = dis.read(bytes, 0, bytes.length)) != -1) {
                            total += length;
                            fos.write(bytes, 0, length);
                            fos.flush();
                        }
                        fos.close();
                    }
                    else UI.showMsg(str); // broadcast message
                }
            } catch (IOException e) {
                if (!(e instanceof SocketException)) e.printStackTrace();
            }
        }).start();
        new Thread(() -> {
            final String heartbeat = "[usage for heartbeat packet]";
            while (true) {
                try {
                    Thread.sleep(20 * 1000); // send a heartbeat packet every 20s
                    out.write(heartbeat + "\r\n");
                    out.flush();
                    try {
                        socket.sendUrgentData(0xFF);
                    } catch (IOException ex) {
                        //UI.showDialog("Lost connection to the server");
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void disconnect() throws IOException, InterruptedException {
        speak("exit");
        Thread.sleep(1000);
        socket.shutdownInput();
        socket.shutdownOutput();
        socket.close();
    }

    public void disconnect_file(Socket skt) throws IOException, InterruptedException {
        speak_file("exit", skt);
        Thread.sleep(1000);
        skt.shutdownInput();
        skt.shutdownOutput();
        skt.close();
    }

    public void speak(String str) {
        out.write(str + "\r\n");
        out.flush();
    }

    public void speak_file(String str, Socket skt) throws IOException {
        System.out.println(str); // for debugging
        PrintWriter out_file;
        out_file = new PrintWriter(skt.getOutputStream());
        out_file.write(str + "\r\n");
        out_file.flush();
    }

    public void sendFile(File file, String ip, int port) throws IOException, InterruptedException {
        if (!file.exists()){
            System.err.println("File not found!");
            return;
        }
        new Thread(() -> {
            try {
                synchronized (file_num) {
                    file_num = Integer.valueOf(1 + file_num.intValue());
                }
                Socket socket_file = new Socket(ip, port + file_num.intValue());
                DataOutputStream dos_file = new DataOutputStream(socket_file.getOutputStream());

                speak_file(this.Client_username, socket_file);
                speak_file("upload", socket_file);
                Thread.sleep(500);
                FileInputStream fis = new FileInputStream(file);
                dos_file.writeUTF(file.getName());
                dos_file.flush();
                dos_file.writeLong(file.length());
                dos_file.flush();
                byte[] bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes, 0, bytes.length)) != -1) {
                    dos_file.write(bytes, 0, length);
                    dos_file.flush();
                }
                fis.close();
                disconnect_file(socket_file);
                synchronized (file_num) {
                    file_num = Integer.valueOf(file_num.intValue() - 1);
                }                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();            
    }

    public void receiveFile(String str, String ip, int port) throws IOException, InterruptedException {
        new Thread(() -> {
            try {
                synchronized (file_num) {
                    file_num = Integer.valueOf(1 + file_num.intValue());
                }
                
                Socket socket_file = new Socket(ip, port + file_num.intValue());
                BufferedReader in_file = new BufferedReader(new InputStreamReader(socket_file.getInputStream()));
                
                speak_file(this.Client_username, socket_file);
                String sPath = str.split(" ")[1];
                //System.out.printf("byrbyr, path:%s\n", sPath);
                //System.out.printf("speak, path:%s\n", "download " + str.split(" ")[0] +" " +sPath);
                speak_file("download" + " " + str.split(" ")[0] +" " +sPath, socket_file);
                boolean isFile = false;
                DataInputStream dis = new DataInputStream(socket_file.getInputStream());
                while (true) {
                    if (isFile) {
                        String fileName = dis.readUTF();
                        //System.out.printf("qnmqnmqnm, path:%s\n", fileName);
                        long fileLen = dis.readLong();
                        File directory = new File(sPath);
                        File file = new File(directory.getAbsolutePath() + File.separatorChar + fileName);
                        FileOutputStream fos = new FileOutputStream(file);
                        byte[] bytes = new byte[1024];
                        int total = 0;
                        int length;
                        while (total < fileLen && (length = dis.read(bytes, 0, bytes.length)) != -1) {
                            total += length;
                            fos.write(bytes, 0, length);
                            fos.flush();
                        }
                        fos.close();
                        isFile = false;
                        break;
                    }
                    //.out.printf("d\n");
                    String strr = in_file.readLine();
                    //System.out.printf("dd\n");
                    if (strr == null) break;
                    //System.out.printf("dddddddd");
                    if ("download".equals(strr)) isFile = true;
                }
                try {
                    disconnect_file(socket_file);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                synchronized (file_num) {
                    file_num = Integer.valueOf(file_num.intValue() - 1);
                }
            } catch (IOException e) {
                if (!(e instanceof SocketException)) e.printStackTrace();
            }
        }).start();  
    }

    public String GetIp(){
        return this.ip;
    }

    public int GetPort(){
        return this.port;
    }

}