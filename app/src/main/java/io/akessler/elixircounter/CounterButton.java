package io.akessler.elixircounter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.widget.AppCompatButton;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;

/**
 * Created by Andy on 9/15/2017.
 */
public class CounterButton extends AppCompatButton implements OnTouchListener, OnClickListener {

    private int counterValue;

    private ElixirStore elixirStore;

    public CounterButton(Context context, ElixirStore elixirStore, int counterValue) {
        super(context);
        this.counterValue = counterValue;

        setOnTouchListener(this);
        setOnClickListener(this);

        String text = String.valueOf(counterValue);
        if(counterValue > 0) {
            text = '+' + text;
        }
        setText(text);
        setTypeface(Typeface.MONOSPACE);
        setTextColor(Color.MAGENTA);
        setBackgroundColor(Color.argb(127,0,0,0));
        setTextSize(TypedValue.COMPLEX_UNIT_PT, 14);
    }

    @Override
    public void onClick(View v) {
        elixirStore.add(counterValue);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // TODO Auto-generated method stub
        return false;
    }
}