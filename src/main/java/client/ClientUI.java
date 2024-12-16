package client;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import static Utils.ResourceLoader.loadProperties;

public class ClientUI implements ActionListener {
    private static String AUDIO_PATH;
    private static final String PCM = "audio.pcm";
    private static final String WAV = "audio.wav";

    private Client client;
    private String username;
    private static final int MAX_MSG = 1024;
    private String[] msgList;
    private int index;

    private JFrame jFrame;
    private JPanel jContentPane;
    private JList<String> jMsgList;
    private JScrollPane msgListPane;
    private JScrollPane msgTFPane;
    private JTextArea msgTF;
    private JButton sendBtn;
    private JButton audioBtn;
    private JButton downloadAudioBtn;
    private JButton leaveBtn;

    ChatroomUI chatroomUI;

    //login verification: -1 default or timeout; 0 username or password error; 1 login successful
    private int verify = -1;

    public void setVerify(int val) {
        verify = val;
    }

    public ClientUI() {
        Properties properties = loadProperties();
        AUDIO_PATH = properties.getProperty("client.audio");  //TODO: Change audio path in config.properties
    }

    public void createClient(String username, String ip, int port, boolean isClient) {
        try {
            client = new Client(username, ip, port, new ChatroomUI(username, ip, port));
            this.username = username;
            msgList = new String[MAX_MSG];
            index = 0;
            if(isClient){ // no UI if temporary call in login
                getJFrame().setVisible(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int GuestSpeakAndVerify(String str) {
        client.speak(str);
        try {
            Thread.sleep(500);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return verify;
    }

    public void shutDownClient() {
        try {
            client.disconnect();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void showMsg(String msg) {
        if(jMsgList == null){
            return;
        }
        this.msgList[index++] = msg;
        jMsgList.setListData(msgList);
    }

    private JFrame getJFrame() {
        jFrame = new JFrame();
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setResizable(false);
        jFrame.setLocation(new Point(500, 265));

        jFrame.setJMenuBar(new JMenuBar());
        jFrame.setSize(350, 410);
        jFrame.setContentPane(getJContentPane());
        jFrame.setTitle("ChatRoom_Client");

        return jFrame;
    }

    private JPanel getJContentPane() {
        if (jContentPane == null) {
            JLabel chatRoomLabel = new JLabel();
            chatRoomLabel.setBounds(new Rectangle(20, 15, 200, 25));
            chatRoomLabel.setText("ChatRoom: " + client.chatRoomName);

            JLabel userLabel = new JLabel();
            userLabel.setBounds(new Rectangle(20, 290, 100, 25));
            userLabel.setText("Text here" + ": ");

            jContentPane = new JPanel();
            jContentPane.setLayout(null);
            jContentPane.setBackground(Color.LIGHT_GRAY);

            jContentPane.add(chatRoomLabel, null);
            jContentPane.add(userLabel, null);
            jContentPane.add(getMsgListPane(), null);
            jContentPane.add(getMsgTF(), null);
            jContentPane.add(getSendBtn(), null);
            jContentPane.add(getAudioBtn(), null);
            jContentPane.add(getDownloadAudioBtn(), null);
            jContentPane.add(getLeaveBtn(), null);
        }
        return jContentPane;
    }

    private JScrollPane getMsgListPane() {
        if (msgListPane == null) {
            jMsgList = new JList<>(msgList);
            msgListPane = new JScrollPane(jMsgList);
            msgListPane.setBounds(new Rectangle(20, 90, 290, 195));
        }
        return msgListPane;
    }

    private JTextArea getMsgTF() {
        if (msgTF == null) {
            msgTF = new JTextArea();
            msgTF.setBounds(new Rectangle(20, 315, 222, 40));
            msgTF.setLineWrap(true);
            msgTFPane = new JScrollPane(msgTF, 22, 32);
            //msgTFPane.setBounds(new Rectangle(20, 315, 222, 30));
        }
        return msgTF;
    }

    private JButton getSendBtn() {
        if (sendBtn == null) {
            sendBtn = new JButton();
            sendBtn.setBounds(new Rectangle(255, 315, 65, 30));
            sendBtn.addActionListener(this);
            sendBtn.setText("Send");
        }
        return sendBtn;
    }

    private JButton getAudioBtn() {
        if (audioBtn == null) {
            audioBtn = new JButton();
            audioBtn.setBounds(new Rectangle(20, 50, 85, 30));
            audioBtn.addActionListener(this);
            audioBtn.setText("Record");
        }
        return audioBtn;
    }

    private JButton getDownloadAudioBtn() {
        if (downloadAudioBtn == null) {
            downloadAudioBtn = new JButton();
            downloadAudioBtn.setBounds(new Rectangle(115, 50, 85, 30));
            downloadAudioBtn.addActionListener(this);
            downloadAudioBtn.setText("Play");
        }
        return downloadAudioBtn;
    }

    private JButton getLeaveBtn() {
        if (leaveBtn == null) {
            leaveBtn = new JButton();
            leaveBtn.setBounds(new Rectangle(245, 10, 75, 30));
            leaveBtn.addActionListener(this);
            leaveBtn.setText("Leave");
        }
        return leaveBtn;
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            String command = e.getActionCommand();
            switch (command) {
                case "Send":
                    handleSendCommand();
                    break;
                case "Record":
                    handleRecordCommand();
                    break;
                case "Stop":
                    handleStopCommand();
                    break;
                case "Play":
                    handlePlayCommand();
                    break;
                default:
                    jFrame.dispose();
                    shutDownClient();
                    break;
            }
        } catch (IOException | InterruptedException exception) {
            exception.printStackTrace();
        }
    }

    private void handleSendCommand() throws IOException, InterruptedException {
        String message = msgTF.getText().trim();
        if (message.isEmpty()) {
            showDialog("Content cannot be empty.");
            return;
        }

        if (message.startsWith("upload")) {
            String[] parts = message.split(" ");
            if (parts.length < 2) {
                showDialog("Invalid upload command.");
                return;
            }
//            client.sendFile(parts[1], client.GetIp(), client.GetPort());
        } else if (message.startsWith("download")) {
            String filename = message.substring(9);
            client.receiveFile(filename, client.GetIp(), client.GetPort());
        } else {
            client.speak(message);
        }
        msgTF.setText(""); // clear the text area
    }

    private void handleRecordCommand(){
        audioBtn.setText("Stop");
        new Thread(() -> {
            try {
                AudioRecord.save(AUDIO_PATH + File.separatorChar +  PCM);
            } catch (IOException | LineUnavailableException ioException) {
                ioException.printStackTrace();
            }
        }).start();
    }

    private void handleStopCommand() throws IOException, InterruptedException {
        AudioRecord.running = false;
        String pcmPath = AUDIO_PATH + File.separatorChar + PCM;
        String wavPath = AUDIO_PATH + File.separatorChar + WAV;

        Pcm2Wav.convertAudioFiles(new String[]{pcmPath, wavPath});
//        client.sendFile(wavPath, client.GetIp(), client.GetPort());
        System.out.printf("Record the AUDIO, path:%s\n", wavPath);

        audioBtn.setText("Record");

        //File pcmFile = new File(pcmPath);
        //File wavFile = new File(wavPath);
        //pcmFile.delete();
        //wavFile.delete();
    }

    private void handlePlayCommand() throws InterruptedException, IOException {
        for (int i = index - 1; i >= 0; i--) {
            if (msgList[i].contains("audio.wav")) {
                String audioName = msgList[i].substring(20, msgList[i].lastIndexOf("] [Size: "));
                System.out.printf("Play the AUDIO, path:%s\n", audioName + " " + AUDIO_PATH);
                client.receiveFile(audioName + " " + AUDIO_PATH, client.GetIp(), client.GetPort());
                Thread.sleep(1500);
                AudioPlayer.playAndDelete(AUDIO_PATH + File.separatorChar + audioName);
                break;
            }
        }
    }

    public void showDialog(String msg) {
        JOptionPane.showMessageDialog(jFrame, msg);
    }
}