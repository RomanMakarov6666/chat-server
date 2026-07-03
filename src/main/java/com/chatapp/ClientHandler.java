package com.chatapp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final DataOutputStream out;
    private final ChatRoom room;

    public ClientHandler(Socket socket, DataOutputStream out, ChatRoom room) {
        this.socket = socket;
        this.out = out;
        this.room = room;
    }

    @Override
    public void run() {

        String clientId = "Client-" + Thread.currentThread().getId();

        try (DataInputStream in =
                     new DataInputStream(socket.getInputStream())) {

            out.writeUTF("Welcome! You are " + clientId);

            while (true) {

                String message = in.readUTF();

                if (message.startsWith("IMAGE:")) {

                    String[] parts = message.split(":", 3);

                    int len = Integer.parseInt(parts[2]);

                    byte[] bytes = new byte[len];

                    in.readFully(bytes);

                    room.broadcastImage(message, bytes);

                } else {

                    System.out.println(message);

                    room.broadcast(message);

                    if (message.equalsIgnoreCase("exit()")) {
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.out.println(clientId + ": " + e.getMessage());

        } finally {

            room.removeClient(out);

            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }
}