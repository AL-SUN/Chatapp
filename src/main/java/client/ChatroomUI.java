package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.sound.sampled.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Utils.ResourceLoader.loadProperties;

public class ChatroomUI extends JFrame {
    private final JTextPane chatPane;
    private final JTextArea messageArea;
    private JButton sendButton;
    private JButton fileButton;
    private JButton voiceButton;
    private JButton leaveButton;
    private JLabel chatroomNameLabel;
    private final StyledDocument chatDocument;
    private final SimpleDateFormat timestampFormat;
    private boolean isRecording = false;

    private final Client client;

    private static String AUDIO_PATH;
    private static final String PCM = "audio.pcm";
    private static final String WAV = "audio.wav";

    // Map to store file download links
    private final Map<Integer, String> fileDownloadLinks = new HashMap<>();
    private int linkCounter = 0;


    public ChatroomUI(String username, String ip, int port) throws Exception {
        this.client = new Client(username, ip, port, this);

        Properties properties = loadProperties();
        AUDIO_PATH = properties.getProperty("client.audio");

        // Set basic window properties
        setTitle("Chatroom Client: "+ username);
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize timestamp formatter
        timestampFormat = new SimpleDateFormat("MM-dd HH:mm:ss");

        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(240, 240, 240));

        // Top panel: Chatroom name and Leave button
        JPanel topPanel = new JPanel(new BorderLayout());
        chatroomNameLabel = new JLabel("Chatroom: " + client.chatRoomName, SwingConstants.CENTER);
        chatroomNameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        leaveButton = new JButton("Leave");
        leaveButton.setBackground(new Color(255, 100, 100));
        leaveButton.setForeground(Color.WHITE);

