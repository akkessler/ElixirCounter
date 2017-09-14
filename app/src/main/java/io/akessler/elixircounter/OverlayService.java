package io.akessler.elixircounter;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

/**
 * Created by Andy on 8/17/2017.
 */
public class OverlayService extends Service implements View.OnTouchListener, View.OnClickListener {

    WindowManager windowManager;
    Button button;
    float x0, y0, xOff, yOff;
    boolean moving;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        button = new Button(this);
        button.setOnTouchListener(this);
        button.setOnClickListener(this);
        button.setText("Click me!");

        WindowManager.LayoutParams buttonParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        buttonParams.gravity = Gravity.LEFT | Gravity.TOP;
        buttonParams.x = 0;
        buttonParams.y = 0;

        windowManager.addView(button, buttonParams);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(button != null) {
            windowManager.removeView(button);
            button = null;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                int[] buttonLocation = new int[2];
                button.getLocationOnScreen(buttonLocation);

                x0 = buttonLocation[0];
                y0 = buttonLocation[1];
                button.setText(buttonLocation[0] +";"+ buttonLocation[1]);

                xOff = x0 - event.getRawX();
                yOff = y0 - event.getRawY();
                moving = false;

                break;
            case MotionEvent.ACTION_MOVE:
                int x = (int) (xOff + event.getRawX());
                int y = (int) (yOff + event.getRawY());
                if(Math.abs(x - x0) < 1 && Math.abs(y - y0) < 1 && !moving) {
                    return false;
                }

                WindowManager.LayoutParams layoutParams =  (WindowManager.LayoutParams) button.getLayoutParams();
                layoutParams.x = x;
                layoutParams.y = y - button.getHeight();

                windowManager.updateViewLayout(button, layoutParams);
                moving = true;

                break;
            case MotionEvent.ACTION_UP:
                return moving;
            default:
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        Toast.makeText(this, "Hello World!", Toast.LENGTH_SHORT).show();
    }
}