package org.example;

import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class SocketHandler implements Runnable{

    public static ArrayList<SocketHandler> socketHandlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    //private Node node;
    private AppNode appNode = null;
    private AlarmNode alarmNode = null;

    private String deviceID;
    private String deviceType;
    private List<String> deviceOwned = new ArrayList<>();

    public SocketHandler(Socket socket) throws IOException {

        String request;
        if(socket != null) {
            try {
                this.socket = socket;
                this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                socketHandlers.add(this);
                respondToNodeRequest("Connected to server - OK");
                String registration = bufferedReader.readLine();
                if(registration != null) {
                    assert registration != null;
                    JSONObject jsonObject = new JSONObject(registration);
                    request = jsonObject.get("request").toString();
                    if (request.equals("register")) {
                        deviceType = jsonObject.get("type").toString();
                        if (Objects.equals(deviceType, "node")) {
                            String deviceId = jsonObject.getString("deviceId");
                            String owner = jsonObject.getString("owner");
                            String nicName = jsonObject.getString("nicName");
                            String startTime = jsonObject.getString("startTime");
                            String endTime = jsonObject.getString("endTime");
                            alarmNode = new AlarmNode(deviceId, owner, nicName, startTime, endTime);
                            respondToNodeRequest("Device node registered: OK");
                        } else if (Objects.equals(deviceType, "app")) {
                            appNode = new AppNode(jsonObject.getString("userId"));
                            respondToNodeRequest("App node registered: OK");
                        }
                    }
                }
                else {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                    System.out.println("Connection closed before registered!");
                }
            } catch (IOException e) {
                respondToNodeRequest("Registered: Error");
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }

/*        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            socketHandlers.add(this);
            respondToNodeRequest("Connected to server - OK");
            JSONObject jsonObject = new JSONObject(bufferedReader.readLine());
            request = jsonObject.get("request").toString();

            if (request.equals("register")) {
                deviceType = jsonObject.get("type").toString();
                if (Objects.equals(deviceType, "node")) {
                    deviceID = jsonObject.get("deviceId").toString();
                } else if (Objects.equals(deviceType, "app")) {
                    this.userEmail = jsonObject.get("userEmail").toString();

                }
                respondToNodeRequest("Registered: OK");
            }
        } catch (IOException e) {
            respondToNodeRequest("Registered: Error");
            closeEverything(socket, bufferedReader, bufferedWriter);
        }*/
    }

    @Override
    public void run() {
        String messagesFromClient;
        while(socket.isConnected()){
            try{
                messagesFromClient = bufferedReader.readLine();
                if(messagesFromClient != null){
                    //broadcastMessage(messagesFromClient);
                    handleRequest(messagesFromClient);
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

    public void handleRequest(String message) throws IOException {

        JSONObject jsonObject = new JSONObject(message);
        String request = jsonObject.getString("request");

    //Handle request from the alarm node
        if(this.alarmNode != null){
            switch (request){
                case "getDate":{
                    respondToNodeRequest(new Date().toString());
                }
                break;
                case "alarm":{
                    String owner = jsonObject.getString("owner");
                    for (SocketHandler socketHandler : socketHandlers){
                        if(socketHandler.appNode != null){
                            if(Objects.equals(socketHandler.appNode.getId(), owner)){
                                JSONObject returnMessages = jsonObject.put("sourceId", this.alarmNode.getDeviceId()).put("nicName", this.alarmNode.getNicName());
                                socketHandler.bufferedWriter.write(returnMessages.toString());
                                socketHandler.bufferedWriter.newLine();
                                socketHandler.bufferedWriter.flush();
                            }
                        }
                    }
                }
            }
        }

//Handle requests from the app
        else if(this.appNode != null){
            switch (request){
                case "addDevice": {
                    String deviceToAdd = jsonObject.getString("deviceId");
                    for (SocketHandler socketHandler : socketHandlers){
                        if(socketHandler.alarmNode != null){
                            if(Objects.equals(socketHandler.alarmNode.getDeviceId(), deviceToAdd) && Objects.equals(socketHandler.alarmNode.getOwner(), this.appNode.getId())){
                                this.appNode.addAlarm(socketHandler.alarmNode);
                                socketHandler.bufferedWriter.write("OK");
                                socketHandler.bufferedWriter.newLine();
                                socketHandler.bufferedWriter.flush();
                                System.out.println("Alarm added: " + this.appNode.getAlarms());
                            }
                        }
                    }
                }
                case "setPeriod":{
                    String deviceIdToEdit = jsonObject.getString("deviceId");
                    String startTime = jsonObject.getString("startTime");
                    String endTime = jsonObject.getString("endTime");

                    for (SocketHandler socketHandler : socketHandlers){
                        if(socketHandler.alarmNode != null){
                            if(Objects.equals(socketHandler.alarmNode.getDeviceId(), deviceIdToEdit)){
                                socketHandler.alarmNode.setStartTime(startTime);
                                socketHandler.alarmNode.setEndTime(endTime);
                                JSONObject messageToDevice = new JSONObject();
                                messageToDevice.put("request", "setPeriod").put("startTime", startTime).put("endTime", endTime);
                                socketHandler.bufferedWriter.write(messageToDevice.toString());
                                socketHandler.bufferedWriter.newLine();
                                socketHandler.bufferedWriter.flush();
                                System.out.println(socketHandler.alarmNode.toString());
                            }
                        }
                    }
                }
            }
        }
    }


    public void sendMessageToNode(String deviceId, String message){

        for (SocketHandler socketHandler : socketHandlers){
            if(Objects.equals(socketHandler.deviceID, deviceId)){
                try{
                    socketHandler.bufferedWriter.write(message);
                    socketHandler.bufferedWriter.newLine();
                    socketHandler.bufferedWriter.flush();
                } catch (IOException e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        }
    }

    public void respondToNodeRequest(String response) {
        for(SocketHandler socketHandler : socketHandlers){
            if(Objects.equals(socketHandler.deviceID, this.deviceID)){
                try{
                    socketHandler.bufferedWriter.write(response);
                    socketHandler.bufferedWriter.newLine();
                    socketHandler.bufferedWriter.flush();

                } catch (IOException e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }

        }
    }

    public void broadcastMessage(String messageToSend) {
        for(SocketHandler socketHandler : socketHandlers){
            try{
                if(!socketHandler.deviceID.equals(deviceID)) {
                    socketHandler.bufferedWriter.write(messageToSend);
                    socketHandler.bufferedWriter.newLine();
                    socketHandler.bufferedWriter.flush();
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
        socketHandlers.remove(this);
        broadcastMessage("SERVER: " + deviceID + " has left!");
    }

/*    public Node getNode() {
        return node;
    }*/
}
