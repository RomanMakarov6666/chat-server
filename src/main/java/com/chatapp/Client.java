package com.chatapp;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;

public class Client {
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private Socket socket;
    private JFrame frame;
    private JPanel chatPanel;
    private JTextField inputField;
    private JButton sendButton;
    private String name;
    private String ip;
    private JLabel statusLabel;
    private JButton imageButton;
    private volatile boolean connected = false;

    public Client() {
        name = JOptionPane.showInputDialog("Enter your name:");
        ip = JOptionPane.showInputDialog("Server address:");
        if (name == null || name.trim().isEmpty()) {
            name = "Anonymous";
        }
        String ipPattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        if (ip == null || !ip.matches(ipPattern) || ip.startsWith("127.") || ip.startsWith("169.254.") || ip.trim().isEmpty()) {
            ip = "127.0.0.1";
        }

        setupGUI();
        connectToServer();
    }

    private void setupGUI() {
        frame = new JFrame("Chat Client - " + name);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);

        // Handle window close properly
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                disconnect();
            }
        });
        URL iconURL = getClass().getResource("/images/instagram.png");
        if(iconURL!=null) {
            ImageIcon imageIcon = new ImageIcon(iconURL);
            frame.setIconImage(imageIcon.getImage());
        }

        frame.setLayout(new BorderLayout());

        // TOP: Status
        statusLabel = new JLabel(" Status: Connecting...");
        frame.add(statusLabel, BorderLayout.NORTH);

        // CENTER: Chat display
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(chatPanel);
        frame.add(scrollPane, BorderLayout.CENTER);

        // SOUTH: Input panel
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Send");
        sendButton.setEnabled(false); // Disabled until connected

        inputField.addActionListener(e -> sendMessage());
        sendButton.addActionListener(e -> sendMessage());

        imageButton = new JButton("Send Image");
        imageButton.setEnabled(false);
        imageButton.addActionListener(e -> {
            try {
                sendImage();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.add(imageButton, BorderLayout.WEST);

        frame.add(inputPanel, BorderLayout.SOUTH);

        // Connect button
        JButton connectButton = new JButton("Reconnect");
        connectButton.addActionListener(e -> reconnect());
        frame.add(connectButton, BorderLayout.WEST);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void connectToServer() {
        new Thread(() -> {
            try {

                socket = new Socket(ip, 5000);
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                connected = true;


                // Update UI
                statusLabel.setText("Status: Connected");
                SwingUtilities.invokeLater(() -> {
                    sendButton.setEnabled(true);
                    inputField.setEnabled(true);
                    imageButton.setEnabled(true);
                    inputField.requestFocus();
                });
//                addMessageToPanel("[Connected to server]\n");
                String[] messages = dataInputStream.readUTF().split(",");
                for(String msg : messages){
                    addMessageToPanel(msg);
                }
                // Send join message
                dataOutputStream.writeUTF(name + " has joined the chat");

                // Start receiving messages
                new Thread(this::receiveMessages).start();

            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                    sendButton.setEnabled(false));
                addMessageToPanel("[Connection failed: " + e.getMessage() + "]\nMake sure server is running on port 5000\n");
                connected = false;
            }
        }).start();
    }

    private void receiveMessages() {
        try {
            while (connected) {
                String message = dataInputStream.readUTF();
                if(message.startsWith("IMAGE:")) {
                    String[] parts = message.split(":", 3);
                    String sender = parts[1];
                    int len = Integer.parseInt(parts[2]);

                    byte[] bytes = new byte[len];
                    dataInputStream.readFully(bytes);
                    ImageIcon icon = new ImageIcon(bytes);
                    int maxWidth = 300;

                    Image img = icon.getImage();
                    int width = img.getWidth(null);
                    int height = img.getHeight(null);

                    int newHeight = (height * maxWidth) / width;

                    Image scaled = img.getScaledInstance(maxWidth, newHeight, Image.SCALE_SMOOTH);
                    addImageToPanel(new ImageIcon(scaled));
                }else addMessageToPanel(message);
            }
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                if (connected) {
                    addMessageToPanel("[Disconnected from server: " + e.getMessage() + "]\n");
                }
            });
            connected = false;
            SwingUtilities.invokeLater(() -> {
                sendButton.setEnabled(false);
            });
        }
    }

    private void sendMessage() {
        if (!connected) {
            chatPanel.add(new JLabel("[Not connected to server]\n"));
            return;
        }

        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            try {
                dataOutputStream.writeUTF(name + ": " + message);
                inputField.setText("");

                if (message.equalsIgnoreCase("exit")) {
                    disconnect();
                }
            } catch (Exception e) {
                chatPanel.add(new JLabel("[Failed to send message]\n"));
                connected = false;
                SwingUtilities.invokeLater(() -> {
                    sendButton.setEnabled(false);
                });
            }
        }
        inputField.requestFocus();
    }
    private void sendImage() throws IOException {
        JFileChooser fileChooser = new JFileChooser();
        if(fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION){
            File file = fileChooser.getSelectedFile();
            byte[] bytes = Files.readAllBytes(file.toPath());
            String base64Image = Base64.getEncoder().encodeToString(bytes);

            dataOutputStream.writeUTF("IMAGE:" + name + ":" + bytes.length);
            dataOutputStream.write(bytes);
        }
    }

    private void disconnect() {
        connected = false;
        try {
            if (dataOutputStream != null) {
                dataOutputStream.writeUTF(name + " has left the chat");
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            // Ignore
        }
        chatPanel.add(new JLabel("[Disconnected]\n"));
        sendButton.setEnabled(false);
    }

    private void reconnect() {
        if (!connected) {
            chatPanel.add(new JLabel("[Reconnecting...]\n"));
            connectToServer();
        }
    }
    private void addMessageToPanel(String message) {
        SwingUtilities.invokeLater(() -> {
            JLabel text = new JLabel(message);
            chatPanel.add(text);
            chatPanel.revalidate();
            chatPanel.repaint();
            scrollToBottom();
        });
    }

    private void addImageToPanel(ImageIcon image) {
        SwingUtilities.invokeLater(() -> {
            JLabel imageLabel = new JLabel(image);
            chatPanel.add(imageLabel);
            chatPanel.revalidate();
            chatPanel.repaint();
            scrollToBottom();
        });
    }

    private void scrollToBottom() {
        JScrollBar vertical = ((JScrollPane)frame.getContentPane().getComponent(1)).getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}