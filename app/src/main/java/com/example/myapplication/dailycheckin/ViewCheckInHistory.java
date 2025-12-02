package com.example.myapplication.dailycheckin;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.ResultCallBack;
import com.example.myapplication.UserManager;
import com.example.myapplication.childmanaging.SignInChildProfileActivity;
import com.example.myapplication.userdata.AccountType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class ViewCheckInHistory extends AppCompatActivity {
    TextView historyTextTitle;
    TextView historyText;
    String history = "";
    Button exportButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_history);
        this.historyTextTitle = (TextView)findViewById(R.id.view_history_title);
        Intent intent = getIntent();
        if(intent.hasExtra("isProvider")&&intent.hasExtra("childName")){
            historyTextTitle.setText("History for " + intent.getStringExtra("childName"));
        }else{
            historyTextTitle.setText("History for " + SignInChildProfileActivity.getCurrentChild().getName());
        }
        this.historyText = (TextView)findViewById(R.id.display_history);
        createHistory();
        exportButton = (Button)findViewById(R.id.export_pdf_button);
        exportButton.setOnClickListener(v -> {
            // Call your PDF export logic here.
            try {
                exportToPdf();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

    }

    public void returnToSymptoms(View view) {
        Intent intent = new Intent(this, FilterCheckInBySymptoms.class);
        Intent thisintent = getIntent();
        if(thisintent.hasExtra("permissionToTriggers")){
            intent.putExtra("permissionToTriggers", thisintent.getStringExtra("permissionToTriggers"));
        }
        if(thisintent.hasExtra("permissionToSymptoms")){
            intent.putExtra("permissionToSymptoms", thisintent.getStringExtra("permissionToSymptoms"));
        }
        if(thisintent.hasExtra("isProvider")){
            intent.putExtra("isProvider", thisintent.getStringExtra("isProvider"));
            intent.putExtra("childName", thisintent.getStringExtra("childName"));
        }
        startActivity(intent);
    }

    public void createHistory() {
        CheckInHistoryFilters filters = CheckInHistoryFilters.getInstance();
        CheckInModel.readFromDB(CheckInHistoryFilters.getInstance().getUsername(), filters.getStartDate(), filters.getEndDate(), new ResultCallBack<HashMap<String,DailyCheckin>>(){
            @Override
            public void onComplete(HashMap<String,DailyCheckin> result){
                ArrayList<String>datesInOrder = new ArrayList<>(result.keySet());
                //Toast.makeText(ViewCheckInHistory.this, "# of dates: " + datesInOrder.size(), Toast.LENGTH_SHORT).show();
                Collections.sort(datesInOrder);
                int datesProcessed = 0;
                for (String date: datesInOrder) {
                    DailyCheckin entry = result.get(date);
                    if (entry != null && filters.matchFilters(entry)) {
                        datesProcessed++;
                        String message = "" + date + "\n";
                        message = message + "Logged by: " + entry.getLoggedBy() + "\n";
                        if (!UserManager.currentUser.getAccount().equals(AccountType.PROVIDER) /*|| provider has permission to see symptoms*/) {
                            message = message + "Night waking: " + entry.getNightWaking() + "\n";
                            message = message + entry.getActivityLimits() + "\n";
                            message = message + "Cough/Wheeze level: " + entry.getCoughWheezeLevel() + "\n";
                        }

                        if (!UserManager.currentUser.getAccount().equals(AccountType.PROVIDER) /*|| provider has permission to see triggers*/) {
                            message = message + "Triggers: ";
                            for (String trigger : entry.getTriggers()) {
                                message = message + trigger + ", ";
                            }
                        }
                        message = message + "\n";
                        history = history + message + "\n";
                    }
                }
                //Toast.makeText(ViewCheckInHistory.this, "# of entries: " + datesProcessed, Toast.LENGTH_SHORT).show();
                if (history.isEmpty()) {
                    history = "No data for selected filters.";
                }
                historyText.setText(history);
                historyText.setTextSize(14);
            }
        });
    }

    public void exportToPdf() throws IOException {
        File documentsDir = this.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (documentsDir == null) {
            Toast.makeText(this, "Storage not available.", Toast.LENGTH_SHORT).show();
            return; // Stop here
        }
        File outputFile = new File(documentsDir, CheckInHistoryFilters.getInstance().getUsername()+"_check_in_history.pdf");

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.BLACK); // Set text color
        paint.setTextSize(20f); // Set text size
        String textToWrite = "History for " + CheckInHistoryFilters.getInstance().getUsername();
        canvas.drawText(textToWrite, 200, 24, paint);
        paint.setTextSize(14f);


        String[]historyEntries = history.split("\n");
        int entryPageCount = 1;
        for (String entry: historyEntries) {
            if (18*entryPageCount > 700) {
                entryPageCount = 0;
                document.finishPage(page);
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                paint = new Paint();
                paint.setTextSize(14f); // Set text size
                paint.setColor(Color.BLACK);
            }
            textToWrite = entry;
            canvas.drawText(textToWrite, 10, 24+(18*entryPageCount), paint);
            entryPageCount++;
        }
        document.finishPage(page);

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            document.writeTo(fos);
            Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "help me", Toast.LENGTH_SHORT).show();
        }

        document.close();
    }
}
