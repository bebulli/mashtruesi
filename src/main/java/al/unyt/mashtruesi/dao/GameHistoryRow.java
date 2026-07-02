package al.unyt.mashtruesi.dao;

import java.time.Instant;

public record GameHistoryRow(
        String gameId,
        String category,
        int playerCount,
        int imposterCount,
        String secretWord,
        boolean ended,
        Boolean imposterCaught,
        Instant createdAt) {
}
