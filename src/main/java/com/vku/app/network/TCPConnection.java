package com.vku.app.network;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TCPConnection implements AutoCloseable {
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private final Object writeLock = new Object();
    private final Object readLock = new Object();

    public TCPConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    public TCPConnection(String host, int port) throws IOException {
        this(new Socket(host, port));
    }

    public void sendText(String text) throws IOException {
        synchronized (writeLock) {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        }
    }

    public String receiveText() throws IOException {
        synchronized (readLock) {
            int length = dis.readInt();
            byte[] bytes = new byte[length];
            dis.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    public void sendFile(File file) throws IOException {
        synchronized (writeLock) {
            dos.writeUTF(file.getName());
            dos.writeLong(file.length());

            try (FileInputStream fis = new FileInputStream(file);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
            }
            dos.flush();
        }
    }

    public File receiveFile(File destinationDir) throws IOException {
        synchronized (readLock) {
            if (!destinationDir.exists()) {
                destinationDir.mkdirs();
            }
            String fileName = dis.readUTF();
            long fileSize = dis.readLong();

            File outputFile = new File(destinationDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(outputFile);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                byte[] buffer = new byte[8192];
                long totalRead = 0;
                while (totalRead < fileSize) {
                    int bytesToRead = (int) Math.min(buffer.length, fileSize - totalRead);
                    int bytesRead = dis.read(buffer, 0, bytesToRead);
                    if (bytesRead == -1) {
                        throw new EOFException("Mất kết nối đột ngột khi đang tải file: " + fileName);
                    }
                    bos.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                }
                bos.flush();
            }
            return outputFile;
        }
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }

    @Override
    public void close() {
        try {
            if (dos != null) dos.close();
        } catch (Exception ignored) {}
        try {
            if (dis != null) dis.close();
        } catch (Exception ignored) {}
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception ignored) {}
    }
}
