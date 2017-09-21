package io.akessler.elixircounter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * Created by Andy on 9/16/2017.
 */
public class ElixirStore {

    private static ElixirStore elixirStore = null;

    private final static int MIN_ELIXIR = 0;

    private final static int MAX_ELIXIR = 10;

    private final static int START_ELIXIR = 5;

    private static int elixir;

    private static TextView elixirText;

    private ElixirStore(Context context, WindowManager windowManager) {
        elixir = START_ELIXIR;

        elixirText = new TextView(context);
        elixirText.setText(String.valueOf(elixir));
        elixirText.setTextColor(Color.MAGENTA);
        elixirText.setTypeface(Typeface.MONOSPACE);
        elixirText.setBackgroundColor(Color.argb(127,0,0,0));

        WindowManager.LayoutParams textParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, // FIXME Acts up on certain API versions
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        textParams.gravity = Gravity.RIGHT | Gravity.TOP;
        // FIXME Change these values to be dynamic, based on dimensions of screen
        textParams.x = 25;
        textParams.y = 150;

        windowManager.addView(elixirText, textParams);
    }

    // Might want to rethink this...
    public static ElixirStore createInstance(Context context, WindowManager windowManager) {
        if(elixirStore == null) {
            elixirStore = new ElixirStore(context, windowManager);
            return elixirStore;
        }
        throw new IllegalStateException("ElixirStore is a singleton class that has already been instantiated.");

    }

//    public static ElixirStore getInstance() {
//        return elixirStore;
//    }

    public static void add(int i){
        elixir += i;
        if(elixir < MIN_ELIXIR) {
            elixir = MIN_ELIXIR;
        } else if(elixir > MAX_ELIXIR) {
            elixir = MAX_ELIXIR;
        }
        elixirText.setText(String.valueOf(elixir));
    }

    public static int getElixir() {
        return elixir;
    }

    public static View getTextView() { return elixirText; }

    public static void destroy() {
        elixirStore = null;
    }
}
