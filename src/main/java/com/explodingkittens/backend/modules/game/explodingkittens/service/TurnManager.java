package com.explodingkittens.backend.modules.game.explodingkittens.service;

import com.explodingkittens.backend.modules.game.explodingkittens.model.GameState;

public interface TurnManager {
    void finishTurnWithoutDraw(GameState state);

    void addTurnsToNextPlayer(GameState state, int turns);
}
