package al.unyt.mashtruesi.web;

import al.unyt.mashtruesi.dao.GameHistoryDao;
import al.unyt.mashtruesi.dao.GameHistoryRow;
import al.unyt.mashtruesi.exception.DataAccessException;
import al.unyt.mashtruesi.exception.GameException;
import al.unyt.mashtruesi.service.GameService;
import al.unyt.mashtruesi.web.dto.StatsResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class StatsController {
    private final GameService gameService;
    private final GameHistoryDao gameHistoryDao;

    public StatsController(GameService gameService, GameHistoryDao gameHistoryDao) {
        this.gameService = gameService;
        this.gameHistoryDao = gameHistoryDao;
    }

    @GetMapping("/api/stats")
    public StatsResponse stats() {
        try {
            Map<String, Integer> byCategory = gameHistoryDao.countByCategory();
            List<GameHistoryRow> recent = gameHistoryDao.findRecent(10);
            return new StatsResponse(
                    gameService.activeGameCount(),
                    gameService.totalGamesCreated(),
                    byCategory,
                    recent);
        } catch (DataAccessException ex) {
            throw new GameException("Nuk u lexuan dot statistikat.", ex);
        }
    }

    @DeleteMapping("/api/stats")
    public Map<String, Integer> clear() {
        try {
            int removed = gameHistoryDao.deleteAll();
            return Map.of("deleted", removed);
        } catch (DataAccessException ex) {
            throw new GameException("Nuk u fshi dot historiku.", ex);
        }
    }
}
