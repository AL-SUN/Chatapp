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
    private Map<Integer, File> fileDownloadLinks = new HashMap<>();
    private int linkCounter = 0;


    public ChatroomUI(String username, String ip, int port) throws Exception {
        this.client = new Client(username, ip, port, this);

        Properties properties = loadProperties();
        AUDIO_PATH = properties.getProperty("client.audio");

        // Set basic window properties
        setTitle("Chatroom Client");
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
                    for (Map.Entry<Integer, File> entry : fileDownloadLinks.entrySet()) {
                        Style linkStyle = chatPane.getStyle("link-" + entry.getKey());
                        if (attrs.containsAttributes(linkStyle)) {
                            downloadFile(entry.getValue());
                            return;  // 找到并下载后立即返回，避免多次触发
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
        appendToChat(sender, parts[1], Color.BLACK, sender.equals("System"));
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
//            appendToChat("System", "File sent: " + selectedFile.getName(), Color.GREEN, true);

            System.out.println("Sending file: " + selectedFile.getName());
            try {
                client.sendFile(selectedFile, client.GetIp(), client.GetPort());
            } catch (Exception e ) {
                System.err.println("Failed to send file: " + e.getMessage());
            }
            // 添加文件下载按钮，并传递唯一标识
//            appendFileDownloadLink(selectedFile);
        }
    }

    private void appendFileDownloadLink(File file) {
        try {
            // 为每个文件链接创建唯一标识
            int linkId = linkCounter++;

            // Create a clickable hyperlink-like style for file download
            Style linkStyle = chatPane.addStyle("link-" + linkId, null);
            StyleConstants.setForeground(linkStyle, Color.BLUE);
            StyleConstants.setUnderline(linkStyle, true);

            // 将文件存储在映射中，使用唯一标识作为键
            fileDownloadLinks.put(linkId, file);

            // Insert download link with unique identifier
            chatDocument.insertString(chatDocument.getLength(),
                    "Download: " + file.getName() + " [Link-" + linkId + "]\n",
                    linkStyle
            );

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }


    private void downloadFile(File sourceFile) {
        JFileChooser saveChooser = new JFileChooser();
        saveChooser.setSelectedFile(sourceFile);
        int result = saveChooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File destinationFile = saveChooser.getSelectedFile();
            try {
                // 实际文件复制逻辑
                java.nio.file.Files.copy(
                        sourceFile.toPath(),
                        destinationFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
                JOptionPane.showMessageDialog(this,
                        "File downloaded successfully: " + destinationFile.getName(),
                        "Download Complete",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Download failed: " + e.getMessage(),
                        "Download Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
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
                client.sendFile(new File(wavPath), client.GetIp(), client.GetPort());
                System.out.printf("Record the AUDIO, path:%s\n", wavPath);
            }

            /*// 语音录制模拟
            File tempVoiceFile = File.createTempFile("voice_message", ".wav");
            appendToChat("System", "Voice message recorded", Color.GREEN, true);
            appendVoicePlaybackLink(tempVoiceFile);*/
        } catch (Exception e) {
            e.printStackTrace();
            }
    }

    private void appendVoicePlaybackLink(File voiceFile) {
        try {
            // Create a clickable hyperlink-like style for voice playback
            Style linkStyle = chatPane.addStyle("link", null);
            StyleConstants.setForeground(linkStyle, Color.GREEN);
            StyleConstants.setUnderline(linkStyle, true);

            // Insert playback link
            chatDocument.insertString(chatDocument.getLength(),
                    "Play Voice Message\n",
                    linkStyle
            );

            // Add mouse listener to handle voice playback
            chatPane.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int offset = chatPane.viewToModel(e.getPoint());
                    try {
                        if (chatDocument.getCharacterElement(offset).getAttributes().containsAttributes(linkStyle)) {
                            playVoiceMessage(voiceFile);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void playVoiceMessage(File voiceFile) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(voiceFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.start();
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