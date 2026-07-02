package al.unyt.mashtruesi.web;

import al.unyt.mashtruesi.model.GameSession;
import al.unyt.mashtruesi.model.GameSettings;
import al.unyt.mashtruesi.model.PlayerAssignment;
import al.unyt.mashtruesi.model.RevealOutcome;
import al.unyt.mashtruesi.service.GameService;
import al.unyt.mashtruesi.web.dto.CreateGameRequest;
import al.unyt.mashtruesi.web.dto.CreateGameResponse;
import al.unyt.mashtruesi.web.dto.EndGameRequest;
import al.unyt.mashtruesi.web.dto.EndGameResponse;
import al.unyt.mashtruesi.web.dto.RevealResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    public ResponseEntity<CreateGameResponse> createGame(@RequestBody CreateGameRequest request) {
        GameSettings settings = GameSettings.builder()
                .players(request.players())
                .category(request.category())
                .imposterCount(request.imposterCount())
                .hintEnabled(request.hintEnabled() == null ? true : request.hintEnabled())
                .imposterFirstWeight(request.imposterFirstWeight() == null
                        ? 0.25 : request.imposterFirstWeight())
                .build();

        GameSession session = gameService.createGame(settings);

        List<String> order = session.getTurnOrder().stream()
                .map(PlayerAssignment::getPlayerName)
                .toList();

        CreateGameResponse body = new CreateGameResponse(
                session.getId(),
                session.getCategoryName(),
                settings.getPlayerCount(),
                settings.getImposterCount(),
                settings.isHintEnabled(),
                order,
                session.getTurnOrder().size());

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/{gameId}/reveal/{position}")
    public RevealResponse reveal(@PathVariable String gameId, @PathVariable int position) {
        RevealOutcome o = gameService.reveal(gameId, position);
        return new RevealResponse(o.position(), o.playerName(), o.imposter(),
                o.word(), o.hint(), o.speaksFirst(), o.allRevealed());
    }

    @PostMapping("/{gameId}/end")
    public EndGameResponse endGame(@PathVariable String gameId, @RequestBody EndGameRequest request) {
        GameSession session = gameService.endGame(gameId, request.imposterCaught());
        return new EndGameResponse(
                gameId,
                session.getSecretWord(),
                gameService.imposterNames(session),
                request.imposterCaught());
    }
}
