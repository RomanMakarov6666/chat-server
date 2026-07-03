package com.chatapp;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final int PORT = 5000;
    private static final int THREADS = 10;

    public static void main(String[] args) throws IOException {

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        ChatRoom room = new ChatRoom();

        try (ServerSocket server = new ServerSocket(PORT)) {

            System.out.println("Server started");

            while (true) {

                Socket socket = server.accept();

                DataOutputStream out =
                        new DataOutputStream(socket.getOutputStream());

                out.writeUTF(room.getHistory());

                room.addClient(out);

                pool.execute(new ClientHandler(socket, out, room));
            }
        }
    }
}