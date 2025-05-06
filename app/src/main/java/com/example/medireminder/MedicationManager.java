package com.example.medireminder.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.medireminder.NotificationReceiver;
import com.example.medireminder.models.Medication;
import com.example.medireminder.models.MedicationReminder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MedicationManager {
    private static final String TAG = "MedicationManager";
    private static final String PREFS_NAME = "MedicationPrefs";
    private static final String MEDICATIONS_KEY = "medications";

    private static MedicationManager instance;
    private final Context context;
    private List<Medication> medications;
    private final Gson gson;

    private MedicationManager(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        loadMedications();
    }

    public static synchronized MedicationManager getInstance(Context context) {
        if (instance == null) {
            instance = new MedicationManager(context);
        }
        return instance;
    }

    private void loadMedications() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String medicationsJson = prefs.getString(MEDICATIONS_KEY, "");

        if (!medicationsJson.isEmpty()) {
            Type type = new TypeToken<ArrayList<Medication>>() {}.getType();
            medications = gson.fromJson(medicationsJson, type);
        } else {
            medications = new ArrayList<>();
        }
    }

    public void saveMedications() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String medicationsJson = gson.toJson(medications);
        editor.putString(MEDICATIONS_KEY, medicationsJson);
        editor.apply();
    }

    public List<Medication> getAllMedications() {
        return medications;
    }

    public Medication getMedicationById(String id) {
        for (Medication medication : medications) {
            if (medication.getId().equals(id)) {
                return medication;
            }
        }
        return null;
    }

    public void addMedication(Medication medication) {
        medications.add(medication);
        saveMedications();
        scheduleAlarmsForMedication(medication);
    }

    public void updateMedication(Medication medication) {
        // Primero cancelar todas las alarmas existentes
        cancelAlarmsForMedication(medication);

        // Actualizar el medicamento en la lista
        for (int i = 0; i < medications.size(); i++) {
            if (medications.get(i).getId().equals(medication.getId())) {
                medications.set(i, medication);
                break;
            }
        }

        // Guardar cambios y reprogramar alarmas
        saveMedications();
        scheduleAlarmsForMedication(medication);
    }

    public void deleteMedication(Medication medication) {
        cancelAlarmsForMedication(medication);
        medications.remove(medication);
        saveMedications();
    }

    public void scheduleAllAlarms() {
        for (Medication medication : medications) {
            if (medication.isActive()) {
                scheduleAlarmsForMedication(medication);
            }
        }
    }

    public void cancelAllAlarms() {
        for (Medication medication : medications) {
            cancelAlarmsForMedication(medication);
        }
    }

    private void scheduleAlarmsForMedication(Medication medication) {
        if (!medication.isActive()) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        for (MedicationReminder reminder : medication.getReminders()) {
            if (!reminder.isEnabled()) continue;

            for (int dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
                if (!reminder.isDayEnabled(dayOfWeek)) continue;

                // Configurar la alarma para este día específico
                scheduleAlarm(alarmManager, medication, reminder, dayOfWeek);
            }
        }
    }

    private void scheduleAlarm(AlarmManager alarmManager, Medication medication,
                               MedicationReminder reminder, int dayOfWeek) {
        // Crear intent para el BroadcastReceiver
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("medication_id", medication.getId());
        intent.putExtra("reminder_id", reminder.getId());

        // Generar un requestCode único basado en los IDs
        int requestCode = (medication.getId() + reminder.getId() + dayOfWeek).hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Configurar tiempo para la alarma
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, reminder.getHour());
        calendar.set(Calendar.MINUTE, reminder.getMinute());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Ajustar al próximo día de la semana correspondiente
        int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; // 0 = domingo
        if (currentDayOfWeek != dayOfWeek || calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            // Calcular cuántos días añadir para llegar al día deseado
            int daysToAdd = (dayOfWeek - currentDayOfWeek + 7) % 7;
            if (daysToAdd == 0 && calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                daysToAdd = 7; // Si ya pasó la hora hoy, programar para la próxima semana
            }
            calendar.add(Calendar.DAY_OF_YEAR, daysToAdd);
        }

        // Establecer alarma semanal
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY * 7, // Repetir cada 7 días
                pendingIntent
        );

        Log.d(TAG, "Alarma programada: " + medication.getName() +
                " a las " + reminder.getTimeFormatted() +
                " para el día " + getDayName(dayOfWeek));
    }

    private void cancelAlarmsForMedication(Medication medication) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        for (MedicationReminder reminder : medication.getReminders()) {
            for (int dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
                Intent intent = new Intent(context, NotificationReceiver.class);
                int requestCode = (medication.getId() + reminder.getId() + dayOfWeek).hashCode();

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                alarmManager.cancel(pendingIntent);
            }
        }
    }

    private String getDayName(int dayOfWeek) {
        switch (dayOfWeek) {
            case 0: return "Domingo";
            case 1: return "Lunes";
            case 2: return "Martes";
            case 3: return "Miércoles";
            case 4: return "Jueves";
            case 5: return "Viernes";
            case 6: return "Sábado";
            default: return "Desconocido";
        }
    }

    public void registerMedicationTaken(String medicationId, String reminderId) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());

        Medication medication = getMedicationById(medicationId);
        String medicationName = medication != null ? medication.getName() : "Desconocido";

        String entryText = "Medicamento '" + medicationName + "' tomado el: " + currentTime + "\n";

        try {
            FileOutputStream fos = context.openFileOutput("historial_medicamentos.txt", Context.MODE_APPEND);
            fos.write(entryText.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}