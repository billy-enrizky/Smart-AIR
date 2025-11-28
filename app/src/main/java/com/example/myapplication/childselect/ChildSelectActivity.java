package com.example.myapplication.childselect;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.myapplication.R;
import java.util.ArrayList;

public class ChildSelectActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_select);

        ListView listView = findViewById(R.id.childListView);

        ArrayList<String> temp = new ArrayList<>();
        temp.add("Child A");
        temp.add("Child B");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, temp);

        listView.setAdapter(adapter);
    }
}
