package com.explodingkittens.backend.modules.game.explodingkittens.service.impl;

import com.explodingkittens.backend.modules.game.explodingkittens.dto.BotPlay;
import com.explodingkittens.backend.modules.game.explodingkittens.dto.GameEvent;
import com.explodingkittens.backend.modules.game.explodingkittens.dto.GameResult;
import com.explodingkittens.backend.modules.game.explodingkittens.dto.PrivateGameEvent;
import com.explodingkittens.backend.modules.game.explodingkittens.model.enums.CardType;
import com.explodingkittens.backend.modules.game.explodingkittens.model.enums.GamePhase;
import com.explodingkittens.backend.modules.game.explodingkittens.model.Card;
import com.explodingkittens.backend.modules.game.explodingkittens.model.GameState;
import com.explodingkittens.backend.modules.game.explodingkittens.model.PendingAction;
import com.explodingkittens.backend.modules.game.explodingkittens.model.Player;

import com.explodingkittens.backend.modules.game.explodingkittens.service.ExplodingKittensBotService;
import com.explodingkittens.backend.modules.game.explodingkittens.service.ExplodingKittensService;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExplodingKittensBotServiceImpl implements ExplodingKittensBotService {
    private static final long BOT_THINK_TIME_MS = 2500;

    private final ExplodingKittensService gameService;
    private final SimpMessagingTemplate messagingTemplate;
    private final TaskScheduler taskScheduler;
    private final Random random = new Random();
    private final Map<String, Boolean> queuedRooms = new ConcurrentHashMap<>();

    // Use @Lazy here to avoid circular dependency since gameService and botService depend on each other.
    public ExplodingKittensBotServiceImpl(@Lazy ExplodingKittensService gameService, SimpMessagingTemplate messagingTemplate, TaskScheduler taskScheduler) {
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
        this.taskScheduler = taskScheduler;
    }

    @Override
    public void scheduleBotTurn(String roomId) {
        if (roomId == null || queuedRooms.putIfAbsent(roomId, true) != null) {
            return;
        }

        taskScheduler.schedule(() -> {
            queuedRooms.remove(roomId);
            try {
                performBotStep(roomId);
            } catch (RuntimeException exception) {
                messagingTemplate.convertAndSend(
                        "/topic/rooms/" + roomId + "/events",
                        new GameEvent("BOT_ERROR", exception.getMessage(), null)
                );
            }
        }, Instant.now().plusMillis(BOT_THINK_TIME_MS));
    }

    private void performBotStep(String roomId) {
        GameState state = gameService.getState(roomId);
        if (state.getPhase() == GamePhase.FINISHED) {
            return;
        }

        PendingAction pending = state.getPendingAction();
        if (state.getPhase() == GamePhase.WAITING_FOR_BOMB_INSERTION && pending != null && isBot(state, pending.getActorId())) {
            int index = random.nextInt(state.getDrawPile().size() + 1);
            GameResult result = gameService.insertBomb(roomId, pending.getActorId(), index);
            publishResult(roomId, "BOT_INSERTED_BOMB", result);
            scheduleBotTurn(roomId);
            return;
        }

        if (state.getPhase() == GamePhase.WAITING_FOR_FAVOR_CARD && pending != null && isBot(state, pending.getTargetPlayerId())) {
            Player bot = findPlayer(state, pending.getTargetPlayerId()).orElseThrow();
            if (!bot.getHand().isEmpty()) {
                Card card = bot.getHand().get(random.nextInt(bot.getHand().size()));
                GameResult result = gameService.giveFavorCard(roomId, bot.getId(), card.getId());
                publishResult(roomId, "BOT_GAVE_FAVOR", result);
                scheduleBotTurn(roomId);
            }
            return;
        }

        if (state.getPhase() == GamePhase.WAITING_FOR_NOPE && pending != null) {
            maybePlayNope(roomId, state, pending);
            return;
        }

        Player currentPlayer = state.getCurrentPlayer().orElse(null);
        if (state.getPhase() != GamePhase.ACTION_PHASE || currentPlayer == null || !currentPlayer.isBot()) {
            return;
        }

        GameResult result = chooseAction(roomId, state, currentPlayer);
        publishResult(roomId, "BOT_ACTION", result);
        schedulePendingResolution(roomId, result);
        scheduleBotTurn(roomId);
    }

    private GameResult chooseAction(String roomId, GameState state, Player bot) {
        Optional<BotPlay> playable = choosePlayableCard(state, bot);
        if (playable.isPresent() && random.nextDouble() < 0.62) {
            BotPlay play = playable.get();
            return gameService.playCard(roomId, bot.getId(), play.getCardIds(), play.getTargetPlayerId());
        }
        return gameService.drawCard(roomId, bot.getId());
    }

    private Optional<BotPlay> choosePlayableCard(GameState state, Player bot) {
        List<Player> targets = state.getPlayers().stream()
                .filter(Player::isAlive)
                .filter(player -> !player.getId().equals(bot.getId()))
                .filter(player -> !player.getHand().isEmpty())
                .toList();

        Optional<BotPlay> catPair = chooseCatPair(bot, targets);
        if (catPair.isPresent()) {
            return catPair;
        }

        List<CardType> priority = List.of(CardType.ATTACK, CardType.SKIP, CardType.SHUFFLE, CardType.SEE_THE_FUTURE, CardType.FAVOR);
        for (CardType preferred : priority) {
            Optional<Card> card = findCard(bot, preferred);
            if (card.isPresent()) {
                Optional<BotPlay> play = gameService.evaluateBotPlay(bot, state, card.get(), targets);
                if (play.isPresent()) {
                    return play;
                }
            }
        }

        for (Card card : bot.getHand()) {
            if (!priority.contains(card.getType())) {
                Optional<BotPlay> play = gameService.evaluateBotPlay(bot, state, card, targets);
                if (play.isPresent()) {
                    return play;
                }
            }
        }

        return Optional.empty();
    }

    private Optional<BotPlay> chooseCatPair(Player bot, List<Player> targets) {
        if (targets.isEmpty()) {
            return Optional.empty();
        }
        Player target = targets.get(random.nextInt(targets.size()));

        for (CardType type : List.of(CardType.TACOCAT, CardType.BEARD_CAT, CardType.CATTERMELON, CardType.HAIRY_POTATO_CAT)) {
            List<Card> matches = bot.getHand().stream()
                    .filter(card -> card.getType() == type)
                    .limit(2)
                    .toList();
            if (matches.size() == 2) {
                return Optional.of(new BotPlay(List.of(matches.get(0).getId(), matches.get(1).getId()), target.getId()));
            }
        }
        return Optional.empty();
    }

    private void maybePlayNope(String roomId, GameState state, PendingAction pending) {
        if (pending.getNopeCount() >= 4 || random.nextDouble() > 0.45) {
            return;
        }

        List<Player> candidates = state.getPlayers().stream()
                .filter(Player::isBot)
                .filter(Player::isAlive)
                .filter(player -> !player.getId().equals(pending.getActorId()))
                .filter(player -> findCard(player, CardType.NOPE).isPresent())
                .toList();
        if (candidates.isEmpty()) {
            return;
        }

        Player bot = candidates.get(random.nextInt(candidates.size()));
        Card nope = findCard(bot, CardType.NOPE).orElseThrow();
        GameResult result = gameService.playCard(roomId, bot.getId(), List.of(nope.getId()), null);
        publishResult(roomId, "BOT_NOPE", result);
    }

    private Optional<Player> chooseTarget(GameState state, Player bot) {
        List<Player> targets = new ArrayList<>(state.getPlayers().stream()
                .filter(Player::isAlive)
                .filter(player -> !player.getId().equals(bot.getId()))
                .filter(player -> !player.getHand().isEmpty())
                .toList());
        if (targets.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(targets.get(random.nextInt(targets.size())));
    }

    private Optional<Card> findCard(Player player, CardType type) {
        return player.getHand().stream()
                .filter(card -> card.getType() == type)
                .findFirst();
    }

    private boolean isBot(GameState state, String playerId) {
        return findPlayer(state, playerId)
                .map(Player::isBot)
                .orElse(false);
    }

    private Optional<Player> findPlayer(GameState state, String playerId) {
        return state.getPlayers().stream()
                .filter(player -> player.getId().equals(playerId))
                .findFirst();
    }

    @Override
    public void schedulePendingResolution(String roomId, GameResult result) {
        if (result.getPendingActionId() == null || result.getResolveAt() == null) {
            return;
        }

        taskScheduler.schedule(() -> {
            GameResult resolved = gameService.resolvePendingAction(roomId, result.getPendingActionId());
            publishResult(roomId, "CARD_RESOLVED", resolved);
            scheduleBotTurn(roomId);
        }, result.getResolveAt());
    }

    @Override
    public void publishResult(String roomId, String eventType, GameResult result) {
        publishState(roomId, result.getState(), eventType, result.getPublicMessage());
        for (PrivateGameEvent event : result.getPrivateEvents()) {
            messagingTemplate.convertAndSend(
                    privateDestination(roomId, event.getPlayerId()),
                    new GameEvent(event.getType(), "Private event", event.getPayload())
            );
        }
    }

    @Override
    public void publishState(String roomId, GameState state, String eventType, String message) {
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/state", state);
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/events", new GameEvent(eventType, message, null));
    }

    private String privateDestination(String roomId, String playerId) {
        return "/topic/rooms/" + roomId + "/players/" + playerId;
    }
}
