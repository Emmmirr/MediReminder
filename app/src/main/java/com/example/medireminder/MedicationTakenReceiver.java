package com.example.medireminder;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MedicationTakenReceiver extends BroadcastReceiver {

    private static final String HISTORY_FILE = "historial_medicamentos.txt";
    private static final String TAG = "MedicationTakenReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        int medicamentoNumero = intent.getIntExtra("medicamento_numero", 1);
        registerMedicationTaken(context, medicamentoNumero);

        // Cancelar la notificación
        int notificationId = intent.getIntExtra("notification_id", medicamentoNumero);
        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(notificationId);
        }

        // Si la Activity está en primer plano, mostrar feedback visual
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            showFeedback(activity, medicamentoNumero);
        }
    }

    private void showFeedback(Activity activity, int medicamentoNumero) {
        // Crear y configurar el FrameLayout para el check
        FrameLayout feedbackContainer = new FrameLayout(activity);
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        feedbackContainer.setLayoutParams(containerParams);

        // Crear y configurar el ImageView para el check
        ImageView checkView = new ImageView(activity);
        FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(
                dpToPx(activity, 48),
                dpToPx(activity, 48)
        );
        checkParams.gravity = Gravity.CENTER;
        checkView.setLayoutParams(checkParams);
        checkView.setImageResource(R.drawable.ic_notification);
        checkView.setColorFilter(ContextCompat.getColor(activity, android.R.color.holo_green_dark));

        // Añadir el ImageView al container
        feedbackContainer.addView(checkView);

        // Añadir el container a la vista raíz de la actividad
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        rootView.addView(feedbackContainer);

        // Animar el check
        Animation scaleAnimation = AnimationUtils.loadAnimation(activity, R.anim.check_animation);
        checkView.startAnimation(scaleAnimation);

        // Mostrar Snackbar
        Snackbar snackbar = Snackbar.make(
                rootView,
                "Medicamento " + medicamentoNumero + " registrado correctamente",
                Snackbar.LENGTH_SHORT);

        snackbar.setBackgroundTint(ContextCompat.getColor(activity, android.R.color.holo_green_dark));
        snackbar.show();

        // Eliminar el feedback visual después de un tiempo
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            rootView.removeView(feedbackContainer);
        }, 1500);
    }

    private int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void registerMedicationTaken(Context context, int medicamentoNumero) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        String entryText = "Medicamento " + medicamentoNumero + " tomado el: " + currentTime + "\n";

        try {
            FileOutputStream fos = context.openFileOutput(HISTORY_FILE, Context.MODE_APPEND);
            fos.write(entryText.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}