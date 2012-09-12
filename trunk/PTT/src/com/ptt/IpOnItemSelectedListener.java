package com.ptt;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Toast;

public class IpOnItemSelectedListener implements OnItemSelectedListener {
 
    public void onItemSelected(AdapterView<?> parent,
        View view, int pos, long id) {
      Toast.makeText(parent.getContext(), "press call to contact " +
          parent.getItemAtPosition(pos).toString(), Toast.LENGTH_LONG).show();
    }
 
    public void onNothingSelected(AdapterView parent) {
      // Do nothing.
    }
}
