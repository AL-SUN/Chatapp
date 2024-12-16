package client;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyStore;
import java.security.KeyStoreException;

public class AuthClient {
    private QQLoginUI UI;
    private int verify;
    private SSLSocket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final String ip;
    private final int port;
    private boolean running = true;
    SSLSocketFactory socketFactory;


    public AuthClient(String ip, int port, QQLoginUI UI) {
        this.ip = ip;
        this.port = port;
        this.UI = UI;
        this.verify = -1; // -1 default or timeout
    }

    public void init() throws Exception{
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(new FileInputStream("client.truststore"), "123456".toCharArray());

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

        socketFactory = sslContext.getSocketFactory();
        socket = (SSLSocket) socketFactory.createSocket(ip, port);
    }

    public void connect() throws IOException {
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!socket.getTcpNoDelay()) socket.setTcpNoDelay(true);
        if (!socket.getKeepAlive()) socket.setKeepAlive(true);
//        if (!socket.getOOBInline()) socket.setOOBInline(true);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "GBK"));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "GBK"));

        speak("Guest");

        new Thread(() -> {
            try {
                while (running) {
                    String str = in.readLine();
                    if (str == null) break;

                    synchronized (this) {
                        switch (str) {
                            case "NotVerified":
                                verify = 0;
                                UI.showDialog("Please check your username and password.");
                                break;
                            case "Verified":
                                verify = 1;
                                break;
                            case "Duplicate":
                                verify = -2;
                                UI.showDialog("Username has been used!");
                                break;
                            case "RegFail":
                                verify = -3;
                                UI.showDialog("Registration failed!");
                                break;
                            case "RegSucc":
                                verify = 3;
                                UI.showDialog("Registration successful!");
                                break;
                        }
                        // Notify the main thread that a server response has been received
                        this.notifyAll();
                    }
                }
            } catch (IOException e) {
                if (!(e instanceof SocketException)) e.printStackTrace();
            } finally {
                closeResources();
            }
        }).start();
    }

    public void speak(String str) {
        out.write(str + "\r\n");
        out.flush();
    }

    public void disconnect() {
        try {
            speak("exit");
            synchronized (this) {
                running = false;
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int GuestSpeakAndVerify(String str) {
        try {
            connect(); // Connect to server and listen for verification
            speak(str);

            synchronized (this) {
                // Wait for server to update the verify value
                while (verify == -1) {
                    this.wait(); // Wait until `verify` is updated by the listener thread
                }
            }
            disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return verify;
    }

    private void closeResources() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}