package com.explodingkittens.backend.modules.game.explodingkittens.service.impl;

import com.explodingkittens.backend.modules.game.explodingkittens.dto.GameResult;
import com.explodingkittens.backend.modules.game.explodingkittens.dto.PrivateGameEvent;
import com.explodingkittens.backend.modules.game.explodingkittens.dto.BotPlay;
import com.explodingkittens.backend.modules.game.explodingkittens.effect.CardEffect;
import com.explodingkittens.backend.modules.game.explodingkittens.effect.CatPairEffect;
import com.explodingkittens.backend.modules.game.explodingkittens.effect.EffectContext;
import com.explodingkittens.backend.modules.game.explodingkittens.model.enums.CardCategory;
import com.explodingkittens.backend.modules.game.explodingkittens.model.enums.CardType;
import com.explodingkittens.backend.modules.game.explodingkittens.model.enums.GamePhase;
import com.explodingkittens.backend.modules.game.explodingkittens.model.enums.PendingActionType;
import com.explodingkittens.backend.modules.game.explodingkittens.model.enums.PlayerStatus;
import com.explodingkittens.backend.modules.game.explodingkittens.model.Card;
import com.explodingkittens.backend.modules.game.explodingkittens.model.GameState;
import com.explodingkittens.backend.modules.game.explodingkittens.model.PendingAction;
import com.explodingkittens.backend.modules.game.explodingkittens.model.Player;
import com.explodingkittens.backend.modules.game.explodingkittens.repository.ExplodingKittensRoomRepository;

import com.explodingkittens.backend.modules.game.explodingkittens.service.ExplodingKittensService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
public class ExplodingKittensServiceImpl implements ExplodingKittensService {
    public static final Duration NOPE_WINDOW = Duration.ofSeconds(5);

    private final ExplodingKittensRoomRepository roomRepository;
    private final Map<CardType, CardEffect> effects = new HashMap<>();
    private final CatPairEffect catPairEffect = new CatPairEffect();
    private final Random random = new Random();

    @Autowired
    public ExplodingKittensServiceImpl(ExplodingKittensRoomRepository roomRepository, List<CardEffect> registeredEffects) {
        this.roomRepository = roomRepository;
        for (CardEffect effect : registeredEffects) {
            if (effect.supports() != null) {
                register(effect);
            }
        }
    }

    private void register(CardEffect effect) {
        effects.put(effect.supports(), effect);
    }

    @Override
    public synchronized GameState joinRoom(String roomId, String playerId, String playerName) {
        String safeRoomId = requireText(roomId, "roomId");
        String safePlayerId = requireText(playerId, "playerId");
        String safePlayerName = requireText(playerName, "playerName");

        GameState state = roomRepository.findById(safeRoomId)
                .orElseGet(() -> {
                    GameState newState = new GameState(safeRoomId);
                    return roomRepository.save(newState);
                });

        if (state.getPhase() != GamePhase.WAITING_FOR_PLAYERS) {
            throw new IllegalStateException("Game already started.");
        }

        Optional<Player> existingPlayer = findPlayer(state, safePlayerId);
        if (existingPlayer.isPresent()) {
            existingPlayer.get().setStatus(PlayerStatus.CONNECTED);
            existingPlayer.get().setName(safePlayerName);
        } else {
            state.getPlayers().add(new Player(safePlayerId, safePlayerName, state.getPlayers().size()));
        }
        state.touch();
        return roomRepository.save(state);
    }

