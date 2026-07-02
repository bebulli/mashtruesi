package al.unyt.mashtruesi.web.dto;

public record RevealResponse(
        int position,
        String playerName,
        boolean imposter,
        String word,
        String hint,
        boolean speaksFirst,
        boolean allRevealed) {
}
