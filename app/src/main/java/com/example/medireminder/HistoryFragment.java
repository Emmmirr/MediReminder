package com.example.medireminder;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryFragment extends Fragment {

    private TextView historyTextView;
    private Button registrarTomaButton;
    private Button exportarPdfButton;

    private static final String HISTORY_FILE = "historial_medicamentos.txt";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        historyTextView = view.findViewById(R.id.history_text);
        registrarTomaButton = view.findViewById(R.id.registrar_toma_button);
        exportarPdfButton = view.findViewById(R.id.exportar_pdf_button);


        // Cargar historial
        loadHistory();

        // Configurar botón para registrar toma
        registrarTomaButton.setOnClickListener(v -> registerMedicationTaken());

        exportarPdfButton.setOnClickListener(v -> exportToPdf());

        return view;
    }

    private void exportToPdf() {
        try {
            // Crear nombre de archivo con fecha y hora
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            String fileName = "MediReminder_" + timestamp + ".pdf";

            // Obtener el directorio de documentos de la aplicación
            File pdfFolder = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "MediReminder");
            if (!pdfFolder.exists()) {
                boolean created = pdfFolder.mkdirs();
                if (!created) {
                    Toast.makeText(getContext(), "No se pudo crear el directorio", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            File pdfFile = new File(pdfFolder, fileName);
            Log.d("PDF_EXPORT", "Attempting to save PDF to: " + pdfFile.getAbsolutePath());

            // Crear documento PDF
            PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            try {
                // Añadir título
                document.add(new Paragraph("Historial de Medicamentos - MediReminder Pro")
                        .setFontSize(18)
                        .setBold());

                document.add(new Paragraph("Generado el: " +
                        new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                                .format(new Date())));

                document.add(new Paragraph("\n")); // Espacio en blanco

                // Leer y añadir el contenido del historial
                FileInputStream fis = requireActivity().openFileInput(HISTORY_FILE);
                BufferedReader br = new BufferedReader(new InputStreamReader(fis));

                String line;
                while ((line = br.readLine()) != null) {
                    document.add(new Paragraph(line));
                }

                br.close();
                document.close();

                // Mostrar éxito y ruta del archivo
                String message = "PDF guardado en: " + pdfFile.getAbsolutePath();
                Log.d("PDF_EXPORT", "Success: " + message);

                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
                        .setAction("OK", null)
                        .show();

            } catch (Exception e) {
                Log.e("PDF_EXPORT", "Error writing PDF content", e);
                throw e;
            }

        } catch (Exception e) {
            Log.e("PDF_EXPORT", "Error exporting PDF", e);
            Toast.makeText(getContext(),
                    "Error al exportar el PDF: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        loadHistory();
    }
    
    

    private void loadHistory() {
        StringBuilder historyBuilder = new StringBuilder();

        try {
            FileInputStream fis = requireActivity().openFileInput(HISTORY_FILE);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);

            String line;
            while ((line = br.readLine()) != null) {
                historyBuilder.append(line).append("\n");
            }

            br.close();
            isr.close();
            fis.close();
        } catch (IOException e) {
            historyBuilder.append("No hay registros de toma de medicamentos.");
        }

        historyTextView.setText(historyBuilder.toString());
    }

    private void registerMedicationTaken() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        String entryText = "Medicamento tomado el: " + currentTime + "\n";

        try {
            FileOutputStream fos = requireActivity().openFileOutput(HISTORY_FILE, Context.MODE_APPEND);
            fos.write(entryText.getBytes());
            fos.close();

            loadHistory();
            Toast.makeText(getContext(), "Toma registrada correctamente", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error al guardar el registro", Toast.LENGTH_SHORT).show();
        }
    }
}