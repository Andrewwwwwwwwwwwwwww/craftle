package io.github.andrewwwwwwwwwwwwwww.craftle.game;

public enum GameMode {
    DAILY,
    RANDOM;

    public static final GameMode[] VALUES = values();

    public byte id() {
        return (byte) ordinal();
    }

    public static GameMode byId(int id) {
        return VALUES[id];
    }
}
