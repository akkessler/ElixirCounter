package io.akessler.elixircounter;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent overlayService = new Intent(this, OverlayService.class);
        startService(overlayService);
        finish();
    }
}
