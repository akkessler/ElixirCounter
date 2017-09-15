package io.akessler.elixircounter;

import android.content.Context;
import android.support.v7.widget.AppCompatButton;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Toast;

/**
 * Created by Andy on 9/15/2017.
 */
public class CounterButton extends AppCompatButton implements OnTouchListener, OnClickListener {

    private Context context;
    private int counterValue;

    public CounterButton(Context context, int counterValue) {
        super(context);
        this.context = context;
        this.counterValue = counterValue;
        setOnTouchListener(this);
        setOnClickListener(this);
        setText(String.valueOf(counterValue));
    }

    @Override
    public void onClick(View v) {
        Toast.makeText(context, String.valueOf(counterValue), Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // TODO Auto-generated method stub
        return false;
    }
}