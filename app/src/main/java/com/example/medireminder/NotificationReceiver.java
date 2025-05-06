package com.example.medireminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "medi_reminder_channel";
    private static final String CHANNEL_NAME = "Recordatorios de Medicamentos";
    private static final String CHANNEL_DESC = "Canal para notificaciones de recordatorios de medicamentos";
    private static final String ACTION_MEDICATION_TAKEN = "com.example.medireminder.ACTION_MEDICATION_TAKEN";
    private static final String TAG = "NotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "NotificationReceiver activado");
        int medicamentoNumero = intent.getIntExtra("medicamento_numero", 1);
        Log.d(TAG, "Mostrando notificación para medicamento: " + medicamentoNumero);

        MedicationMissedChecker checker = new MedicationMissedChecker(context);
        checker.checkMissedMedications();

        // Crear canal de notificación (requerido para Android 8.0+)
        createNotificationChannel(context);

        // Crear intent para abrir la app al tocar la notificación
        Intent activityIntent = new Intent(context, MainActivity.class);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context,
                medicamentoNumero * 100,  // Request code único
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Crear intent para la acción "Tomado"
        Intent takenIntent = new Intent(context, MedicationTakenReceiver.class);
        takenIntent.setAction(ACTION_MEDICATION_TAKEN);
        takenIntent.putExtra("medicamento_numero", medicamentoNumero);
        takenIntent.putExtra("notification_id", medicamentoNumero);

        // Asegurarse de que el PendingIntent sea único para cada acción
        PendingIntent takenPendingIntent = PendingIntent.getBroadcast(
                context,
                medicamentoNumero * 200,  // Request code único diferente al anterior
                takenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Construir la notificación con el botón de acción
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Recordatorio de Medicamento")
                .setContentText("Es hora de tomar tu medicamento número " + medicamentoNumero)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentPendingIntent)
                .addAction(android.R.drawable.ic_menu_send, "Tomado", takenPendingIntent)
                .setAutoCancel(true);

        // Mostrar notificación
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(medicamentoNumero, builder.build());
            Log.d(TAG, "Notificación enviada con ID: " + medicamentoNumero);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}