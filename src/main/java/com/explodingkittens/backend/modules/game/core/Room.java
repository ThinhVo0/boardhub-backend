package com.explodingkittens.backend.modules.game.core;

import java.util.List;

public interface Room {
    String getId();
    List<? extends Player> getPlayers();
}
