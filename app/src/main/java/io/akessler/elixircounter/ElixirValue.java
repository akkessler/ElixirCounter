package io.akessler.elixircounter;

/**
 * Created by Andy on 9/22/2017.
 */
public enum ElixirValue {
    ONE("one", -1),
    TWO("two", -2),
    THREE("three", -3),
    FOUR("four", -4),
    FIVE("five", -5),
    SIX("six", -6),
    SEVEN("seven", -7),
    EIGHT("eight", -8),
    NINE("nine", -9),
    TEN("ten", -10),
    PUMP("pump", 1);

    String text;

    int value;

    ElixirValue(String text, int value) {
        this.text = text;
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public int getValue() {
        return value;
    }
}
