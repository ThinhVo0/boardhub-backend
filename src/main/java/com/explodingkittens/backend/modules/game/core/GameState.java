package com.explodingkittens.backend.modules.game.core;

public interface GameState {
    String getRoomId();
    boolean isFinished();
}
