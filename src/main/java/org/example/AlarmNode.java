package org.example;

import java.util.List;

public class AlarmNode{
    private String deviceId;
    private String owner;
    private String nicName;
    private String startTime;
    private String endTime;

    public AlarmNode(String deviceId, String ownedBy, String nicName, String startTime, String endTime) {
        this.deviceId = deviceId;
        this.owner = ownedBy;
        this.nicName = nicName;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return "AlarmNode{" +
                "deviceId='" + deviceId + '\'' +
                ", owner='" + owner + '\'' +
                ", nicName='" + nicName + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                '}';
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getOwner() {
        return owner;
    }

    public String getNicName() {
        return nicName;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