    @Override
    public synchronized GameState startGame(String roomId) {
        GameState state = getRoom(roomId);
        if (state.getPlayers().size() < 2) {
            throw new IllegalStateException("At least 2 players are required.");
        }
        if (state.getPlayers().size() > 5) {
            throw new IllegalStateException("The basic deck supports up to 5 players.");
        }

        List<Card> normalCards = new ArrayList<>();
        List<Card> defuses = new ArrayList<>();
        List<Card> explodingKittens = new ArrayList<>();
        for (Card card : createFullDeck()) {
            if (card.getType() == CardType.DEFUSE) {
                defuses.add(card);
            } else if (card.getType() == CardType.EXPLODING_KITTEN) {
                explodingKittens.add(card);
            } else {
                normalCards.add(card);
            }
        }

        Collections.shuffle(normalCards, random);
        Collections.shuffle(defuses, random);
        Collections.shuffle(explodingKittens, random);

        for (Player player : state.getPlayers()) {
            player.setHand(new ArrayList<>());
            player.setStatus(PlayerStatus.CONNECTED);
            player.setPendingTurns(1);
            for (int i = 0; i < 7; i++) {
                player.addCard(drawFrom(normalCards));
            }
            player.addCard(drawFrom(defuses));
        }

        List<Card> drawPile = new ArrayList<>(normalCards);
        for (int i = 0; i < state.getPlayers().size() - 1; i++) {
            drawPile.add(drawFrom(explodingKittens));
        }
        drawPile.addAll(defuses);
        Collections.shuffle(drawPile, random);

        state.setDrawPile(drawPile);
        state.setDiscardPile(new ArrayList<>());
        state.setLastPlayedCards(new ArrayList<>());
        state.setCurrentPlayerIndex(0);
        state.setPendingAction(null);
        state.setWinnerPlayerId(null);
        state.setPhase(GamePhase.ACTION_PHASE);
        state.touch();
        return roomRepository.save(state);
    }

    @Override
    public synchronized GameState addBots(String roomId, int botCount) {
        GameState state = getRoom(roomId);
        if (state.getPhase() != GamePhase.WAITING_FOR_PLAYERS) {
            throw new IllegalStateException("Bots can only be added before the game starts.");
        }

        int existingBots = (int) state.getPlayers().stream().filter(Player::isBot).count();
        int wantedBots = Math.max(0, botCount);
        for (int i = existingBots; i < wantedBots; i++) {
            if (state.getPlayers().size() >= 5) {
                break;
            }
            int botNumber = i + 1;
            String botId = "bot-" + botNumber;
            if (findPlayer(state, botId).isEmpty()) {
                state.getPlayers().add(new Player(botId, "Bot " + botNumber, state.getPlayers().size(), true));
            }
        }
        state.touch();
        return roomRepository.save(state);
    }

    @Override
    public synchronized GameState getState(String roomId) {
        return getRoom(roomId);
    }

    @Override
    public synchronized GameState leaveRoom(String roomId, String playerId) {
        GameState state = getRoom(roomId);
        Optional<Player> playerOpt = findPlayer(state, playerId);
        if (playerOpt.isPresent()) {
            Player player = playerOpt.get();
            if (state.getPhase() == GamePhase.ACTION_PHASE || state.getPhase() == GamePhase.WAITING_FOR_NOPE || state.getPhase() == GamePhase.WAITING_FOR_BOMB_INSERTION || state.getPhase() == GamePhase.WAITING_FOR_FAVOR_CARD) {
                player.setStatus(PlayerStatus.DEAD);
                if (player.getHand() != null) {
                    state.getDiscardPile().addAll(player.getHand());
                    player.setHand(new ArrayList<>());
                }
                
                checkWinner(state);
                
                Optional<Player> currPlayerOpt = state.getCurrentPlayer();
                if (currPlayerOpt.isPresent() && currPlayerOpt.get().getId().equals(playerId) && state.getPhase() != GamePhase.FINISHED) {
                    PendingAction pending = state.getPendingAction();
                    if (pending != null && (pending.getActorId().equals(playerId) || playerId.equals(pending.getTargetPlayerId()))) {
                        state.setPendingAction(null);
                    }
                    moveToNextAlivePlayer(state);
                }
            } else if (state.getPhase() == GamePhase.WAITING_FOR_PLAYERS) {
                state.getPlayers().remove(player);
                for (int i = 0; i < state.getPlayers().size(); i++) {
                    state.getPlayers().get(i).setSeatIndex(i);
                }
            }
            state.touch();
        }
        return roomRepository.save(state);
    }

