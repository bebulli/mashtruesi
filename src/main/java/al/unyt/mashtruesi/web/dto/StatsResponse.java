package al.unyt.mashtruesi.web.dto;

import al.unyt.mashtruesi.dao.GameHistoryRow;

import java.util.List;
import java.util.Map;

public record StatsResponse(
        int activeGames,
        long totalGames,
        Map<String, Integer> playsByCategory,
        List<GameHistoryRow> recentGames) {
}
