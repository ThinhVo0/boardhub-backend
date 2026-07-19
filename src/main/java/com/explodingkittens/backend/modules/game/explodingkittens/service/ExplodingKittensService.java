package com.explodingkittens.backend.modules.game.explodingkittens.service;

import com.explodingkittens.backend.modules.game.explodingkittens.dto.BotPlay;
import com.explodingkittens.backend.modules.game.explodingkittens.dto.GameResult;
import com.explodingkittens.backend.modules.game.explodingkittens.model.Card;
import com.explodingkittens.backend.modules.game.explodingkittens.model.GameState;
import com.explodingkittens.backend.modules.game.explodingkittens.model.Player;

import java.util.List;
import java.util.Optional;

public interface ExplodingKittensService extends TurnManager {
    GameState joinRoom(String roomId, String playerId, String playerName);
    GameState startGame(String roomId);
    GameState addBots(String roomId, int botCount);
    GameState getState(String roomId);
    GameState leaveRoom(String roomId, String playerId);
    GameResult playCard(String roomId, String playerId, List<String> cardIds, String targetPlayerId);
    GameResult resolvePendingAction(String roomId, String pendingActionId);
    GameResult drawCard(String roomId, String playerId);
    GameResult insertBomb(String roomId, String playerId, int index);
    GameResult giveFavorCard(String roomId, String playerId, String cardId);
    Optional<BotPlay> evaluateBotPlay(Player bot, GameState state, Card card, List<Player> targets);
}
