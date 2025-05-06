package com.example.medireminder;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private static final long CHECK_INTERVAL = 15 * 60 * 1000; // 15 minutos
    private Handler missedMedicationHandler;
    private MedicationMissedChecker missedChecker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Agregar este código en MainActivity.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
        }

        // Inicializar el Bottom Navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Cargar el fragmento home por defecto
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();

        // Configurar listener para la navegación
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            }else if (itemId == R.id.nav_stats) {
                selectedFragment = new StatsFragment();
            }else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            } else if (itemId == R.id.nav_history) {
                selectedFragment = new HistoryFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }

            return false;
        });

        missedChecker = new MedicationMissedChecker(this);
        missedMedicationHandler = new Handler(Looper.getMainLooper());

        // Iniciar verificación periódica
        startPeriodicCheck();
    }

    private void startPeriodicCheck() {
        missedMedicationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                missedChecker.checkMissedMedications();
                missedMedicationHandler.postDelayed(this, CHECK_INTERVAL);
            }
        }, CHECK_INTERVAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (missedMedicationHandler != null) {
            missedMedicationHandler.removeCallbacksAndMessages(null);
        }
    }
}