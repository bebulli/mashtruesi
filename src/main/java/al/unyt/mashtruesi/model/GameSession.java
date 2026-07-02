package al.unyt.mashtruesi.model;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GameSession {
    private final String id;
    private final GameSettings settings;
    private final String categoryName;
    private final String secretWord;
    private final String imposterHint;

    private final List<PlayerAssignment> turnOrder;

    private final Instant createdAt;
    private final AtomicInteger revealedCount = new AtomicInteger(0);
    private volatile boolean ended = false;

    public GameSession(String id,
                       GameSettings settings,
                       String categoryName,
                       String secretWord,
                       String imposterHint,
                       List<PlayerAssignment> turnOrder) {
        this.id = id;
        this.settings = settings;
        this.categoryName = categoryName;
        this.secretWord = secretWord;
        this.imposterHint = imposterHint;
        this.turnOrder = turnOrder;
        this.createdAt = Instant.now();
    }

    public int markRevealed() {
        return revealedCount.incrementAndGet();
    }

    public boolean allRevealed() {
        return revealedCount.get() >= turnOrder.size();
    }

    public String getId() {
        return id;
    }

    public GameSettings getSettings() {
        return settings;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getSecretWord() {
        return secretWord;
    }

    public String getImposterHint() {
        return imposterHint;
    }

    public List<PlayerAssignment> getTurnOrder() {
        return turnOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public int getRevealedCount() {
        return revealedCount.get();
    }

    public boolean isEnded() {
        return ended;
    }

    public void setEnded(boolean ended) {
        this.ended = ended;
    }
}
