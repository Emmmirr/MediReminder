package com.example.medireminder;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StatsFragment extends Fragment {
    private static final String TAG = "StatsFragment";
    private static final String[] DAYS_OF_WEEK = {"Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"};

    private final TextView[] dayTexts = new TextView[7];
    private final ProgressBar[] dayBars = new ProgressBar[7];
    private TextView weeklyProgressText;
    private ProgressBar weeklyProgressBar;
    private TextView commonTimeText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);
        initializeViews(view);
        loadStatistics();
        return view;
    }

    private void initializeViews(View view) {
        dayTexts[0] = view.findViewById(R.id.day_0_text);
        dayTexts[1] = view.findViewById(R.id.day_1_text);
        dayTexts[2] = view.findViewById(R.id.day_2_text);
        dayTexts[3] = view.findViewById(R.id.day_3_text);
        dayTexts[4] = view.findViewById(R.id.day_4_text);
        dayTexts[5] = view.findViewById(R.id.day_5_text);
        dayTexts[6] = view.findViewById(R.id.day_6_text);

        dayBars[0] = view.findViewById(R.id.day_0_bar);
        dayBars[1] = view.findViewById(R.id.day_1_bar);
        dayBars[2] = view.findViewById(R.id.day_2_bar);
        dayBars[3] = view.findViewById(R.id.day_3_bar);
        dayBars[4] = view.findViewById(R.id.day_4_bar);
        dayBars[5] = view.findViewById(R.id.day_5_bar);
        dayBars[6] = view.findViewById(R.id.day_6_bar);

        weeklyProgressText = view.findViewById(R.id.weekly_progress_text);
        weeklyProgressBar = view.findViewById(R.id.weekly_progress_bar);
        commonTimeText = view.findViewById(R.id.common_time_text);

        // Inicializar las barras de progreso con el color primario
        for (ProgressBar bar : dayBars) {
            if (bar != null) {
                bar.setProgress(0);
            }
        }
    }

    private void loadStatistics() {
        Map<Integer, Integer> dailyStats = new HashMap<>();
        Map<Integer, Integer> hourlyStats = new HashMap<>();
        Map<String, Boolean> weeklyCompletionMap = new HashMap<>();

        try {
            FileInputStream fis = requireContext().openFileInput("historial_medicamentos.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line;

            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            // Obtener la fecha de hace 7 días
            Calendar weekAgo = Calendar.getInstance();
            weekAgo.add(Calendar.DAY_OF_YEAR, -7);

            while ((line = reader.readLine()) != null) {
                try {
                    // Extraer la fecha del registro
                    int startIndex = line.indexOf("Medicamento tomado el: ") + "Medicamento tomado el: ".length();
                    int endIndex = line.indexOf(" a las");
                    if (startIndex >= 0 && endIndex >= 0) {
                        String dateTimeStr = line.substring(startIndex, endIndex);
                        dateTimeStr = dateTimeStr.trim();

                        // Obtener la hora
                        int timeStartIndex = line.indexOf("a las ") + "a las ".length();
                        String timeStr = line.substring(timeStartIndex, timeStartIndex + 5); // HH:mm

                        // Combinar fecha y hora
                        Date date = sdf.parse(dateTimeStr + " " + timeStr);

                        if (date != null) {
                            cal.setTime(date);

                            // Estadísticas diarias
                            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
                            dailyStats.merge(dayOfWeek, 1, Integer::sum);

                            // Estadísticas horarias
                            int hour = cal.get(Calendar.HOUR_OF_DAY);
                            hourlyStats.merge(hour, 1, Integer::sum);

                            // Progreso semanal (solo últimos 7 días)
                            if (cal.after(weekAgo)) {
                                String dateKey = String.format(Locale.getDefault(), "%d-%d-%d",
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH));
                                weeklyCompletionMap.put(dateKey, true);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing line: " + line, e);
                }
            }
            reader.close();

            // Actualizar la UI
            updateStats(dailyStats, hourlyStats, weeklyCompletionMap.size());

        } catch (IOException e) {
            Log.e(TAG, "Error reading history file", e);
        }
    }

    private void updateStats(Map<Integer, Integer> dailyStats,
                             Map<Integer, Integer> hourlyStats,
                             int daysCompleted) {
        // Encontrar el máximo valor diario para calcular porcentajes
        int maxDailyCount = 0;
        for (Integer count : dailyStats.values()) {
            if (count != null && count > maxDailyCount) {
                maxDailyCount = count;
            }
        }

        // Actualizar las barras de progreso y textos para cada día
        for (int i = 0; i < 7; i++) {
            int count = dailyStats.getOrDefault(i, 0);
            int progress = maxDailyCount > 0 ? (count * 100) / maxDailyCount : 0;

            if (dayBars[i] != null) {
                dayBars[i].setProgress(progress);
            }
            if (dayTexts[i] != null) {
                dayTexts[i].setText(String.format(Locale.getDefault(), "%s\n%d",
                        DAYS_OF_WEEK[i], count));
            }
        }

        // Encontrar la hora más común
        int maxHour = 0;
        int maxCount = 0;
        for (Map.Entry<Integer, Integer> entry : hourlyStats.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                maxHour = entry.getKey();
            }
        }

        // Actualizar texto de hora más común
        if (maxCount > 0) {
            commonTimeText.setText(String.format(Locale.getDefault(),
                    "Hora más frecuente: %02d:00 (%d tomas)", maxHour, maxCount));
        } else {
            commonTimeText.setText("No hay datos suficientes");
        }

        // Actualizar progreso semanal
        int weeklyProgress = (daysCompleted * 100) / 7;
        weeklyProgressBar.setProgress(weeklyProgress);
        weeklyProgressText.setText(String.format(Locale.getDefault(),
                "Progreso semanal: %d%% (%d/7 días)", weeklyProgress, daysCompleted));
    }
}