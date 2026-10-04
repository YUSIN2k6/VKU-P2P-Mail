package com.vku.app;

import java.net.*;
import java.nio.charset.StandardCharsets;

public class UDPConnection {
    private DatagramSocket socket;

    public UDPConnection(int port) throws SocketException {
        this.socket = new DatagramSocket(port);
    }

    public UDPConnection() throws SocketException {
        this.socket = new DatagramSocket();
    }

    public void send(String data, InetAddress address, int port) throws Exception {
        byte[] buffer = data.getBytes(StandardCharsets.UTF_8); // Ép kiểu ở đây
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        socket.send(packet);
    }

    public DatagramPacket receive() throws Exception {
        // Tăng buffer lên 64KB để nhận được file nhỏ
        byte[] buffer = new byte[64000];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return packet;
    }

    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}