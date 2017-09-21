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
import android.support.v4.app.NotificationCompat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

/**
 * Created by Andy on 9/14/2017.
 */
public class OverlayService extends Service {

    private final static int ONGOING_NOTIFICATION_ID = 1337;

    private final static String EXIT_ACTION = "io.akessler.elixircounter.action.exit";

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

        initNotificationIntent();

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
                Toast.makeText(OverlayService.this, R.string.timer_elixir2, Toast.LENGTH_SHORT).show();
            }
        };

        regularElixirTimer = new CountDownTimer(120000, 2800) {
            public void onTick(long millisUntilFinished) {
                ElixirStore.add(1);
            }

            public void onFinish() {
                Toast.makeText(OverlayService.this, R.string.timer_elixir1, Toast.LENGTH_SHORT).show();
                ElixirStore.add(1);
                doubleElixirTimer.start();
            }
        };
    }

    private void initStartButton() {
        startButton = new Button(this);
        startButton.setText(R.string.button_start);
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
        exitButton.setText(R.string.button_exit);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OverlayService.this.stopSelf();
                OverlayService.this.stopForeground(true);
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(EXIT_ACTION.equals(intent.getAction())) {
            stopForeground(true);
            stopSelf();
        }
        return START_STICKY;
    }

    private void initNotificationIntent() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, 0);

        Intent exitIntent = new Intent(this, OverlayService.class);
        exitIntent.setAction(EXIT_ACTION);
        PendingIntent exitPendingIntent = PendingIntent.getService(
                this, 0, exitIntent, 0);
        NotificationCompat.Action exitAction = new NotificationCompat.Action.Builder(
                android.R.drawable.ic_delete,
                getText(R.string.button_exit),
                exitPendingIntent).build();

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getText(R.string.notification_title))
                .setContentText(getText(R.string.notification_message))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setColor(0xFF00FF)
                .addAction(exitAction)
                .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }
}