    @Override
    public synchronized GameResult playCard(String roomId, String playerId, List<String> cardIds, String targetPlayerId) {
        GameState state = getRoom(roomId);
        Player actor = requirePlayer(state, playerId);
        List<Card> selectedCards = requireCards(actor, cardIds);

        if (selectedCards.size() == 1 && selectedCards.get(0).getType() == CardType.NOPE) {
            GameResult res = playNope(state, actor, selectedCards.get(0));
            roomRepository.save(state);
            return res;
        }

        ensureActionPhase(state);
        ensureCurrentPlayer(state, actor);
        validatePlayableCards(selectedCards);

        Player target = targetPlayerId == null || targetPlayerId.isBlank() ? null : requirePlayer(state, targetPlayerId);
        if (isTargetRequired(selectedCards) && target == null) {
            throw new IllegalArgumentException("This card requires a target player.");
        }
        if (target != null && actor.getId().equals(target.getId())) {
            throw new IllegalArgumentException("You cannot target yourself.");
        }

        actor.getHand().removeAll(selectedCards);
        state.getDiscardPile().addAll(selectedCards);
        state.setLastPlayedCards(new ArrayList<>(selectedCards));

        PendingAction pending = new PendingAction(PendingActionType.CARD_EFFECT, actor.getId(), selectedCards);
        pending.setTargetPlayerId(target == null ? null : target.getId());
        pending.setExpiresAt(Instant.now().plus(NOPE_WINDOW));
        state.setPendingAction(pending);
        state.setPhase(GamePhase.WAITING_FOR_NOPE);
        state.touch();

        GameResult result = new GameResult(state, actor.getName() + " đã đánh " + describeCards(selectedCards) + ". Đang đợi Phủ Nhận (Nope).");
        result.setPendingActionId(pending.getId());
        result.setResolveAt(pending.getExpiresAt());
        roomRepository.save(state);
        return result;
    }

    @Override
    public synchronized GameResult resolvePendingAction(String roomId, String pendingActionId) {
        GameState state = getRoom(roomId);
        PendingAction pending = state.getPendingAction();
        if (pending == null || pending.getType() != PendingActionType.CARD_EFFECT || !pending.getId().equals(pendingActionId)) {
            return new GameResult(state, "No pending card effect to resolve.");
        }

        Player actor = requirePlayer(state, pending.getActorId());
        List<Card> cards = pending.getPlayedCards();
        boolean noped = pending.isNoped();
        state.setPendingAction(null);
        state.setPhase(GamePhase.ACTION_PHASE);

        GameResult result = new GameResult(state, "");
        if (noped) {
            result.setPublicMessage(describeCards(cards) + " đã bị Phủ Nhận (Nope) vô hiệu hóa.");
            state.touch();
            roomRepository.save(state);
            return result;
        }

        Player target = pending.getTargetPlayerId() == null ? null : requirePlayer(state, pending.getTargetPlayerId());
        EffectContext context = new EffectContext(actor, target, cards, random, this);
        if (isCatPair(cards)) {
            catPairEffect.apply(state, context, result);
        } else {
            CardEffect effect = effects.get(cards.get(0).getType());
            if (effect == null) {
                throw new IllegalArgumentException("No effect registered for " + cards.get(0).getType());
            }
            effect.apply(state, context, result);
        }
        checkWinner(state);
        state.touch();
        roomRepository.save(state);
        return result;
    }

