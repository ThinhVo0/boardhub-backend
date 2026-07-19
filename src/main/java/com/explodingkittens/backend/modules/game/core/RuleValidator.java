package com.explodingkittens.backend.modules.game.core;

public interface RuleValidator<S, A> {
    boolean isValid(S state, A action);
}
