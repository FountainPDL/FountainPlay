package com.fountainpdl.fountainplay;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.fountainpdl.fountainplay.util.ThemeSetup;

/** Applies the user's accent / Material You / AMOLED choices to every screen that extends it. */
public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeSetup.apply(this, true);
    }
}
