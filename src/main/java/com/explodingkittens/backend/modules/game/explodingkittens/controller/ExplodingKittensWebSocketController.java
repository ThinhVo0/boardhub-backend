package com.explodingkittens.backend.modules.game.explodingkittens.controller;

import com.explodingkittens.backend.modules.game.explodingkittens.dto.*;
import com.explodingkittens.backend.modules.game.explodingkittens.model.GameState;
import com.explodingkittens.backend.modules.game.explodingkittens.service.ExplodingKittensBotService;
import com.explodingkittens.backend.modules.game.explodingkittens.service.ExplodingKittensService;

import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Controller;

@Controller
public class ExplodingKittensWebSocketController {
    private final ExplodingKittensService gameService;
    private final ExplodingKittensBotService gameBotService;
    private final SimpMessagingTemplate messagingTemplate;
    private final TaskScheduler taskScheduler;

    public ExplodingKittensWebSocketController(ExplodingKittensService gameService, ExplodingKittensBotService gameBotService, SimpMessagingTemplate messagingTemplate, TaskScheduler taskScheduler) {
        this.gameService = gameService;
        this.gameBotService = gameBotService;
        this.messagingTemplate = messagingTemplate;
        this.taskScheduler = taskScheduler;
    }

    @MessageMapping("/rooms/join")
    public void joinRoom(JoinRoomRequest request) {
        GameState state = gameService.joinRoom(request.getRoomId(), request.getPlayerId(), request.getPlayerName());
        publishState(request.getRoomId(), state, "PLAYER_JOINED", request.getPlayerName() + " đã tham gia phòng.");
        gameBotService.scheduleBotTurn(request.getRoomId());
    }

    @MessageMapping("/rooms/leave")
    public void leaveRoom(LeaveRoomRequest request) {
        GameState state = gameService.leaveRoom(request.getRoomId(), request.getPlayerId());
        publishState(request.getRoomId(), state, "PLAYER_LEFT", request.getPlayerName() + " đã rời phòng.");
        gameBotService.scheduleBotTurn(request.getRoomId());
    }

    @MessageMapping("/rooms/start")
    public void startGame(StartGameRequest request) {
        if (request.getBotCount() > 0) {
            gameService.addBots(request.getRoomId(), request.getBotCount());
        }
        GameState state = gameService.startGame(request.getRoomId());
        publishState(request.getRoomId(), state, "GAME_STARTED", "Trò chơi bắt đầu!");
        gameBotService.scheduleBotTurn(request.getRoomId());
    }

    @MessageMapping("/cards/play")
    public void playCard(PlayCardRequest request) {
        GameResult result = gameService.playCard(request.getRoomId(), request.getPlayerId(), request.getCardIds(), request.getTargetPlayerId());
        publishResult(request.getRoomId(), "CARD_PLAYED", result);
        schedulePendingResolution(request.getRoomId(), result);
        gameBotService.scheduleBotTurn(request.getRoomId());
    }

    @MessageMapping("/cards/draw")
    public void drawCard(DrawCardRequest request) {
        GameResult result = gameService.drawCard(request.getRoomId(), request.getPlayerId());
        publishResult(request.getRoomId(), "CARD_DRAWN", result);
        gameBotService.scheduleBotTurn(request.getRoomId());
    }

    @MessageMapping("/cards/insert-bomb")
    public void insertBomb(InsertBombRequest request) {
        GameResult result = gameService.insertBomb(request.getRoomId(), request.getPlayerId(), request.getIndex());
        publishResult(request.getRoomId(), "BOMB_INSERTED", result);
        gameBotService.scheduleBotTurn(request.getRoomId());
    }

    @MessageMapping("/cards/give-favor")
    public void giveFavorCard(FavorCardRequest request) {
        GameResult result = gameService.giveFavorCard(request.getRoomId(), request.getPlayerId(), request.getCardId());
        publishResult(request.getRoomId(), "FAVOR_RESOLVED", result);
        gameBotService.scheduleBotTurn(request.getRoomId());
    }

    @MessageExceptionHandler
    public void handleException(Exception exception) {
        messagingTemplate.convertAndSend("/topic/errors", new GameEvent("ERROR", exception.getMessage(), null));
    }

    private void schedulePendingResolution(String roomId, GameResult result) {
        if (result.getPendingActionId() == null || result.getResolveAt() == null) {
            return;
        }
        taskScheduler.schedule(() -> {
            GameResult resolved = gameService.resolvePendingAction(roomId, result.getPendingActionId());
            publishResult(roomId, "CARD_RESOLVED", resolved);
            gameBotService.scheduleBotTurn(roomId);
        }, result.getResolveAt());
    }

    private void publishResult(String roomId, String eventType, GameResult result) {
        publishState(roomId, result.getState(), eventType, result.getPublicMessage());
        for (PrivateGameEvent event : result.getPrivateEvents()) {
            messagingTemplate.convertAndSend(
                    privateDestination(roomId, event.getPlayerId()),
                    new GameEvent(event.getType(), "Private event", event.getPayload())
            );
        }
    }

    private void publishState(String roomId, GameState state, String eventType, String message) {
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/state", state);
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/events", new GameEvent(eventType, message, null));
    }

    private String privateDestination(String roomId, String playerId) {
        return "/topic/rooms/" + roomId + "/players/" + playerId;
    }
}
