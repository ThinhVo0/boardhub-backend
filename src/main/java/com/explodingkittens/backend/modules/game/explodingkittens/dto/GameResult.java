package com.explodingkittens.backend.modules.game.explodingkittens.dto;

import com.explodingkittens.backend.modules.game.explodingkittens.model.GameState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameResult {
    private GameState state;
    private String publicMessage;
    private String pendingActionId;
    private java.time.Instant resolveAt;
    private List<PrivateGameEvent> privateEvents = new java.util.ArrayList<>();

    public GameResult(GameState state, String publicMessage) {
        this.state = state;
        this.publicMessage = publicMessage;
        this.privateEvents = new ArrayList<>();
    }

    public void addPrivateEvent(PrivateGameEvent event) {
        this.privateEvents.add(event);
    }
}
