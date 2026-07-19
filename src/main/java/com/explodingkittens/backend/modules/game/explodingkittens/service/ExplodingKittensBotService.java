package com.explodingkittens.backend.modules.game.explodingkittens.service;

import com.explodingkittens.backend.modules.game.explodingkittens.dto.GameResult;
import com.explodingkittens.backend.modules.game.explodingkittens.model.GameState;

public interface ExplodingKittensBotService {
    void scheduleBotTurn(String roomId);
    void schedulePendingResolution(String roomId, GameResult result);
    void publishResult(String roomId, String eventType, GameResult result);
    void publishState(String roomId, GameState state, String eventType, String message);
}
