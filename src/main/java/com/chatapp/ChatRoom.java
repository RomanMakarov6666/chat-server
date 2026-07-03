package com.chatapp;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatRoom {

    private static final int MAX_HISTORY = 10;

    private final List<DataOutputStream> clients =
            Collections.synchronizedList(new ArrayList<>());

    private final List<String> history = new CopyOnWriteArrayList<>();

    public void addClient(DataOutputStream out) {
        clients.add(out);
    }

    public void removeClient(DataOutputStream out) {
        clients.remove(out);
    }

    public String getHistory() {
        return String.join(",", history);
    }

    public void broadcast(String message) {
        synchronized (clients) {
            for (DataOutputStream out : clients) {
                try {
                    out.writeUTF(message);
                    out.flush();
                } catch (IOException ignored) {
                }
            }
        }

        history.add(message);

        if (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
    }

    public void broadcastImage(String header, byte[] bytes) throws IOException {
        synchronized (clients) {
            for (DataOutputStream out : clients) {
                out.writeUTF(header);
                out.write(bytes);
                out.flush();
            }
        }
    }
}