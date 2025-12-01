package com.example.myapplication.reports;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ProviderReportViewActivity extends AppCompatActivity {
    private static final String TAG = "ProviderReportView";

    private String pdfFilePath;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor fileDescriptor;
    private LinearLayout pdfPagesContainer;
    private Button buttonSavePDF;
    private TextView textViewTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_provider_report_view);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        pdfFilePath = getIntent().getStringExtra("pdfFilePath");
        String childName = getIntent().getStringExtra("childName");

        if (pdfFilePath == null) {
            Log.e(TAG, "PDF file path not provided");
            Toast.makeText(this, "PDF file not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews(childName);
        displayPDF();
    }

    private void initializeViews(String childName) {
        pdfPagesContainer = findViewById(R.id.pdfPagesContainer);
        buttonSavePDF = findViewById(R.id.buttonSavePDF);
        textViewTitle = findViewById(R.id.textViewTitle);

        if (childName != null) {
            textViewTitle.setText("Provider Report: " + childName);
        }

        buttonSavePDF.setOnClickListener(v -> savePDF());
    }

    private void displayPDF() {
        try {
            File pdfFile = new File(pdfFilePath);
            if (!pdfFile.exists()) {
                Toast.makeText(this, "PDF file not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(fileDescriptor);

            int pageCount = pdfRenderer.getPageCount();
            Log.d(TAG, "PDF has " + pageCount + " pages");

            for (int i = 0; i < pageCount; i++) {
                PdfRenderer.Page page = pdfRenderer.openPage(i);
                Bitmap bitmap = Bitmap.createBitmap(
                        page.getWidth() * 2,
                        page.getHeight() * 2,
                        Bitmap.Config.ARGB_8888
                );
                bitmap.eraseColor(android.graphics.Color.WHITE);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                ImageView imageView = new ImageView(this);
                imageView.setImageBitmap(bitmap);
                imageView.setAdjustViewBounds(true);
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 16);
                imageView.setLayoutParams(params);

                pdfPagesContainer.addView(imageView);

                page.close();
            }

        } catch (FileNotFoundException e) {
            Log.e(TAG, "PDF file not found", e);
            Toast.makeText(this, "PDF file not found", Toast.LENGTH_SHORT).show();
            finish();
        } catch (IOException e) {
            Log.e(TAG, "Error opening PDF", e);
            Toast.makeText(this, "Error opening PDF", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void savePDF() {
        try {
            File pdfFile = new File(pdfFilePath);
            if (!pdfFile.exists()) {
                Toast.makeText(this, "PDF file not found", Toast.LENGTH_SHORT).show();
                return;
            }

            android.net.Uri fileUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    pdfFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Save PDF Report"));

        } catch (Exception e) {
            Log.e(TAG, "Error saving PDF", e);
            Toast.makeText(this, "Error saving PDF", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pdfRenderer != null) {
            pdfRenderer.close();
        }
        if (fileDescriptor != null) {
            try {
                fileDescriptor.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing file descriptor", e);
            }
        }
    }
}

