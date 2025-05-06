package com.example.medireminder.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Medication implements Serializable {
    private String id;
    private String name;
    private String dosage;
    private String instructions;
    private String notes;
    private List<MedicationReminder> reminders;
    private boolean active;

    public Medication() {
        this.id = UUID.randomUUID().toString();
        this.reminders = new ArrayList<>();
        this.active = true;
    }

    public Medication(String name, String dosage, String instructions) {
        this();
        this.name = name;
        this.dosage = dosage;
        this.instructions = instructions;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<MedicationReminder> getReminders() {
        return reminders;
    }

    public void addReminder(MedicationReminder reminder) {
        this.reminders.add(reminder);
    }

    public void removeReminder(MedicationReminder reminder) {
        this.reminders.remove(reminder);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}