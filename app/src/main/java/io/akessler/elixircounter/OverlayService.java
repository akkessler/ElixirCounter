package io.akessler.elixircounter;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

/**
 * Created by Andy on 9/14/2017.
 */
public class OverlayService extends Service {

    WindowManager windowManager;

    Button[] counterButtons;

    Button startButton, exitButton;

    CountDownTimer regularElixirTimer, doubleElixirTimer;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        ElixirStore.createInstance(this, windowManager); // TODO Revisit this design pattern...

        initTimers();

        initStartButton(); // TODO initStopButton();

        initExitButton();

        initCounterButtons();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        regularElixirTimer.cancel();
        doubleElixirTimer.cancel();

        windowManager.removeView(startButton);
        windowManager.removeView(exitButton);

        for(int i = 0; i < counterButtons.length; i++) {
            Button b = counterButtons[i];
            if(counterButtons[i] != null) {
                windowManager.removeView(b);
            }
        }

        // TODO Refactor?
        windowManager.removeView(ElixirStore.getTextView());
        ElixirStore.destroy(); // needed?
    }

    private void initTimers() {
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
    }

    private void initStartButton() {
        startButton = new Button(this);
        startButton.setText("Start");
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                regularElixirTimer.start();
                startButton.setEnabled(false);
                startButton.setVisibility(View.GONE);
            }
        });
        WindowManager.LayoutParams buttonParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, // FIXME Acts up on certain API versions
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        buttonParams.gravity = Gravity.CENTER;
        buttonParams.x = 0;
        buttonParams.y = 0;
        windowManager.addView(startButton, buttonParams);
    }

    private void initExitButton() {
        exitButton = new Button(this);
        exitButton.setText("Exit");
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OverlayService.this.stopSelf();
            }
        });
        WindowManager.LayoutParams buttonParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, // FIXME Acts up on certain API versions
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        buttonParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        buttonParams.x = 0;
        buttonParams.y = 0;
        windowManager.addView(exitButton, buttonParams);
    }

    private void initCounterButtons() {
        counterButtons = new Button[11];
        for(int i = 0; i < counterButtons.length; i++) {
            int counterValue = i != 0 ? -i : 1; // FIXME There might be a cleaner way...
            counterButtons[i] = new CounterButton(this, counterValue);
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
            buttonParams.y = (counterButtons.length - i) * 150;
            windowManager.addView(counterButtons[i], buttonParams);
        }
    }
}