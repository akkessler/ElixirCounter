package io.akessler.elixircounter;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.util.TypedValue;
import android.view.Gravity;
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

/**
 * Created by Andy on 9/14/2017.
 */
public class OverlayService extends Service {

    private final static int ONGOING_NOTIFICATION_ID = 1337;

    private final static String EXIT_ACTION = "io.akessler.elixircounter.action.exit";

    private final static String STOP_ACTION = "io.akessler.elixircounter.action.stop";

    private final static String DISPLAY_ACTION = "io.akessler.elixircounter.action.display";

    private final static String DIGITS_SEARCH = "digits"; // FIXME In 2 locations

    WindowManager windowManager;

    FloatingActionButton startButton;

    CountDownTimer regularElixirTimer, doubleElixirTimer;

    ProgressBar elixirBar;

    TextView elixirText, speechText;

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

        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        initNotificationIntent();

        initTimers();
        initStartButton();
        initElixirBar();
        initElixirText();
        initSpeechText();

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

        windowManager.removeView(elixirBar);
        windowManager.removeView(startButton);
        windowManager.removeView(elixirText);
        windowManager.removeView(speechText);
    }

    private void start() {
        if(recognizerReady && recognizer != null) {
            recognizer.startListening(DIGITS_SEARCH);

            initTimers(); // This call might not be needed?
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
        elixirBar.setVisibility(View.INVISIBLE);
        elixirText.setVisibility(View.INVISIBLE);
        speechText.setVisibility(View.INVISIBLE);
        startButton.setVisibility(View.INVISIBLE);

        hidden = true;
    }

    private void show() {
        elixirBar.setVisibility(View.VISIBLE);
        elixirText.setVisibility(View.VISIBLE);
        speechText.setVisibility(View.VISIBLE);
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
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, // FIXME Acts up on certain API versions
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        buttonParams.gravity = Gravity.CENTER;
        buttonParams.x = 0;
        buttonParams.y = 0;
        windowManager.addView(startButton, buttonParams);
    }

    private void initElixirBar() {
        elixirBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        elixirBar.getProgressDrawable().setColorFilter(Color.MAGENTA, android.graphics.PorterDuff.Mode.SRC_IN);
        elixirBar.setIndeterminate(false);
        elixirBar.setScaleY(4f);
        WindowManager.LayoutParams barParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, // FIXME Acts up on certain API versions
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        barParams.gravity = Gravity.TOP;
        // FIXME Change these values to be dynamic, based on dimensions of screen
        barParams.x = 0;
        barParams.y = 0;
        windowManager.addView(elixirBar, barParams);
    }

    private void initElixirText() {
        elixirText = new TextView(this);
        elixirText.setTextColor(Color.MAGENTA);
        elixirText.setTypeface(Typeface.MONOSPACE);
        elixirText.setBackgroundColor(Color.argb(127,0,0,0));
        elixirText.setTextSize(TypedValue.COMPLEX_UNIT_PT, 14);

        WindowManager.LayoutParams textParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, // FIXME Acts up on certain API versions
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        textParams.gravity = Gravity.TOP;
        // FIXME Change these values to be dynamic, based on dimensions of screen
        textParams.x = 0;
        textParams.y = 100;
        windowManager.addView(elixirText, textParams);
    }

    private void initSpeechText() {
        speechText = new TextView(this);
        speechText.setText("");
        speechText.setTypeface(Typeface.MONOSPACE);
        speechText.setTextColor(Color.MAGENTA);
        speechText.setBackgroundColor(Color.argb(127,0,0,0));
        speechText.setTextSize(TypedValue.COMPLEX_UNIT_PT, 14);
        WindowManager.LayoutParams textParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, // FIXME Acts up on certain API versions
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        textParams.gravity = Gravity.LEFT;
        textParams.x = 0;
        textParams.y = 0;
        windowManager.addView(speechText, textParams);
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
            if(!hidden)
                hide();
            else
                show();
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