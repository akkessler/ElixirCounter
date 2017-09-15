package io.akessler.elixircounter;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;

/**
 * Created by Andy on 9/14/2017.
 */
public class OverlayService extends Service {

    WindowManager windowManager;
    Button[] buttons;

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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for(int i=0; i<buttons.length; i++) {
            Button b = buttons[i];
            if(buttons[i] != null) {
                windowManager.removeView(b);
                b = null;
            }
        }
    }
}