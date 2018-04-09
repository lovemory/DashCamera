package com.askey.dvr.cdr7010.dashcam.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.askey.dvr.cdr7010.dashcam.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera2PreviewFragment.newInstance())
                    .commit();
        }
    }

}
