package com.wiret.arbrowser;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

/**
 * Created by farhad on 11/29/16.
 */

public class PlaceListActivity extends Activity{

    ListView listview;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.place_list);
        listview = (ListView) findViewById(R.id.listview);
        listview.setAdapter(new RowAdapter(this, new String[] { "data1",
                "data2" }));
    }
}