    @Override
    public synchronized GameResult drawCard(String roomId, String playerId) {
        GameState state = getRoom(roomId);
        ensureActionPhase(state);
        Player player = requirePlayer(state, playerId);
        ensureCurrentPlayer(state, player);
        if (state.getDrawPile().isEmpty()) {
            throw new IllegalStateException("Draw pile is empty.");
        }

        Card drawnCard = state.getDrawPile().remove(0);
        if (drawnCard.getType() == CardType.EXPLODING_KITTEN) {
            GameResult res = handleExplosion(state, player, drawnCard);
            roomRepository.save(state);
            return res;
        }

        player.addCard(drawnCard);
        consumeCurrentTurnAfterDraw(state);
        state.touch();
        roomRepository.save(state);
        return new GameResult(state, player.getName() + " đã rút 1 lá bài.");
    }

    @Override
    public synchronized GameResult insertBomb(String roomId, String playerId, int index) {
        GameState state = getRoom(roomId);
        PendingAction pending = state.getPendingAction();
        if (pending == null || pending.getType() != PendingActionType.INSERT_EXPLODING_KITTEN) {
            throw new IllegalStateException("No exploding kitten is waiting to be inserted.");
        }
        if (!pending.getActorId().equals(playerId)) {
            throw new IllegalStateException("Only the defusing player can insert the exploding kitten.");
        }

        Card bomb = pending.getPlayedCards().get(0);
        int safeIndex = Math.max(0, Math.min(index, state.getDrawPile().size()));
        state.getDrawPile().add(safeIndex, bomb);
        state.setPendingAction(null);
        consumeCurrentTurnAfterDraw(state);
        state.touch();
        roomRepository.save(state);
        return new GameResult(state, "Mèo Nổ đã được đặt lại vào chồng bài rút.");
    }

    @Override
    public synchronized GameResult giveFavorCard(String roomId, String playerId, String cardId) {
        GameState state = getRoom(roomId);
        PendingAction pending = state.getPendingAction();
        if (pending == null || pending.getType() != PendingActionType.FAVOR_CARD_SELECTION) {
            throw new IllegalStateException("No Favor request is waiting.");
        }
        if (!pending.getTargetPlayerId().equals(playerId)) {
            throw new IllegalStateException("Only the targeted player can choose a Favor card.");
        }

        Player giver = requirePlayer(state, playerId);
        Player receiver = requirePlayer(state, pending.getActorId());
        Card card = removeCardById(giver, cardId);
        receiver.addCard(card);
        state.setPendingAction(null);
        state.setPhase(GamePhase.ACTION_PHASE);
        state.touch();
        roomRepository.save(state);
        return new GameResult(state, giver.getName() + " đã đưa 1 lá bài cho " + receiver.getName() + ".");
    }

    @Override
    public void finishTurnWithoutDraw(GameState state) {
        Player currentPlayer = state.getCurrentPlayer().orElseThrow();
        currentPlayer.setPendingTurns(Math.max(0, currentPlayer.getPendingTurns() - 1));
        if (currentPlayer.getPendingTurns() > 0) {
            state.setPhase(GamePhase.ACTION_PHASE);
            return;
        }
        moveToNextAlivePlayer(state);
    }

    @Override
    public void addTurnsToNextPlayer(GameState state, int turns) {
        Player nextPlayer = findNextAlivePlayer(state)
                .orElseThrow(() -> new IllegalStateException("No next player found."));
        nextPlayer.setPendingTurns(Math.max(1, nextPlayer.getPendingTurns()) + turns);
    }

    private GameResult playNope(GameState state, Player actor, Card nope) {
        if (state.getPhase() != GamePhase.WAITING_FOR_NOPE || state.getPendingAction() == null) {
            throw new IllegalStateException("There is no action to Nope.");
        }

        actor.removeCard(nope);
        state.getDiscardPile().add(nope);
        state.setLastPlayedCards(List.of(nope));
        PendingAction pending = state.getPendingAction();
        pending.setNopeCount(pending.getNopeCount() + 1);
        state.touch();

        String message = pending.isNoped()
                ? actor.getName() + " đã đánh Phủ Nhận (Nope). Hành động trước đó sẽ bị hủy trừ khi có một lá Phủ Nhận khác."
                : actor.getName() + " đã đánh Phủ Nhận (Nope) đè lên Phủ Nhận. Hành động trước đó có hiệu lực trở lại.";
        return new GameResult(state, message);
    }

