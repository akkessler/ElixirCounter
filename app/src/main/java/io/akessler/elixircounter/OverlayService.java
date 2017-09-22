package io.akessler.elixircounter;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

/**
 * Created by Andy on 9/14/2017.
 */
public class OverlayService extends Service implements RecognitionListener {

    private final static int ONGOING_NOTIFICATION_ID = 1337;

    private final static String EXIT_ACTION = "io.akessler.elixircounter.action.exit";

    private final static String DIGITS_SEARCH = "digits";

    private SpeechRecognizer recognizer;

    WindowManager windowManager;

    Button[] counterButtons;

    Button startButton;

    TextView speechText;

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
        initCounterButtons();
        initSpeechText();

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

        windowManager.removeView(startButton);
        windowManager.removeView(speechText);

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
            buttonParams.x = 0;
            buttonParams.y = (counterButtons.length - i) * 175;
            windowManager.addView(counterButtons[i], buttonParams);
        }
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
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        textParams.gravity = Gravity.TOP;
        textParams.x = 0;
        textParams.y = 0;
        windowManager.addView(speechText, textParams);
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

    private void runRecognizerSetup() {
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
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
                    recognizer.startListening(DIGITS_SEARCH);
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
        recognizer.addListener(this);

        // Create grammar-based search for digit recognition
        File digitsGrammar = new File(assetsDir, "digits.gram");
        recognizer.addGrammarSearch(DIGITS_SEARCH, digitsGrammar);

//        // Phonetic search
//        File phoneticModel = new File(assetsDir, "en-phone.dmp");
//        recognizer.addAllphoneSearch(PHONE_SEARCH, phoneticModel);
    }

    @Override
    public void onBeginningOfSpeech() {
        // Do nothing
    }

    @Override
    public void onEndOfSpeech() {
        recognizer.stop();
        recognizer.startListening(DIGITS_SEARCH);
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        // Do nothing
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        if(hypothesis != null) {
            String text = hypothesis.getHypstr();
            for(String token : text.split("\\s+")) {
                ElixirValue ev = ElixirValue.valueOf(token.toUpperCase()); // TODO Handle IllegalArgumentException
                int value = ev.getValue();
                ElixirStore.add(value);
            }
            speechText.setText(String.valueOf(text));
        }
    }

    @Override
    public void onError(Exception e) {
        System.err.println(e.toString());
        Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTimeout() {
        Toast.makeText(this, "Recognizer timeout", Toast.LENGTH_SHORT).show();

    }
}