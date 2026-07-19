package com.explodingkittens.backend.modules.game.explodingkittens.repository;

import com.explodingkittens.backend.modules.game.explodingkittens.model.GameState;

import java.util.Optional;

public interface ExplodingKittensRoomRepository {
    GameState save(GameState state);
    Optional<GameState> findById(String roomId);
    void deleteById(String roomId);
}
