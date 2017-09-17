package io.akessler.elixircounter;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by Andy on 9/14/2017.
 */
public class OverlayService extends Service {

    WindowManager windowManager;

    Button[] buttons;

    CountDownTimer regularElixirTimer;

    CountDownTimer doubleElixirTimer;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        buttons = new Button[11];
        for(int i = 0; i < buttons.length; i++) {
            int counterValue = i != 0 ? -i : 1; // FIXME There might be a cleaner way...
            buttons[i] = new CounterButton(this, counterValue);
            WindowManager.LayoutParams buttonParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, // FIXME Acts up on certain API versions
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT
            );
            buttonParams.gravity = Gravity.LEFT | Gravity.TOP;
            // FIXME Change these values to be dynamic, based on dimensions of screen
            buttonParams.x = 25;
            buttonParams.y = (buttons.length - i) * 150;
            windowManager.addView(buttons[i], buttonParams);
        }

        ElixirStore.createInstance(this, windowManager);

        doubleElixirTimer = new CountDownTimer(120000, 1400) {
            public void onTick(long millisUntilFinished) {
                ElixirStore.add(1);
            }

            public void onFinish() {
                System.out.println("2x elixir time complete.");
            }
        };

        regularElixirTimer = new CountDownTimer(120000, 2800) {
            public void onTick(long millisUntilFinished) {
                ElixirStore.add(1);
            }

            public void onFinish() {
                System.out.println("1x elixir time complete.");
                ElixirStore.add(1);
                doubleElixirTimer.start();
            }
        };

        regularElixirTimer.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        regularElixirTimer.cancel();
        doubleElixirTimer.cancel();
        // FIXME Remove TextView from ElixirStore
        for(int i=0; i<buttons.length; i++) {
            Button b = buttons[i];
            if(buttons[i] != null) {
                windowManager.removeView(b);
                b = null;
            }
        }
    }
}