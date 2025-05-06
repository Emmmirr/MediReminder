package com.example.medireminder;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private ImageView profileImage;
    private TextInputEditText nameInput;
    private TextInputEditText birthDateInput;
    private TextInputEditText medicationTypeInput;
    private Uri selectedImageUri;
    private SharedPreferences preferences;

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        selectedImageUri = data.getData();
                        profileImage.setImageURI(selectedImageUri);
                        // Guardar la URI de la imagen
                        preferences.edit()
                                .putString("profile_image_uri", selectedImageUri.toString())
                                .apply();
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Inicializar SharedPreferences
        preferences = requireActivity().getSharedPreferences("UserProfile",
                Activity.MODE_PRIVATE);

        // Inicializar vistas
        profileImage = view.findViewById(R.id.profile_image);
        nameInput = view.findViewById(R.id.name_input);
        birthDateInput = view.findViewById(R.id.birth_date_input);
        medicationTypeInput = view.findViewById(R.id.medication_type_input);
        Button changePhotoButton = view.findViewById(R.id.change_photo_button);
        Button saveProfileButton = view.findViewById(R.id.save_profile_button);

        // Cargar datos guardados
        loadProfileData();

        // Configurar selector de fecha
        birthDateInput.setOnClickListener(v -> showDatePicker());

        // Configurar selector de imagen
        changePhotoButton.setOnClickListener(v -> openImagePicker());

        // Configurar guardado de perfil
        saveProfileButton.setOnClickListener(v -> saveProfile());

        return view;
    }

    private void loadProfileData() {
        // Cargar datos del perfil
        nameInput.setText(preferences.getString("user_name", ""));
        birthDateInput.setText(preferences.getString("birth_date", ""));
        medicationTypeInput.setText(preferences.getString("medication_type", ""));

        // Cargar imagen de perfil
        String imageUriString = preferences.getString("profile_image_uri", "");
        if (!imageUriString.isEmpty()) {
            try {
                Uri imageUri = Uri.parse(imageUriString);
                profileImage.setImageURI(imageUri);
                selectedImageUri = imageUri;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    String formattedDate = sdf.format(selectedDate.getTime());
                    birthDateInput.setText(formattedDate);
                },
                year, month, day
        );

        // Establecer fecha máxima (hoy)
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImage.launch(intent);
    }

    private void saveProfile() {
        String name = nameInput.getText().toString().trim();
        String birthDate = birthDateInput.getText().toString().trim();
        String medicationType = medicationTypeInput.getText().toString().trim();

        if (name.isEmpty() || birthDate.isEmpty() || medicationType.isEmpty()) {
            Toast.makeText(getContext(), "Por favor, completa todos los campos",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Calcular edad
        int age = calculateAge(birthDate);

        // Guardar datos
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("user_name", name);
        editor.putString("birth_date", birthDate);
        editor.putString("medication_type", medicationType);
        editor.putInt("user_age", age);
        editor.apply();

        Toast.makeText(getContext(), "Perfil guardado correctamente",
                Toast.LENGTH_SHORT).show();
    }

    private int calculateAge(String birthDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date birthDateParsed = sdf.parse(birthDate);
            Calendar birth = Calendar.getInstance();
            Calendar today = Calendar.getInstance();

            if (birthDateParsed != null) {
                birth.setTime(birthDateParsed);
                int age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR);

                if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
                    age--;
                }

                return age;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }
}