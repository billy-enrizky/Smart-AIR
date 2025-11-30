package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ParentBadge extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_badge);

        EditText req1 = findViewById(R.id.req1);
        EditText req2 = findViewById(R.id.req2);
        EditText req3 = findViewById(R.id.req3);
        EditText req4 = findViewById(R.id.req4);
        EditText req5 = findViewById(R.id.req5);

        AchievementsModel.readFromDB(UserManager.currentUser.getID(), new ResultCallBack<Achievement>() {
            @Override
            public void onComplete(Achievement achievement) {

                if (achievement == null) {
                    Achievement a = new Achievement(UserManager.currentUser.getID());
                    AchievementsModel.writeIntoDB(a, new CallBack() {
                        @Override
                        public void onComplete() {
                            req1.setText(a.getBadgeRequirements()[0]);
                            req2.setText(a.getBadgeRequirements()[1]);
                            req3.setText(a.getBadgeRequirements()[2]);
                            req4.setText(a.getBadgeRequirements()[3]);
                            req5.setText(a.getBadgeRequirements()[4]);
                        }
                    });
                }
                else {
                    req1.setText(achievement.getBadgeRequirements()[0]);
                    req2.setText(achievement.getBadgeRequirements()[1]);
                    req3.setText(achievement.getBadgeRequirements()[2]);
                    req4.setText(achievement.getBadgeRequirements()[3]);
                    req5.setText(achievement.getBadgeRequirements()[4]);
                }
            }
        });

        Button backbutton = findViewById(R.id.backbutton);
        Button confirmbutton = findViewById(R.id.confirmbutton);

        backbutton.setOnClickListener(v -> {
            startActivity(new Intent(ParentBadge.this, ParentActivity.class));
        });

        String r1 = req1.getText().toString();
        String r2 = req2.getText().toString();
        String r3 = req3.getText().toString();
        String r4 = req4.getText().toString();
        String r5 = req5.getText().toString();

        confirmbutton.setOnClickListener(v -> {
            if (allValid(r1,r2,r3,r4,r5)){
                AchievementsModel.readFromDB(UserManager.currentUser.getID(), new ResultCallBack<Achievement>() {
                    @Override
                    public void onComplete(Achievement achievement) {

                        if (achievement == null) {Toast.makeText(ParentBadge.this,"Somehow no achievement.",Toast.LENGTH_SHORT).show();}
                        else {
                            achievement.setBadgeRequirements(new int[]{Integer.parseInt(r1),Integer.parseInt(r2),Integer.parseInt(r3),Integer.parseInt(r4),Integer.parseInt(r5)});
                            AchievementsModel.writeIntoDB(achievement, new CallBack() {
                                @Override
                                public void onComplete() {
                                    startActivity(new Intent(ParentBadge.this, ParentActivity.class));
                                }
                            });
                        }
                    }
                });
            }
            else{
                Toast.makeText(ParentBadge.this,"Numbers Invalid.",Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateInts(String a) {
            try {
                Integer.parseInt(a);
            } catch (Exception e) {
                return false;
            }
        return true;
    }

    private boolean allValid(String a, String b, String c, String d, String e){
        return validateInts(a) && validateInts(b) && validateInts(c) && validateInts(d) && validateInts(e);
    }

}