    private GameResult handleExplosion(GameState state, Player player, Card explodingKitten) {
        Optional<Card> defuse = player.getHand().stream()
                .filter(card -> card.getType() == CardType.DEFUSE)
                .findFirst();

        if (defuse.isEmpty()) {
            player.setStatus(PlayerStatus.DEAD);
            state.getDiscardPile().add(explodingKitten);
            checkWinner(state);
            if (state.getPhase() != GamePhase.FINISHED) {
                moveToNextAlivePlayer(state);
            }
            state.touch();
            return new GameResult(state, player.getName() + " đã bốc trúng Mèo Nổ và bị loại khỏi cuộc chơi!");
        }

        Card usedDefuse = defuse.get();
        player.removeCard(usedDefuse);
        state.getDiscardPile().add(usedDefuse);
        PendingAction insertion = new PendingAction(PendingActionType.INSERT_EXPLODING_KITTEN, player.getId(), List.of(explodingKitten));
        state.setPendingAction(insertion);
        state.setPhase(GamePhase.WAITING_FOR_BOMB_INSERTION);
        state.touch();

        GameResult result = new GameResult(state, player.getName() + " đã gỡ bom thành công!");
        result.addPrivateEvent(new PrivateGameEvent(player.getId(), "DEFUSE_REQUIRED", explodingKitten));
        return result;
    }

    private void consumeCurrentTurnAfterDraw(GameState state) {
        Player currentPlayer = state.getCurrentPlayer().orElseThrow();
        currentPlayer.setPendingTurns(Math.max(0, currentPlayer.getPendingTurns() - 1));
        if (currentPlayer.getPendingTurns() > 0) {
            state.setPhase(GamePhase.ACTION_PHASE);
            return;
        }
        moveToNextAlivePlayer(state);
    }

    private void moveToNextAlivePlayer(GameState state) {
        Optional<Player> nextPlayer = findNextAlivePlayer(state);
        if (nextPlayer.isEmpty()) {
            checkWinner(state);
            return;
        }
        state.setCurrentPlayerIndex(nextPlayer.get().getSeatIndex());
        if (nextPlayer.get().getPendingTurns() <= 0) {
            nextPlayer.get().setPendingTurns(1);
        }
        state.setPhase(GamePhase.ACTION_PHASE);
    }

