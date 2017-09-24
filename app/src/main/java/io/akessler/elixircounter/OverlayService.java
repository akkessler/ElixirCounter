package io.akessler.elixircounter;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static io.akessler.elixircounter.RecognitionListenerImpl.DIGITS_SEARCH;

/**
 * Created by Andy on 9/14/2017.
 */
public class OverlayService extends Service {

    private final static int ONGOING_NOTIFICATION_ID = 1337;

    private final static String EXIT_ACTION = "io.akessler.elixircounter.action.exit";

    private final static String STOP_ACTION = "io.akessler.elixircounter.action.stop";

    private final static String DISPLAY_ACTION = "io.akessler.elixircounter.action.display";

    WindowManager windowManager;

    View overlayView;

    ProgressBar elixirBar;

    TextView elixirText, speechText;

    FloatingActionButton startButton;

    CountDownTimer regularElixirTimer, doubleElixirTimer;

    SpeechRecognizer recognizer;

    ElixirStore elixirStore;

    private boolean recognizerReady, hidden;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, // Use TYPE_SYSTEM_OVERLAY to draw over lock screen & notification drawer
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        overlayView = layoutInflater.inflate(R.layout.activity_overlay, null);
        windowManager.addView(overlayView, params);

        elixirBar = (ProgressBar) overlayView.findViewById(R.id.elixirBar);
        elixirText = (TextView) overlayView.findViewById(R.id.elixirText);
        speechText = (TextView) overlayView.findViewById(R.id.speechText);

        initNotificationIntent();
        initTimers();
        initStartButton(); // TODO Extract this to XML

        elixirStore = new ElixirStore(elixirBar, elixirText);

        runRecognizerSetup();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }

        regularElixirTimer.cancel();
        doubleElixirTimer.cancel();

        windowManager.removeView(overlayView);
        windowManager.removeView(startButton);
    }

    private void start() {
        if(recognizerReady && recognizer != null) {
            recognizer.startListening(DIGITS_SEARCH);

            regularElixirTimer.start();

            startButton.setEnabled(false);
            startButton.hide();
        }
    }

    private void stop() {
        if(recognizer != null) {
            recognizer.cancel();
        }

        regularElixirTimer.cancel();
        doubleElixirTimer.cancel();
        elixirStore.reset();

        startButton.setEnabled(true);
        startButton.show();

        speechText.setText("");
    }

    private void hide() {
        overlayView.setVisibility(View.INVISIBLE);
        startButton.setVisibility(View.INVISIBLE);
        hidden = true;
    }

    private void show() {
        overlayView.setVisibility(View.VISIBLE);
        startButton.setVisibility(View.VISIBLE);
        hidden = false;
    }

    private void runRecognizerSetup() {
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        recognizerReady = false;
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(OverlayService.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    String text = "Failed to init recognizer " + result;
                    Toast.makeText(OverlayService.this, text, Toast.LENGTH_SHORT).show();
                } else {
                    recognizerReady = true;
                }
            }
        }.execute();
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
//                .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                .getRecognizer();
        // Seems slightly odd to pass reference of recognizer to the impl?
        RecognitionListenerImpl recognitionListener = new RecognitionListenerImpl(recognizer, elixirStore, speechText);
        recognizer.addListener(recognitionListener);

        // Create grammar-based search for digit recognition
        File digitsGrammar = new File(assetsDir, "digits.gram");
        recognizer.addGrammarSearch(DIGITS_SEARCH, digitsGrammar);
    }

    private void initTimers() {
        doubleElixirTimer = new CountDownTimer(120000, 1400) {
            public void onTick(long millisUntilFinished) {
                elixirStore.add(1);
            }

            public void onFinish() {
                Toast.makeText(OverlayService.this, R.string.timer_elixir2, Toast.LENGTH_SHORT).show();
                stop();
            }
        };

        regularElixirTimer = new CountDownTimer(120000, 2800) {
            public void onTick(long millisUntilFinished) {
                elixirStore.add(1);
            }

            public void onFinish() {
                Toast.makeText(OverlayService.this, R.string.timer_elixir1, Toast.LENGTH_SHORT).show();
                elixirStore.add(1);
                doubleElixirTimer.start();
            }
        };
    }

    private void initStartButton() {
        Context context = new ContextThemeWrapper(this, R.style.AppTheme); // TODO Figure out why wrapper is needed
        startButton = new FloatingActionButton(context);
        startButton.setBackgroundTintList(ColorStateList.valueOf(Color.MAGENTA));
        startButton.setImageResource(android.R.drawable.ic_media_play);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start();
            }
        });
        WindowManager.LayoutParams buttonParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        buttonParams.gravity = Gravity.CENTER;
        windowManager.addView(startButton, buttonParams);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if(STOP_ACTION.equals(action)) {
            stop();
        } else if(EXIT_ACTION.equals(action)) {
            stopForeground(true);
            stopSelf();
        } else if(DISPLAY_ACTION.equals(action)) {
            if(!hidden) {
                hide();
            }
            else {
                show();
            }
        }
        return START_STICKY;
    }

    private void initNotificationIntent() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, 0);

        Intent stopIntent = new Intent(this, OverlayService.class);
        stopIntent.setAction(STOP_ACTION);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 0, stopIntent, 0);
        NotificationCompat.Action stopAction = new NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_pause,
                getText(R.string.button_stop),
                stopPendingIntent).build();

        Intent exitIntent = new Intent(this, OverlayService.class);
        exitIntent.setAction(EXIT_ACTION);
        PendingIntent exitPendingIntent = PendingIntent.getService(
                this, 0, exitIntent, 0);
        NotificationCompat.Action exitAction = new NotificationCompat.Action.Builder(
                android.R.drawable.ic_delete,
                getText(R.string.button_exit),
                exitPendingIntent).build();

        Intent displayIntent = new Intent(this, OverlayService.class);
        displayIntent.setAction(DISPLAY_ACTION);
        PendingIntent displayPendingIntent = PendingIntent.getService(
                this, 0, displayIntent, 0);
        NotificationCompat.Action displayAction = new NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_view,
                getText(R.string.button_display),
                displayPendingIntent).build();

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getText(R.string.notification_title))
                .setContentText(getText(R.string.notification_message))
                .setPriority(Notification.PRIORITY_MAX)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setColor(0xFF00FF)
                .addAction(displayAction)
                .addAction(stopAction)
                .addAction(exitAction)
                .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

}