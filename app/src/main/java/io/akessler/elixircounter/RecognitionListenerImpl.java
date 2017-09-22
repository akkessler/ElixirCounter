package io.akessler.elixircounter;

import android.widget.TextView;

import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;


/**
 * Created by Andy on 9/22/2017.
 */
public class RecognitionListenerImpl implements RecognitionListener {

    private final static String DIGITS_SEARCH = "digits"; // FIXME In 2 locations

    private SpeechRecognizer recognizer;

    private TextView speechText;

    public RecognitionListenerImpl(SpeechRecognizer recognizer, TextView speechText) {
        this.speechText = speechText;
        this.recognizer = recognizer;
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
            String displayText = "";
            String text = hypothesis.getHypstr();
            for(String token : text.split("\\s+")) {
                ElixirValue ev = ElixirValue.valueOf(token.toUpperCase()); // TODO Handle IllegalArgumentException
                int value = ev.getValue();
                ElixirStore.add(value);
                displayText += String.valueOf(value) + ' ';
            }
            speechText.setText(displayText);
        }
    }

    @Override
    public void onError(Exception e) {
        System.err.println(e.toString()); // TODO Use logger
    }

    @Override
    public void onTimeout() {
        // Do nothing
    }
}