    private Optional<Player> findNextAlivePlayer(GameState state) {
        if (state.getPlayers().isEmpty()) {
            return Optional.empty();
        }

        int playerCount = state.getPlayers().size();
        for (int offset = 1; offset <= playerCount; offset++) {
            Player candidate = state.getPlayers().get((state.getCurrentPlayerIndex() + offset) % playerCount);
            if (candidate.isAlive()) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private void checkWinner(GameState state) {
        List<Player> alivePlayers = state.getPlayers().stream()
                .filter(Player::isAlive)
                .sorted(Comparator.comparingInt(Player::getSeatIndex))
                .toList();
        if (alivePlayers.size() == 1) {
            Player winner = alivePlayers.get(0);
            winner.setStatus(PlayerStatus.WINNER);
            state.setWinnerPlayerId(winner.getId());
            state.setPhase(GamePhase.FINISHED);
        }
    }

    private List<Card> createFullDeck() {
        List<Card> deck = new ArrayList<>();
        for (CardType type : CardType.values()) {
            addCards(deck, type, type.getDefaultDeckCount());
        }
        return deck;
    }

    private void addCards(List<Card> deck, CardType type, int count) {
        for (int i = 0; i < count; i++) {
            deck.add(new Card(type));
        }
    }

    private GameState getRoom(String roomId) {
        return roomRepository.findById(requireText(roomId, "roomId"))
                .orElseThrow(() -> new IllegalArgumentException("Room not found."));
    }

    private Player requirePlayer(GameState state, String playerId) {
        return findPlayer(state, requireText(playerId, "playerId"))
                .orElseThrow(() -> new IllegalArgumentException("Player not found."));
    }

    private Optional<Player> findPlayer(GameState state, String playerId) {
        return state.getPlayers().stream()
                .filter(player -> player.getId().equals(playerId))
                .findFirst();
    }

    private List<Card> requireCards(Player player, List<String> cardIds) {
        if (cardIds == null || cardIds.isEmpty()) {
            throw new IllegalArgumentException("At least one card is required.");
        }

        List<Card> cards = new ArrayList<>();
        for (String cardId : cardIds) {
            cards.add(findCardById(player, cardId)
                    .orElseThrow(() -> new IllegalArgumentException("Card not found in hand: " + cardId)));
        }
        return cards;
    }

    private Card removeCardById(Player player, String cardId) {
        Card card = findCardById(player, requireText(cardId, "cardId"))
                .orElseThrow(() -> new IllegalArgumentException("Card not found in hand."));
        player.removeCard(card);
        return card;
    }

    private Optional<Card> findCardById(Player player, String cardId) {
        return player.getHand().stream()
                .filter(card -> card.getId().equals(cardId))
                .findFirst();
    }

    private void ensureActionPhase(GameState state) {
        if (state.getPhase() != GamePhase.ACTION_PHASE) {
            throw new IllegalStateException("The game is not accepting normal actions right now.");
        }
    }

    private void ensureCurrentPlayer(GameState state, Player player) {
        Player currentPlayer = state.getCurrentPlayer()
                .orElseThrow(() -> new IllegalStateException("No current player."));
        if (!currentPlayer.getId().equals(player.getId())) {
            throw new IllegalStateException("It is not this player's turn.");
        }
        if (!player.isAlive()) {
            throw new IllegalStateException("Dead players cannot act.");
        }
    }

    private void validatePlayableCards(List<Card> cards) {
        if (isCatPair(cards)) {
            return;
        }
        if (cards.size() != 1) {
            throw new IllegalArgumentException("Only matching cat cards can be played as a pair.");
        }
        CardType type = cards.get(0).getType();
        if (type.getCategory() != CardCategory.ACTION) {
            throw new IllegalArgumentException("This card cannot be played as a normal action.");
        }
    }

    private boolean isTargetRequired(List<Card> cards) {
        if (isCatPair(cards)) {
            return true;
        }
        if (cards.size() == 1) {
            CardEffect effect = effects.get(cards.get(0).getType());
            return effect != null && effect.requiresTarget();
        }
        return false;
    }

    @Override
    public Optional<BotPlay> evaluateBotPlay(Player bot, GameState state, Card card, List<Player> targets) {
        CardEffect effect = effects.get(card.getType());
        if (effect != null) {
            return effect.evaluateBotPlay(bot, state, card, targets);
        }
        return Optional.empty();
    }

    private boolean isCatPair(List<Card> cards) {
        return cards.size() == 2
                && cards.get(0).getType().isCatCard()
                && cards.get(0).getType() == cards.get(1).getType();
    }

    private String describeCards(List<Card> cards) {
        if (cards.isEmpty()) {
            return "các lá bài";
        }
        if (cards.size() == 1) {
            return "lá " + cards.get(0).getDisplayName();
        }
        return cards.size() + " lá " + cards.get(0).getDisplayName();
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    private Card drawFrom(List<Card> cards) {
        if (cards.isEmpty()) {
            throw new IllegalStateException("Cannot draw from an empty list.");
        }
        return cards.remove(0);
    }
}
