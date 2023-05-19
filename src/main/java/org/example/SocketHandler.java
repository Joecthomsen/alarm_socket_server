package org.example;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

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
                this.bufferedWriter.write("Connected to server - OK");
                this.bufferedWriter.newLine();
                this.bufferedWriter.flush();
                String registration = bufferedReader.readLine();
                if(registration != null) {
                    try {
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
                                this.bufferedWriter.write("Device node registered: OK");
                                this.bufferedWriter.newLine();
                                this.bufferedWriter.flush();
                                System.out.println(new Date() + " Device node registered: OK");
                            } else if (Objects.equals(deviceType, "app")) {
                                appNode = new AppNode(jsonObject.getString("userId"));
                                this.bufferedWriter.write("App node registered: OK");
                                this.bufferedWriter.newLine();
                                this.bufferedWriter.flush();
                                System.out.println(new Date() + " App node registered: OK");
                            }
                            System.out.println(new Date() + " Registration of new client completed.");
                        }
                    }
                    catch (JSONException e){
                        System.out.println(new Date() + " Messages not a valid JSON object: " + e);
                    }
                }
                else {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                    System.out.println(new Date() + " Connection closed before registered!");
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
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

       // System.out.println(new Date() + " Message received: " + message);
    //Clear out any noise in the messages
        char firstChar = message.charAt(0);
        if(message.contains("{")) {
            while (firstChar != '{') {
                message = message.substring(1);
                firstChar = message.charAt(0);
            }
        }
        else{
            if(this.alarmNode != null) {
                System.out.println(new Date() + "ERROR: Alarm node with id" + this.alarmNode + " has send a non-JSON object");
            }
            else if(this.appNode != null) {
                System.out.println(new Date() + "ERROR: Application node with id" + this.appNode + " has send a non-JSON object");
            }
            return;
        }

        JSONObject jsonObject = new JSONObject(message);
        String request = jsonObject.getString("request");

    //Handle request from the alarm node
        if(this.alarmNode != null){
            switch (request){
                case "getDate":{
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+2"));
                    System.out.println(new Date() + " Date requested received from alarm device with ID: " + this.alarmNode.getDeviceId());
                    //respondToNodeRequest(simpleDateFormat.format(new Date().getTime()));
                    bufferedWriter.write(new Date().toString());
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                }
                break;
                case "alarm":{
                    String owner = jsonObject.getString("owner");
                    for (SocketHandler socketHandler : socketHandlers){
                        if(socketHandler.appNode != null){
                            if(Objects.equals(socketHandler.appNode.getId(), owner)){
                                JSONObject returnMessages = new JSONObject();
                                returnMessages.put("request", "alarm").put("sourceId", this.alarmNode.getDeviceId()).put("nicName", this.alarmNode.getNicName());
                                System.out.println(new Date() + " ALARM from device: " + this.alarmNode.getDeviceId() + " owned by appid: " + socketHandler.appNode.getId());
                                socketHandler.bufferedWriter.write(returnMessages.toString());
                                socketHandler.bufferedWriter.newLine();
                                socketHandler.bufferedWriter.flush();
                            }
                        }
                    }
                }
                break;
            }
        }

//Handle requests from the app
        else if(this.appNode != null){
            switch (request){
                case "addDevice": {
                    String deviceToAdd = jsonObject.getString("deviceId");
                    for (SocketHandler socketHandler : socketHandlers){
                        if(socketHandler.alarmNode != null){
                            if(Objects.equals(socketHandler.alarmNode.getDeviceId(), deviceToAdd)){
                                this.appNode.addAlarm(socketHandler.alarmNode);
                                System.out.println(new Date() + " Application node with ID: " + this.appNode.getId() + " has added an alarm with ID: " + socketHandler.alarmNode.getDeviceId());
                                System.out.println(new Date() + " Current alarms: " + this.appNode.getAlarms());
                                this.bufferedWriter.write("Alarm added: OK");
                                this.bufferedWriter.newLine();
                                this.bufferedWriter.flush();
                                break;

                            }
                        }
                    }
                }
                break;
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
        if(this.appNode != null) {
            System.out.println(new Date() + " App with id " + this.appNode.getId() + " has closed the socket");
        }
        else if(this.alarmNode != null) {
            System.out.println(new Date() + " Alarm with device id " + this.alarmNode.getDeviceId() + " has closed the socket");
        }
        socketHandlers.remove(this);
    }

/*    public Node getNode() {
        return node;
    }*/
}
