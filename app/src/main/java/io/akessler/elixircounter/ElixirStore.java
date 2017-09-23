package io.akessler.elixircounter;

import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by Andy on 9/16/2017.
 */
public class ElixirStore {

    private final static int MIN_ELIXIR = 0;

    private final static int MAX_ELIXIR = 10;

    private final static int START_ELIXIR = 5;

    private int elixir;

    private ProgressBar elixirBar;

    private TextView elixirText;


    public ElixirStore(ProgressBar elixirBar, TextView elixirText) {
        this.elixirText = elixirText;
        this.elixirBar = elixirBar;

        elixirBar.setMax(MAX_ELIXIR);
        reset();
    }

    public void reset() {
        elixir = START_ELIXIR;


        updateElixirText();
        updateElixirBar();
    }

    public void add(int i){
        elixir += i;
        if(elixir < MIN_ELIXIR) {
            elixir = MIN_ELIXIR;
        } else if(elixir > MAX_ELIXIR) {
            elixir = MAX_ELIXIR;
        }
        updateElixirBar();
        updateElixirText();
    }

    public int getElixir() {
        return elixir;
    }

    private void updateElixirBar() {
        elixirBar.setProgress(elixir);
    }

    private void updateElixirText() {
        elixirText.setText(String.valueOf(elixir));
    }


}
