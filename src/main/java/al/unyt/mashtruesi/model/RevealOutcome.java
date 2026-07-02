package al.unyt.mashtruesi.model;

public record RevealOutcome(
        int position,
        String playerName,
        boolean imposter,
        String word,
        String hint,
        boolean speaksFirst,
        boolean allRevealed) {
}
