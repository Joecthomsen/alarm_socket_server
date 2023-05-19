package org.example;

import java.util.ArrayList;
import java.util.List;

public class AppNode{
    private String userEmail;
    private List<AlarmNode> alarms = new ArrayList<>();
    private List<String> alarmLog = new ArrayList<>();
    public AppNode(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getId() {
        return userEmail;
    }

    public List<AlarmNode> getAlarms() {
        return alarms;
    }

    public void addAlarm(AlarmNode alarm){
        alarms.add(alarm);
    }

    public List<String> getAlarmLog() {
        return alarmLog;
    }

    public void addAlarmToLog(String log) {
        this.alarmLog.add(log);
    }
}
