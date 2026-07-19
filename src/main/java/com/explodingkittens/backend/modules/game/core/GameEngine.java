package com.explodingkittens.backend.modules.game.core;

public interface GameEngine {
    void initGame(String roomId);
    GameResult processAction(String roomId, String playerId, Object action);
}
