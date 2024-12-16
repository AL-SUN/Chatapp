package Server;

import java.io.*;
import java.math.RoundingMode;
import java.net.Socket;
import java.net.SocketException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Date;

public class MsgTask implements Runnable {
    private static final String HEARTBEAT = "[usage for heartbeat packet]";
    private final Server server;
    private final String localPath;
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final DataOutputStream dos;
    private final String username;
    private boolean connected;
    private long timestamp; // last time received a message
    private int timeout; // count of timeout

    private final DatabaseConnection database;

    public MsgTask(Server server, String localPath, Socket socket, BufferedReader in, PrintWriter out, String username) throws IOException {
        this.server = server;
        this.localPath = localPath;
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.username = username;
        connected = true;
        timestamp = new Date().getTime();
        timeout = 0;
        dos = new DataOutputStream(socket.getOutputStream());
        database = new DatabaseConnection();
    }

    @Override
    public void run() {
        startHeartbeatMonitor();

        try {
            DataInputStream dataInput = new DataInputStream(socket.getInputStream());

            while (connected) {
                // process message from client
                String message = in.readLine();
                timestamp = new Date().getTime();

                if ("exit".equals(message)) {
                    handleExitCommand();
                }
                else if ("upload".equals(message)) {
                    handleFileUpload(dataInput);
                }
                else if (message.startsWith("Register ")) {
                    handleRegistration(message);
                }
                else if (message.startsWith("Login ")) {
                    handleLogin(message);
                }
                else if (message.startsWith("download")) {
                    handleFileDownload(message);
                }
                else if (HEARTBEAT.equals(message)) {
                    System.out.println(username + " heartbeat");
                    this.timeout = 0;
                }
                else { // normal message
                    server.broadcast(username, message);
                }
            }
        } catch (IOException | InterruptedException e) {
            if (!(e instanceof SocketException)) {
                e.printStackTrace();
            }
        }
    }

    private void startHeartbeatMonitor() {
        final int heartbeatIntervalMillis = 25 * 1000;

        new Thread(() -> {
            while (connected) {
                try {
                    Thread.sleep(heartbeatIntervalMillis);
                    long currentTime = new Date().getTime();
                    if (currentTime - timestamp > heartbeatIntervalMillis) {
                        if (timeout == 2) {
                            disconnect(true);
                        } else {
                            timeout++;
                        }
                    }
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void handleExitCommand() throws IOException {
        boolean isMainServer = socket.getLocalPort() == server.getServerLocalPort();
        disconnect(isMainServer); // notify other users if this is the main server
    }

    private void handleFileUpload(DataInputStream dataInput) throws IOException {
        int bufferSize = 1024;
        String fileBaseName = dataInput.readUTF();
        String fileName = username + "_" + new Date().getTime() + "_" + fileBaseName;
        long fileLength = dataInput.readLong();

        File directory = new File(localPath);  // TODO: TBD where to save the file
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(directory.getAbsolutePath() + File.separatorChar + fileName);
        try (FileOutputStream fileOutput = new FileOutputStream(file)) {
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            long totalRead = 0;

            while (totalRead < fileLength && (bytesRead = dataInput.read(buffer)) != -1) {
                totalRead += bytesRead;
                fileOutput.write(buffer, 0, bytesRead);
            }

            // TODO: broadcast and save name should distinguish between audio and file
            server.broadcast(Server.ADMIN, "Received a file from user: " + username);
            int msgId = server.broadcast(Server.ADMIN, "[File Name: " + fileBaseName + "] [Size: " + getFormatFileSize(fileLength) + "]");
            database.saveFile(msgId, fileName, "File");
        }
    }

    private void handleRegistration(String message) throws IOException {
        String[] credentials = message.substring(9).split(" ");
        String newUsername = credentials[0];
        try {
            database.createUser(newUsername, credentials[1]);
        } catch (SQLException e) {
            if(e.getMessage().contains("Duplicate entry")) {
                out.write("Duplicate\r\n");
            } else {
                out.write("RegFail\r\n");
                e.printStackTrace();
            }
            out.flush();
            return;
        }

        out.write("RegSucc\r\n");
        out.flush();
        server.broadcast(Server.ADMIN, "A new user " + newUsername + " has successfully registered!");
    }

    private void handleLogin(String message) throws IOException {
        String[] credentials = message.substring(6).split(" ");
        boolean isVerified = database.verifyUser(credentials[0], credentials[1]);
        if (isVerified) {
            out.write("Verified\r\n");
        } else {
            out.write("NotVerified\r\n");
        }
        out.flush();
    }

    private void handleFileDownload(String message) throws IOException, InterruptedException {
        final int bufferSize = 1024;

        out.write("download\r\n");
        out.flush();
        Thread.sleep(500);

        String requestedFile = message.split(" ")[1];
        File file = new File(localPath + File.separatorChar + requestedFile);

        dos.writeUTF(file.getName());
        dos.flush();
        dos.writeLong(file.length());
        dos.flush();

        try (FileInputStream fileInput = new FileInputStream(file)) {
            byte[] buffer = new byte[bufferSize];
            int bytesRead;

            while ((bytesRead = fileInput.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
                dos.flush();
            }
        }

        server.broadcast(Server.ADMIN, "User " + username + " has downloaded a file successfully");
        server.broadcast(Server.ADMIN, "[File Name: " + file.getName() + "] [Size: " + getFormatFileSize(file.length()) + "]");
    }

    /**
     * Disconnect the client
     * @param notifyUser whether to notify other users（true: user, false: files）
     * @throws IOException if an I/O error occurs
     */
    private void disconnect(boolean notifyUser) throws IOException {
        synchronized (Server.sockets) {
            Server.sockets.remove(server.getAddress(socket));
        }
        socket.shutdownInput();
        socket.shutdownOutput();
        socket.close();

        if (notifyUser && !username.equals("Guest")) {
            server.broadcast(Server.ADMIN, "User " + username + " has left the chatroom.");
        }

        ChatRoom.printStatus();
        connected = false;
    }

    private String getFormatFileSize(long length) {
        DecimalFormat df = new DecimalFormat("#0.0");
        df.setRoundingMode(RoundingMode.HALF_UP);
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(1);
        double size = ((double) length) / (1 << 30);
        if (size >= 1) {
            return df.format(size) + "GB";
        }
        size = ((double) length) / (1 << 20);
        if (size >= 1) {
            return df.format(size) + "MB";
        }
        size = ((double) length) / (1 << 10);
        if (size >= 1) {
            return df.format(size) + "KB";
        }
        return length + "B";
    }

}