# Java Socket Chat App

A lightweight real-time chat application built with **Java Sockets (TCP)** and **Swing**, supporting multi-client messaging and image sharing.

---

## 🚀 Features

- Real-time messaging using TCP sockets
- Multi-client server with thread pool concurrency
- Broadcast system with last 10 message history
- Image sharing via byte-stream transfer
- Swing-based desktop GUI
- Basic reconnect support

---

## 🧠 Technical Highlights

- Custom TCP socket communication
- Concurrent client handling using `ExecutorService`
- Stream-based I/O (`DataInputStream` / `DataOutputStream`)
- Event-driven UI with Java Swing
- In-memory chat history

---

## 🛠️ Tech Stack

- Java 21
- Java Sockets (TCP)
- Java Swing
- Maven

---

## ▶️ Run

```bash
java com.chatapp.Server
java com.chatapp.Client
```
