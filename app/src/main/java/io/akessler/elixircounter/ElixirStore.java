package io.akessler.elixircounter;

import android.widget.TextView;

/**
 * Created by Andy on 9/16/2017.
 */
public class ElixirStore {

    private final static int MIN_ELIXIR = 0;

    private final static int MAX_ELIXIR = 10;

    private final static int START_ELIXIR = 5;

    private int elixir;

    private TextView elixirText;

    public ElixirStore(TextView elixirText) {
        this.elixirText = elixirText;

        elixir = START_ELIXIR;

        updateElixirText();
    }

    public void add(int i){
        elixir += i;
        if(elixir < MIN_ELIXIR) {
            elixir = MIN_ELIXIR;
        } else if(elixir > MAX_ELIXIR) {
            elixir = MAX_ELIXIR;
        }
        updateElixirText();
    }

    public int getElixir() {
        return elixir;
    }

    private void updateElixirText() {
        elixirText.setText(String.valueOf(elixir));
    }

}
