package com.example.medireminder.models;

import java.io.Serializable;
import java.util.UUID;

public class MedicationReminder implements Serializable {
    private String id;
    private int hour;
    private int minute;
    private boolean enabled;
    private boolean[] daysOfWeek; // índice 0 = domingo, 1 = lunes, etc.

    public MedicationReminder() {
        this.id = UUID.randomUUID().toString();
        this.enabled = true;
        this.daysOfWeek = new boolean[7];
        // Por defecto, todos los días están activados
        for (int i = 0; i < 7; i++) {
            daysOfWeek[i] = true;
        }
    }

    public MedicationReminder(int hour, int minute) {
        this();
        this.hour = hour;
        this.minute = minute;
    }

    public String getId() {
        return id;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean[] getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDayEnabled(int dayIndex, boolean enabled) {
        if (dayIndex >= 0 && dayIndex < 7) {
            this.daysOfWeek[dayIndex] = enabled;
        }
    }

    public boolean isDayEnabled(int dayIndex) {
        return dayIndex >= 0 && dayIndex < 7 && daysOfWeek[dayIndex];
    }

    public String getTimeFormatted() {
        return String.format("%02d:%02d", hour, minute);
    }
}