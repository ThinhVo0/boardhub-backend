package com.explodingkittens.backend.modules.game.explodingkittens.repository;

import com.explodingkittens.backend.modules.game.explodingkittens.model.GameState;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ExplodingKittensRoomRepositoryImpl implements ExplodingKittensRoomRepository {
    private final Map<String, GameState> rooms = new ConcurrentHashMap<>();

    @Override
    public GameState save(GameState state) {
        rooms.put(state.getRoomId(), state);
        return state;
    }

    @Override
    public Optional<GameState> findById(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    @Override
    public void deleteById(String roomId) {
        rooms.remove(roomId);
    }
}
