package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private ServerSocket serverSocket;

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void startServer() throws IOException {
        try{
            while (!serverSocket.isClosed()){
                Socket socket = serverSocket.accept();
                System.out.println("A client has connected!");
                SocketHandler socketHandler = new SocketHandler(socket);
                Thread thread = new Thread(socketHandler);
                thread.start();

            }
        } catch (IOException e) {
            //throw new RuntimeException(e);
            closeServerSocket();
        }
    }

    public void closeServerSocket() throws IOException {
        if(serverSocket != null){
            serverSocket.close();
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket socket = new ServerSocket(9090);
        Server server = new Server(socket);
        server.startServer();
    }

/*    public static void main(String[] args) throws IOException {
        ServerSocket socket = new ServerSocket(1234);
        Server server = new Server(socket);
    }*/
}
