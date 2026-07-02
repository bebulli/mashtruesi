package al.unyt.mashtruesi.web.dto;

import java.util.List;

public record EndGameResponse(
        String gameId,
        String secretWord,
        List<String> imposterNames,
        boolean imposterCaught) {
}
