package al.unyt.mashtruesi.service;

import al.unyt.mashtruesi.dao.GameHistoryDao;
import al.unyt.mashtruesi.dao.GameHistoryRow;
import al.unyt.mashtruesi.exception.DataAccessException;
import al.unyt.mashtruesi.exception.GameException;
import al.unyt.mashtruesi.exception.GameNotFoundException;
import al.unyt.mashtruesi.model.GameSession;
import al.unyt.mashtruesi.model.GameSettings;
import al.unyt.mashtruesi.model.PlayerAssignment;
import al.unyt.mashtruesi.model.RevealOutcome;
import al.unyt.mashtruesi.model.Role;
import al.unyt.mashtruesi.model.Word;
import al.unyt.mashtruesi.strategy.TurnOrderStrategy;
import al.unyt.mashtruesi.strategy.UniformTurnOrderStrategy;
import al.unyt.mashtruesi.strategy.WeightedFirstPlayerStrategy;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class GameService {
    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    private final Map<String, GameSession> activeSessions = new ConcurrentHashMap<>();

    private final AtomicLong gamesCreated = new AtomicLong(0);

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    private final WordService wordService;
    private final GameHistoryDao gameHistoryDao;

    public GameService(WordService wordService, GameHistoryDao gameHistoryDao) {
        this.wordService = wordService;
        this.gameHistoryDao = gameHistoryDao;
    }

    public GameSession createGame(GameSettings settings) {
        Random random = new Random();

        CompletableFuture<Word> wordFuture =
                CompletableFuture.supplyAsync(
                        () -> wordService.pickRandomWord(settings.getCategory(), random), executor);

        CompletableFuture<List<PlayerAssignment>> orderFuture =
                CompletableFuture.supplyAsync(
                        () -> assignRolesAndOrder(settings, random), executor);

        GameSession session;
        try {
            session = wordFuture.thenCombine(orderFuture, (word, turnOrder) -> {
                String id = "G-" + gamesCreated.incrementAndGet() + "-"
                        + UUID.randomUUID().toString().substring(0, 8);
                String hint = settings.isHintEnabled() ? word.getHint() : null;
                return new GameSession(id, settings, settings.getCategory(),
                        word.getText(), hint, turnOrder);
            }).join();
        } catch (CompletionException ce) {
            Throwable cause = ce.getCause();
            if (cause instanceof GameException ge) {
                throw ge;
            }
            throw new GameException("Deshtoi krijimi i lojes (game setup failed).", cause);
        }

        activeSessions.put(session.getId(), session);
        recordNewGame(session);
        log.info("Created game {} (category={}, players={}, imposters={}, strategy bias weight={})",
                session.getId(), settings.getCategory(), settings.getPlayerCount(),
                settings.getImposterCount(), settings.getImposterFirstWeight());
        return session;
    }

    private List<PlayerAssignment> assignRolesAndOrder(GameSettings settings, Random random) {
        List<String> names = settings.getPlayerNames();
        int n = names.size();
        int imposters = settings.getImposterCount();

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            indices.add(i);
        }
        Collections.shuffle(indices, random);
        Set<Integer> imposterIndices = new HashSet<>(indices.subList(0, imposters));

        List<PlayerAssignment> assignments = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Role role = imposterIndices.contains(i) ? Role.IMPOSTER : Role.CREW;
            assignments.add(new PlayerAssignment(names.get(i), role));
        }

        TurnOrderStrategy strategy = (settings.getImposterFirstWeight() < 1.0)
                ? new WeightedFirstPlayerStrategy(settings.getImposterFirstWeight())
                : new UniformTurnOrderStrategy();

        return strategy.order(assignments, random);
    }

    public RevealOutcome reveal(String gameId, int position) {
        GameSession session = requireSession(gameId);
        List<PlayerAssignment> order = session.getTurnOrder();
        if (position < 0 || position >= order.size()) {
            throw new GameException("Pozicion i pavlefshem: " + position);
        }

        PlayerAssignment pa = order.get(position);
        synchronized (session) {
            if (!pa.isRevealed()) {
                pa.setRevealed(true);
                session.markRevealed();
            }
        }

        boolean imposter = pa.isImposter();
        String word = imposter ? null : session.getSecretWord();
        String hint = imposter ? session.getImposterHint() : null;
        return new RevealOutcome(
                position, pa.getPlayerName(), imposter, word, hint,
                position == 0, session.allRevealed());
    }

    public GameSession endGame(String gameId, boolean imposterCaught) {
        GameSession session = requireSession(gameId);
        session.setEnded(true);
        activeSessions.remove(gameId);
        try {
            gameHistoryDao.markEnded(gameId, imposterCaught);
        } catch (DataAccessException e) {
            log.warn("Could not update history for game {}: {}", gameId, e.getMessage());
        }
        return session;
    }

    public List<String> imposterNames(GameSession session) {
        return session.getTurnOrder().stream()
                .filter(PlayerAssignment::isImposter)
                .map(PlayerAssignment::getPlayerName)
                .toList();
    }

    public GameSession requireSession(String gameId) {
        GameSession session = activeSessions.get(gameId);
        if (session == null) {
            throw new GameNotFoundException(gameId);
        }
        return session;
    }

    public int activeGameCount() {
        return activeSessions.size();
    }

    public long totalGamesCreated() {
        return gamesCreated.get();
    }

    private void recordNewGame(GameSession session) {
        GameSettings s = session.getSettings();
        GameHistoryRow row = new GameHistoryRow(
                session.getId(), s.getCategory(), s.getPlayerCount(),
                s.getImposterCount(), session.getSecretWord(),
                false, null, Instant.now());
        try {
            gameHistoryDao.insert(row);
        } catch (DataAccessException e) {
            log.warn("Could not record new game {}: {}", session.getId(), e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        log.info("GameService executor shut down.");
    }
}
