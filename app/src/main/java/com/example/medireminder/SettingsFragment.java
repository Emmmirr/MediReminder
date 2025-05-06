package com.example.medireminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import android.util.Log;

public class SettingsFragment extends Fragment {

    private TextView reminder1Text, reminder2Text, reminder3Text;
    private Button setReminder1Button, setReminder2Button, setReminder3Button;
    private Button saveSettingsButton;
    private Switch notificationsSwitch;

    private SharedPreferences preferences;

    // Tiempo mínimo entre medicamentos (en minutos)
    private static final int MIN_TIME_BETWEEN_MEDICATIONS = 60;
    private static final String TAG = "SettingsFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        preferences = requireActivity().getSharedPreferences("MediPrefs", Context.MODE_PRIVATE);

        // Inicializar vistas
        reminder1Text = view.findViewById(R.id.reminder1_text);
        reminder2Text = view.findViewById(R.id.reminder2_text);
        reminder3Text = view.findViewById(R.id.reminder3_text);

        setReminder1Button = view.findViewById(R.id.set_reminder1_button);
        setReminder2Button = view.findViewById(R.id.set_reminder2_button);
        setReminder3Button = view.findViewById(R.id.set_reminder3_button);

        saveSettingsButton = view.findViewById(R.id.save_settings_button);
        notificationsSwitch = view.findViewById(R.id.notifications_switch);

        // Cargar configuraciones guardadas
        loadSavedSettings();

        // Configurar listeners para botones
        setReminder1Button.setOnClickListener(v -> showTimePickerDialog(1));
        setReminder2Button.setOnClickListener(v -> showTimePickerDialog(2));
        setReminder3Button.setOnClickListener(v -> showTimePickerDialog(3));

        saveSettingsButton.setOnClickListener(v -> saveSettings());

