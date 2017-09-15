package io.akessler.elixircounter;

import android.content.Context;
import android.support.v7.widget.AppCompatButton;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Andy on 9/15/2017.
 */
public class CounterButton extends AppCompatButton implements View.OnTouchListener, View.OnClickListener {

    private int counterValue;

    public CounterButton(Context context, int counterValue) {
        super(context);
        this.counterValue = counterValue;
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // TODO Auto-generated method stub
        return false;
    }
}
