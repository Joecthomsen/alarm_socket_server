package org.example;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable{

    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String deviceID;
    private boolean devideTypeIsNode;


/*    private String deviceName;
    private String userEmail;
    private String startTime;
    private  String endTime;
    boolean alarm;
    boolean hasChanged;*/
    public ClientHandler(Socket socket) throws IOException {

        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            clientHandlers.add(this);
            sendMessageToNewClient("Connected, register device id");
            this.deviceID = bufferedReader.readLine();
            sendMessageToNewClient("Welcome " + this.deviceID + " status: OK");
            broadcastMessage("SERVER: " + deviceID + " has joined!");
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    @Override
    public void run() {
        String messagesFromClient;
        while(socket.isConnected()){
            try{
                messagesFromClient = bufferedReader.readLine();
                if(messagesFromClient != null){
                    broadcastMessage("Accepted " + messagesFromClient);
                }
                else{
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }

            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
        closeEverything(socket, bufferedReader, bufferedWriter);
    }

    private void sendMessageToNewClient(String messageToSend) {
        for(ClientHandler clientHandler : clientHandlers){
            try{
                clientHandler.bufferedWriter.write(messageToSend);
                clientHandler.bufferedWriter.newLine();
                clientHandler.bufferedWriter.flush();

            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    private void broadcastMessage(String messageToSend) {
        for(ClientHandler clientHandler : clientHandlers){
            try{
                if(!clientHandler.deviceID.equals(deviceID)) {
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    private void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        removeClientHandler();
        try{
            if(bufferedReader != null){
                bufferedReader.close();
            }
            if(bufferedWriter != null){
                bufferedWriter.close();
            }
            if(socket != null){
                socket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeClientHandler(){
        clientHandlers.remove(this);
        broadcastMessage("SERVER: " + deviceID + " has left!");
    }

/*    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter username: ");
        String username = scanner.nextLine();
        Socket socket1 =
    }*/

}
