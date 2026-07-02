package al.unyt.mashtruesi.web.dto;

import java.util.List;

public record CreateGameRequest(
        List<String> players,
        String category,
        Integer imposterCount,
        Boolean hintEnabled,
        Double imposterFirstWeight) {
}
