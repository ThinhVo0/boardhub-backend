package com.explodingkittens.backend.modules.game.core;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameRegistry {
    private final Map<String, GameModule> registry = new ConcurrentHashMap<>();

    public void register(GameModule module) {
        registry.put(module.getGameId(), module);
    }

    public GameModule get(String gameId) {
        return registry.get(gameId);
    }
}
