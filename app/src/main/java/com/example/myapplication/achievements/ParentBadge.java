package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.childmanaging.SignInChildProfileActivity;

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

        AchievementsModel.readFromDB(SignInChildProfileActivity.currentChild.getID(), new ResultCallBack<Achievement>() {
            @Override
            public void onComplete(Achievement achievement) {

                if (achievement == null) {
                    Achievement a = new Achievement(SignInChildProfileActivity.currentChild.getID());
                    AchievementsModel.writeIntoDB(a, new CallBack() {
                        @Override
                        public void onComplete() {
                            req1.setText(String.valueOf(a.getBadgeRequirements().get(0)));
                            req2.setText(String.valueOf(a.getBadgeRequirements().get(1)));
                            req3.setText(String.valueOf(a.getBadgeRequirements().get(2)));
                            req4.setText(String.valueOf(a.getBadgeRequirements().get(3)));
                            req5.setText(String.valueOf(a.getBadgeRequirements().get(4)));
                        }
                    });
                }
                else {
                    req1.setText(String.valueOf(achievement.getBadgeRequirements().get(0)));
                    req2.setText(String.valueOf(achievement.getBadgeRequirements().get(1)));
                    req3.setText(String.valueOf(achievement.getBadgeRequirements().get(2)));
                    req4.setText(String.valueOf(achievement.getBadgeRequirements().get(3)));
                    req5.setText(String.valueOf(achievement.getBadgeRequirements().get(4)));
                }
            }
        });

        Button backbutton = findViewById(R.id.backbutton);
        Button confirmbutton = findViewById(R.id.confirmbutton);

        backbutton.setOnClickListener(v -> {
            startActivity(new Intent(ParentBadge.this, ParentActivity.class));
        });

        confirmbutton.setOnClickListener(v -> {
            String r1 = req1.getText().toString().trim();
            String r2 = req2.getText().toString().trim();
            String r3 = req3.getText().toString().trim();
            String r4 = req4.getText().toString().trim();
            String r5 = req5.getText().toString().trim();
            if (allValid(r1,r2,r3,r4,r5)){
                AchievementsModel.readFromDB(SignInChildProfileActivity.currentChild.getID(), new ResultCallBack<Achievement>() {
                    @Override
                    public void onComplete(Achievement achievement) {

                        if (achievement == null) {Toast.makeText(ParentBadge.this,"Somehow no achievement.",Toast.LENGTH_SHORT).show();}
                        else {
                            ArrayList<Integer> temp = new ArrayList<Integer>();
                            temp.add(Integer.parseInt(r1));
                            temp.add(Integer.parseInt(r2));
                            temp.add(Integer.parseInt(r3));
                            temp.add(Integer.parseInt(r4));
                            temp.add(Integer.parseInt(r5));
                            achievement.setBadgeRequirements(temp);
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
                Toast.makeText(ParentBadge.this,a+" is invalid.",Toast.LENGTH_SHORT).show();
                return false;
            }
        return true;
    }

    private boolean allValid(String a, String b, String c, String d, String e){
        return validateInts(a) && validateInts(b) && validateInts(c) && validateInts(d) && validateInts(e);
    }

}
