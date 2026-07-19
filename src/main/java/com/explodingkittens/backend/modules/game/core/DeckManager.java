package com.explodingkittens.backend.modules.game.core;

import java.util.List;

public interface DeckManager<C> {
    void shuffle(List<C> deck);
    C draw(List<C> deck);
}