        topPanel.add(chatroomNameLabel, BorderLayout.CENTER);
        topPanel.add(leaveButton, BorderLayout.EAST);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Chat display area using JTextPane
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setBackground(Color.WHITE);
        chatDocument = chatPane.getStyledDocument();
        JScrollPane scrollPane = new JScrollPane(chatPane);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Bottom panel: Message input and buttons
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));

        // Multi-line message input area
        messageArea = new JTextArea();
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane messageScrollPane = new JScrollPane(messageArea);
        messageScrollPane.setPreferredSize(new Dimension(400, 60));

        sendButton = new JButton("Send");
        fileButton = new JButton("📁");
        voiceButton = new JButton("🎤");

        // Style settings
        sendButton.setBackground(new Color(100, 200, 100));
        sendButton.setForeground(Color.WHITE);
        fileButton.setBackground(Color.LIGHT_GRAY);
        voiceButton.setBackground(Color.LIGHT_GRAY);

        // Button event listeners
        sendButton.addActionListener(e -> sendMessage());
        fileButton.addActionListener(e -> selectAndSendFile());
        voiceButton.addActionListener(e -> recordAndSendVoice());
        leaveButton.addActionListener(e -> leaveChatroom());

        // Add key binding for sending message with Ctrl+Enter
        messageArea.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK),
                "send-message"
        );
        messageArea.getActionMap().put("send-message", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        // Bottom button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(fileButton);
        buttonPanel.add(voiceButton);

        bottomPanel.add(messageScrollPane, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        bottomPanel.add(buttonPanel, BorderLayout.WEST);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Add main panel
        add(mainPanel);

        //TODO: Add listeners for file download links
        chatPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int offset = chatPane.viewToModel(e.getPoint());
                try {
                    Element element = chatDocument.getCharacterElement(offset);
                    AttributeSet attrs = element.getAttributes();

                    // 遍历所有链接样式
                    for (Map.Entry<Integer, String> entry : fileDownloadLinks.entrySet()) {

                        Style downStyle = chatPane.getStyle("downlink-" + entry.getKey());
                        Style voiceStyle = chatPane.getStyle("voicelink-" + entry.getKey());

                        if (downStyle != null && attrs.containsAttributes(downStyle)) {
                            downloadFile(entry.getValue());
                            return;
                        } else if (voiceStyle != null && attrs.containsAttributes(voiceStyle)) {
                            playVoiceMessage(entry.getValue());
                            return;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void sendMessage() {
        String message = messageArea.getText().trim();
        if (!message.isEmpty()) {
            client.speak(message);
            messageArea.setText("");
        }
    }

    public void showMsg(String msg){
        //TODO: Update the chat display with the received message
        String[] parts = msg.split(": ", 2);
        String sender = parts[0];
        String message = parts[1];

        Pattern pattern = Pattern.compile("\\[ID: (.*?)\\]");
        Matcher matcher = pattern.matcher(message);

        if(message.startsWith("Sent a voice message:") && matcher.find()){
            String attachmentId = matcher.group(1);
            String trimmedMessage = message.substring(0, matcher.start()).trim();
            appendToChat(sender, trimmedMessage, Color.BLACK, sender.equals("System"));
            appendVoicePlaybackLink(attachmentId); // playback link
        }
        else if(message.startsWith("File sent:")  && matcher.find()){
            String attachmentId = matcher.group(1);
            String trimmedMessage = message.substring(0, matcher.start()).trim();
            appendToChat(sender, trimmedMessage, Color.BLACK, sender.equals("System"));
            appendFileDownloadLink(trimmedMessage.substring(11),attachmentId); // download link
        } else {
            appendToChat(sender, message, Color.BLACK, sender.equals("System"));
        }

    }

    private void appendToChat(String sender, String message, Color color, boolean isSystemMessage) {
        try {
            // Create styles for different types of messages
            Style defaultStyle = chatPane.addStyle("default", null);
            StyleConstants.setFontFamily(defaultStyle, "Arial");
            StyleConstants.setFontSize(defaultStyle, 14);

            Style timestampStyle = chatPane.addStyle("timestamp", defaultStyle);
            StyleConstants.setForeground(timestampStyle, Color.GRAY);
            StyleConstants.setFontSize(timestampStyle, 12);

            Style senderStyle = chatPane.addStyle("sender", defaultStyle);
            StyleConstants.setBold(senderStyle, true);
            StyleConstants.setForeground(senderStyle, isSystemMessage ? Color.RED : Color.BLUE);

            Style messageStyle = chatPane.addStyle("message", defaultStyle);
            StyleConstants.setForeground(messageStyle, color);

            // Get current timestamp
            String timestamp = timestampFormat.format(new Date());

            // Append timestamp
            chatDocument.insertString(chatDocument.getLength(),
                    "[" + timestamp + "] ",
                    timestampStyle
            );

            // Append sender and message
            chatDocument.insertString(chatDocument.getLength(),
                    sender + ": ",
                    senderStyle
            );
            chatDocument.insertString(chatDocument.getLength(),
                    message + "\n",
                    messageStyle
            );

            // Automatically scroll to the bottom
            chatPane.setCaretPosition(chatDocument.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void selectAndSendFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                System.out.println("Sending file: " + selectedFile.getName());
                client.sendFile(selectedFile, client.GetIp(), client.GetPort(), false);
            } catch (Exception e ) {
                System.err.println("Failed to send file: " + e.getMessage());
            }
        }
    }

    private void appendFileDownloadLink(String filename, String AttachmentId) {
        try {
            int linkId = linkCounter++;

            // Create a clickable hyperlink-like style for file download
            Style linkStyle = chatPane.addStyle("downlink-" + linkId, null);
            StyleConstants.setForeground(linkStyle, Color.BLUE);
            StyleConstants.setUnderline(linkStyle, true);

            fileDownloadLinks.put(linkId, filename + "/" + AttachmentId);

            // Insert download link with unique identifier
            chatDocument.insertString(chatDocument.getLength(),
                    "Download: " + filename + " [downlink-" + linkId + "]\n",
                    linkStyle
            );

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }


    private void downloadFile(String filename_id) throws IOException, InterruptedException {
        String[] parts = filename_id.split("/", 2);
        String AttachmentId = parts[1];

        JFileChooser saveChooser = new JFileChooser();

        // get the file name
        Pattern pattern = Pattern.compile("\\[File Name: (.*?)\\]");
        Matcher matcher = pattern.matcher(parts[0]);
        if (matcher.find()) {
            String filename = matcher.group(1);
            saveChooser.setSelectedFile(new File(filename));
        }

        int result = saveChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File destinationFile = saveChooser.getSelectedFile();
            //TODO: Download the file and save it to the selected location

            client.receiveFile(AttachmentId, client.GetIp(), client.GetPort(), destinationFile.getAbsolutePath());

            JOptionPane.showMessageDialog(this,
                    "File downloaded successfully: " + destinationFile.getName(),
                    "Download Complete",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    private void recordAndSendVoice() {
        try {
            if (!isRecording) {
                isRecording = true;
                voiceButton.setBackground(new Color(255, 100, 100));
                new Thread(() -> {
                    try {
                        AudioRecord.save(AUDIO_PATH + File.separatorChar +  PCM);
                    } catch (IOException | LineUnavailableException ioException) {
                        ioException.printStackTrace();
                    }
                }).start();
            } else {
                isRecording = false;
                voiceButton.setBackground(Color.LIGHT_GRAY);

                AudioRecord.running = false;
                String pcmPath = AUDIO_PATH + File.separatorChar + PCM;
                String wavPath = AUDIO_PATH + File.separatorChar + WAV;

                Pcm2Wav.convertAudioFiles(new String[]{pcmPath, wavPath});
                client.sendFile(new File(wavPath), client.GetIp(), client.GetPort(), true);
                System.out.printf("Record the AUDIO, path:%s\n", wavPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void appendVoicePlaybackLink(String AttachmentId) {
        try {
            int linkId = linkCounter++;

            // Create a clickable hyperlink-like style for voice playback
            Style linkStyle = chatPane.addStyle("voicelink-" + linkId, null);
            StyleConstants.setForeground(linkStyle, Color.MAGENTA);
            StyleConstants.setUnderline(linkStyle, true);

            fileDownloadLinks.put(linkId, AttachmentId);

            chatDocument.insertString(chatDocument.getLength(),
                    "Play Voice Message\n",
                    linkStyle
            );
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void playVoiceMessage(String AttachmentId) {
        try {
            String path = AUDIO_PATH + File.separatorChar + WAV;
            client.receiveFile(AttachmentId, client.GetIp(), client.GetPort(), path);
            AudioPlayer.playAndDelete(path);

//            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(voiceFile);
//            Clip clip = AudioSystem.getClip();
//            clip.open(audioInputStream);
//            clip.start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Unable to play voice message: " + e.getMessage(),
                    "Playback Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void leaveChatroom() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to leave the chatroom?",
                "Leave Confirmation",
                JOptionPane.YES_NO_OPTION
        );
        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            try {
                client.disconnect();
            } catch (Exception e) {
                System.err.println("Failed to disconnect client: " + e.getMessage());
            }
        }
    }
}