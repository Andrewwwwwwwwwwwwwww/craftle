package io.github.andrewwwwwwwwwwwwwww.craftle.game;

public enum GameStatus {
    IN_PROGRESS,
    WON,
    LOST;

    public static final GameStatus[] VALUES = values();

    public boolean finished() {
        return this != IN_PROGRESS;
    }

    public byte id() {
        return (byte) ordinal();
    }

    public static GameStatus byId(int id) {
        return VALUES[id];
    }
}