        return view;
    }

    private void loadSavedSettings() {
        // Cargar horas guardadas
        int hora1 = preferences.getInt("hora1", -1);
        int minuto1 = preferences.getInt("minuto1", -1);
        int hora2 = preferences.getInt("hora2", -1);
        int minuto2 = preferences.getInt("minuto2", -1);
        int hora3 = preferences.getInt("hora3", -1);
        int minuto3 = preferences.getInt("minuto3", -1);

        // Cargar estado de notificaciones
        boolean notificationsEnabled = preferences.getBoolean("notificaciones_activas", false);

        // Mostrar valores en UI
        if (hora1 != -1 && minuto1 != -1) {
            reminder1Text.setText(String.format("%02d:%02d", hora1, minuto1));
        }

        if (hora2 != -1 && minuto2 != -1) {
            reminder2Text.setText(String.format("%02d:%02d", hora2, minuto2));
        }

        if (hora3 != -1 && minuto3 != -1) {
            reminder3Text.setText(String.format("%02d:%02d", hora3, minuto3));
        }

        notificationsSwitch.setChecked(notificationsEnabled);
    }

    private void showTimePickerDialog(int reminderNumber) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                getContext(),
                (view, hourOfDay, minutes) -> {
                    // Guardar temporalmente la hora seleccionada
                    String timeString = String.format("%02d:%02d", hourOfDay, minutes);

                    // Verificar si la hora seleccionada está en conflicto con las existentes
                    if (isTimeInConflict(reminderNumber, hourOfDay, minutes)) {
                        // Mostrar mensaje de error y no actualizar la UI
                        return;
                    }
                    // Guardar temporalmente la hora seleccionada
                    switch (reminderNumber) {
                        case 1:
                            reminder1Text.setText(String.format("%02d:%02d", hourOfDay, minutes));
                            break;
                        case 2:
                            reminder2Text.setText(String.format("%02d:%02d", hourOfDay, minutes));
                            break;
                        case 3:
                            reminder3Text.setText(String.format("%02d:%02d", hourOfDay, minutes));
                            break;
                    }
                },
                hour,
                minute,
                true
        );

        timePickerDialog.show();
    }

    private boolean isTimeInConflict(int reminderNumber, int hourOfDay, int minute) {
        // Obtener todos los horarios actuales
        List<TimeInfo> times = new ArrayList<>();

        // Para el recordatorio 1
        if (reminderNumber != 1 && !reminder1Text.getText().toString().equals("No configurado")) {
            String[] time1 = reminder1Text.getText().toString().split(":");
            times.add(new TimeInfo(1, Integer.parseInt(time1[0]), Integer.parseInt(time1[1])));
        }

        // Para el recordatorio 2
        if (reminderNumber != 2 && !reminder2Text.getText().toString().equals("No configurado")) {
            String[] time2 = reminder2Text.getText().toString().split(":");
            times.add(new TimeInfo(2, Integer.parseInt(time2[0]), Integer.parseInt(time2[1])));
        }

        // Para el recordatorio 3
        if (reminderNumber != 3 && !reminder3Text.getText().toString().equals("No configurado")) {
            String[] time3 = reminder3Text.getText().toString().split(":");
            times.add(new TimeInfo(3, Integer.parseInt(time3[0]), Integer.parseInt(time3[1])));
        }

        // Añadir el nuevo tiempo que estamos validando
        TimeInfo newTime = new TimeInfo(reminderNumber, hourOfDay, minute);

        // Comprobar conflictos
        for (TimeInfo existingTime : times) {
            if (areTimesInConflict(newTime, existingTime)) {
                showTimeConflictError(newTime, existingTime);
                return true;
            }
        }

        return false;
    }

    private boolean areTimesInConflict(TimeInfo time1, TimeInfo time2) {
        // Convertir ambos horarios a minutos para facilitar la comparación
        int minutes1 = time1.hour * 60 + time1.minute;
        int minutes2 = time2.hour * 60 + time2.minute;

        // Calcular la diferencia en minutos (teniendo en cuenta que puede dar la vuelta al día)
        int diff = Math.min(
                Math.abs(minutes1 - minutes2),  // Diferencia directa
                1440 - Math.abs(minutes1 - minutes2)  // Diferencia considerando el ciclo de 24h (1440 min)
        );

        // Hay conflicto si los horarios son idénticos o están demasiado próximos
        return diff < MIN_TIME_BETWEEN_MEDICATIONS;
    }

    private void showTimeConflictError(TimeInfo newTime, TimeInfo existingTime) {
        String message = String.format(
                "Conflicto de horario: El recordatorio %d (%02d:%02d) está demasiado cerca del recordatorio %d (%02d:%02d). " +
                        "Debe haber al menos %d minutos entre medicaciones.",
                newTime.id, newTime.hour, newTime.minute,
                existingTime.id, existingTime.hour, existingTime.minute,
                MIN_TIME_BETWEEN_MEDICATIONS
        );

        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        Log.w(TAG, message);
    }

    private boolean validateMedicationTimes() {
        List<TimeInfo> times = new ArrayList<>();

        // Recopilar todos los horarios configurados
        if (!reminder1Text.getText().toString().equals("No configurado")) {
            String[] time1 = reminder1Text.getText().toString().split(":");
            times.add(new TimeInfo(1, Integer.parseInt(time1[0]), Integer.parseInt(time1[1])));
        }

        if (!reminder2Text.getText().toString().equals("No configurado")) {
            String[] time2 = reminder2Text.getText().toString().split(":");
            times.add(new TimeInfo(2, Integer.parseInt(time2[0]), Integer.parseInt(time2[1])));
        }

        if (!reminder3Text.getText().toString().equals("No configurado")) {
            String[] time3 = reminder3Text.getText().toString().split(":");
            times.add(new TimeInfo(3, Integer.parseInt(time3[0]), Integer.parseInt(time3[1])));
        }

        // Si hay menos de 2 horarios, no hay conflictos posibles
        if (times.size() < 2) {
            return true;
        }

        // Comprobar conflictos entre todos los pares de horarios
        for (int i = 0; i < times.size() - 1; i++) {
            for (int j = i + 1; j < times.size(); j++) {
                if (areTimesInConflict(times.get(i), times.get(j))) {
                    showTimeConflictError(times.get(i), times.get(j));
                    return false;
                }
            }
        }

        return true;
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = preferences.edit();

        // Guardar las horas de recordatorio
        String reminder1 = reminder1Text.getText().toString();
        String reminder2 = reminder2Text.getText().toString();
        String reminder3 = reminder3Text.getText().toString();

        if (!reminder1.equals("No configurado")) {
            String[] time = reminder1.split(":");
            editor.putInt("hora1", Integer.parseInt(time[0]));
            editor.putInt("minuto1", Integer.parseInt(time[1]));
        }

        if (!reminder2.equals("No configurado")) {
            String[] time = reminder2.split(":");
            editor.putInt("hora2", Integer.parseInt(time[0]));
            editor.putInt("minuto2", Integer.parseInt(time[1]));
        }

        if (!reminder3.equals("No configurado")) {
            String[] time = reminder3.split(":");
            editor.putInt("hora3", Integer.parseInt(time[0]));
            editor.putInt("minuto3", Integer.parseInt(time[1]));
        }

        // Guardar estado de notificaciones
        boolean notificationsEnabled = notificationsSwitch.isChecked();
        editor.putBoolean("notificaciones_activas", notificationsEnabled);

        editor.apply();

        // Configurar alarmas si las notificaciones están activadas
        if (notificationsEnabled) {
            scheduleNotifications();
        } else {
            cancelNotifications();
        }

        Toast.makeText(getContext(), "Configuración guardada", Toast.LENGTH_SHORT).show();
    }
    private static class TimeInfo {
        final int id;      // Número de recordatorio (1, 2 o 3)
        final int hour;    // Hora (0-23)
        final int minute;  // Minuto (0-59)

        TimeInfo(int id, int hour, int minute) {
            this.id = id;
            this.hour = hour;
            this.minute = minute;
        }
    }
    private void scheduleNotifications() {
        AlarmManager alarmManager = (AlarmManager) requireActivity().getSystemService(Context.ALARM_SERVICE);

        if (alarmManager == null) return;

        // Configurar las 3 alarmas
        scheduleAlarm(alarmManager, 1);
        scheduleAlarm(alarmManager, 2);
        scheduleAlarm(alarmManager, 3);
    }

    private void scheduleAlarm(AlarmManager alarmManager, int alarmNumber) {
        int hour = preferences.getInt("hora" + alarmNumber, -1);
        int minute = preferences.getInt("minuto" + alarmNumber, -1);

        if (hour == -1 || minute == -1) return;

        // Crear intent para el BroadcastReceiver
        Intent intent = new Intent(getContext(), NotificationReceiver.class);
        intent.putExtra("medicamento_numero", alarmNumber);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getContext(),
                alarmNumber,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Configurar tiempo para la alarma
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // Si la hora ya pasó hoy, programarla para mañana
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Establecer alarma diaria
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
    }

    private void cancelNotifications() {
        AlarmManager alarmManager = (AlarmManager) requireActivity().getSystemService(Context.ALARM_SERVICE);

        if (alarmManager == null) return;

        // Cancelar las 3 alarmas
        for (int i = 1; i <= 3; i++) {
            Intent intent = new Intent(getContext(), NotificationReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    getContext(),
                    i,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            alarmManager.cancel(pendingIntent);
        }
    }
}