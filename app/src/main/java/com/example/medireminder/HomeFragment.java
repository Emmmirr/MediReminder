package com.example.medireminder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView welcomeMessage;
    private TextView nextReminderText;
    private ImageView profileImage;
    private TextView userAgeText;
    private TextView medicationTypeText;
    private CardView profileCard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        welcomeMessage = view.findViewById(R.id.welcome_message);
        nextReminderText = view.findViewById(R.id.next_reminder);
        profileImage = view.findViewById(R.id.profile_image_home);
        userAgeText = view.findViewById(R.id.user_age_text);
        medicationTypeText = view.findViewById(R.id.medication_type_text);
        profileCard = view.findViewById(R.id.profile_card);

        updateUI();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void updateUI() {
        // Obtener preferencias del usuario
        SharedPreferences preferences = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        SharedPreferences medicPrefs = requireActivity().getSharedPreferences("MediPrefs", Context.MODE_PRIVATE);

        String userName = preferences.getString("user_name", "");
        int userAge = preferences.getInt("user_age", 0);
        String medicationType = preferences.getString("medication_type", "");
        String imageUriString = preferences.getString("profile_image_uri", "");

        // Actualizar foto de perfil si existe
        if (!imageUriString.isEmpty()) {
            try {
                Uri imageUri = Uri.parse(imageUriString);
                profileImage.setImageURI(imageUri);
            } catch (Exception e) {
                profileImage.setImageResource(R.drawable.ic_profile_placeholder);
            }
        }

        // Actualizar información del perfil
        if (!userName.isEmpty()) {
            welcomeMessage.setText(String.format("¡Bienvenido/a, %s!", userName));
            userAgeText.setText(String.format("Edad: %d años", userAge));
            medicationTypeText.setText(String.format("Medicación: %s", medicationType));
            profileCard.setVisibility(View.VISIBLE);
        } else {
            welcomeMessage.setText("¡Bienvenido a MediReminder Pro!");
            profileCard.setVisibility(View.GONE);
        }

        // Mostrar próximas medicaciones
        updateMedicationReminders(medicPrefs);
    }

    private void updateMedicationReminders(SharedPreferences medicPrefs) {
        // Obtener horarios configurados
        int hora1 = medicPrefs.getInt("hora1", -1);
        int minuto1 = medicPrefs.getInt("minuto1", -1);
        int hora2 = medicPrefs.getInt("hora2", -1);
        int minuto2 = medicPrefs.getInt("minuto2", -1);
        int hora3 = medicPrefs.getInt("hora3", -1);
        int minuto3 = medicPrefs.getInt("minuto3", -1);

        StringBuilder reminderText = new StringBuilder("Próximas medicaciones:\n");
        boolean hasReminders = false;

        if (hora1 != -1 && minuto1 != -1) {
            reminderText.append(String.format("\n• %02d:%02d", hora1, minuto1));
            hasReminders = true;
        }
        if (hora2 != -1 && minuto2 != -1) {
            reminderText.append(String.format("\n• %02d:%02d", hora2, minuto2));
            hasReminders = true;
        }
        if (hora3 != -1 && minuto3 != -1) {
            reminderText.append(String.format("\n• %02d:%02d", hora3, minuto3));
            hasReminders = true;
        }

        if (!hasReminders) {
            reminderText = new StringBuilder("No hay recordatorios configurados.\nVe a Configuración para agregar.");
        }

        nextReminderText.setText(reminderText.toString());
    }
}