package com.vku.app;

import java.net.*;
import java.nio.charset.StandardCharsets;

public class UDPConnection {
    private DatagramSocket socket;

    public UDPConnection(int port) throws SocketException {
        this.socket = new DatagramSocket(port);
        // Tăng bộ đệm nhận/gửi lên 1MB để hứng file bị băm mảnh
        this.socket.setReceiveBufferSize(1024 * 1024);
        this.socket.setSendBufferSize(1024 * 1024);
    }

    public UDPConnection() throws SocketException {
        this.socket = new DatagramSocket();
        // Tăng bộ đệm nhận/gửi lên 1MB
        this.socket.setReceiveBufferSize(1024 * 1024);
        this.socket.setSendBufferSize(1024 * 1024);
    }

    public void send(String data, InetAddress address, int port) throws Exception {
        byte[] buffer = data.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        socket.send(packet);
    }

    public DatagramPacket receive() throws Exception {
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