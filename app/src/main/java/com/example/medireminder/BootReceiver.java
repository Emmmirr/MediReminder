package com.example.medireminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            // Restaurar alarmas después de reiniciar el dispositivo
            SharedPreferences preferences = context.getSharedPreferences("MediPrefs", Context.MODE_PRIVATE);
            boolean notificationsEnabled = preferences.getBoolean("notificaciones_activas", false);

            if (notificationsEnabled) {
                // Programar nuevamente las alarmas
                // (el código detallado se omite para mantener el ejemplo simple,
                // pero sería similar al método scheduleNotifications() en SettingsFragment)
            }
        }
    }
}