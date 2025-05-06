package com.example.medireminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import java.util.Calendar;

public class MedicationMissedChecker {
    private static final String CHANNEL_ID = "missed_medication_channel";
    private static final String CHANNEL_NAME = "Medicamentos Omitidos";
    private static final String PREFS_LAST_CHECK = "last_check_times";
    private final Context context;

    public MedicationMissedChecker(Context context) {
        this.context = context;
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alertas de medicamentos no tomados");

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public void checkMissedMedications() {
        SharedPreferences medicPrefs = context.getSharedPreferences("MediPrefs", Context.MODE_PRIVATE);
        SharedPreferences lastCheckPrefs = context.getSharedPreferences(PREFS_LAST_CHECK, Context.MODE_PRIVATE);
        Calendar now = Calendar.getInstance();

        // Verificar cada horario configurado
        checkMedicationTime(medicPrefs, lastCheckPrefs, now, 1);
        checkMedicationTime(medicPrefs, lastCheckPrefs, now, 2);
        checkMedicationTime(medicPrefs, lastCheckPrefs, now, 3);
    }

    private void checkMedicationTime(SharedPreferences medicPrefs,
                                     SharedPreferences lastCheckPrefs,
                                     Calendar now,
                                     int medicationNumber) {
        int hour = medicPrefs.getInt("hora" + medicationNumber, -1);
        int minute = medicPrefs.getInt("minuto" + medicationNumber, -1);

        if (hour == -1 || minute == -1) return;

        Calendar medicationTime = Calendar.getInstance();
        medicationTime.set(Calendar.HOUR_OF_DAY, hour);
        medicationTime.set(Calendar.MINUTE, minute);
        medicationTime.set(Calendar.SECOND, 0);

        // Si la hora actual es después de la hora programada
        if (now.after(medicationTime)) {
            // Verificar si han pasado 30 minutos
            Calendar thirtyMinutesAfter = (Calendar) medicationTime.clone();
            thirtyMinutesAfter.add(Calendar.MINUTE, 30);

            if (now.after(thirtyMinutesAfter)) {
                String lastCheckKey = "last_check_" + medicationNumber + "_" +
                        medicationTime.get(Calendar.DAY_OF_YEAR);
                boolean alreadyChecked = lastCheckPrefs.getBoolean(lastCheckKey, false);

                if (!alreadyChecked && !isMedicationTaken(medicationNumber, medicationTime)) {
                    showMissedMedicationAlert(medicationNumber, hour, minute);
                    // Marcar como verificado para no mostrar múltiples alertas
                    lastCheckPrefs.edit().putBoolean(lastCheckKey, true).apply();
                }
            }
        }
    }

    private boolean isMedicationTaken(int medicationNumber, Calendar medicationTime) {
        // Verificar en el historial si la medicación fue tomada
        String today = String.format("%02d/%02d/%d",
                medicationTime.get(Calendar.DAY_OF_MONTH),
                medicationTime.get(Calendar.MONTH) + 1,
                medicationTime.get(Calendar.YEAR));

        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(
                            context.openFileInput("historial_medicamentos.txt")
                    )
            );

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Medicamento " + medicationNumber) &&
                        line.contains(today)) {
                    reader.close();
                    return true;
                }
            }
            reader.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void showMissedMedicationAlert(int medicationNumber, int hour, int minute) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String formattedTime = String.format("%02d:%02d", hour, minute);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Medicamento Omitido")
                .setContentText("¿Olvidaste tu dosis de las " + formattedTime + "?")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(1000 + medicationNumber, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}