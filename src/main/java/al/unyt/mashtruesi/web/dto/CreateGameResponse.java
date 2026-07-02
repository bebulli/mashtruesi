package al.unyt.mashtruesi.web.dto;

import java.util.List;

public record CreateGameResponse(
        String gameId,
        String category,
        int playerCount,
        int imposterCount,
        boolean hintEnabled,
        List<String> turnOrder,
        int totalToReveal) {
}
