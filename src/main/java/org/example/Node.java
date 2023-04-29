package org.example;

import java.util.List;

public interface Node {

    String getId();
    void setId(String id);
    void handleAlarm();
    void handlePeriod();
    List<AlarmNode> getOwnedAlarms();
    void addAlarmDevice();
}
