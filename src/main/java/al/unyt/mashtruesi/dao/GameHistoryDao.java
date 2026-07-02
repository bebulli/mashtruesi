package al.unyt.mashtruesi.dao;

import al.unyt.mashtruesi.exception.DataAccessException;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class GameHistoryDao {
    private final DataSource dataSource;

    public GameHistoryDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void ensureSchema() throws DataAccessException {
        String ddl = """
                CREATE TABLE IF NOT EXISTS game_history (
                    game_id VARCHAR(64) PRIMARY KEY,
                    category VARCHAR(64) NOT NULL,
                    player_count INT NOT NULL,
                    imposter_count INT NOT NULL,
                    secret_word VARCHAR(128) NOT NULL,
                    ended BOOLEAN NOT NULL DEFAULT FALSE,
                    imposter_caught BOOLEAN NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """;
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(ddl);
        } catch (SQLException e) {
            throw new DataAccessException("Could not create game_history table", e);
        }
    }

    public void insert(GameHistoryRow row) throws DataAccessException {
        String sql = """
                INSERT INTO game_history
                    (game_id, category, player_count, imposter_count, secret_word, ended, imposter_caught, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, row.gameId());
            ps.setString(2, row.category());
            ps.setInt(3, row.playerCount());
            ps.setInt(4, row.imposterCount());
            ps.setString(5, row.secretWord());
            ps.setBoolean(6, row.ended());
            if (row.imposterCaught() == null) {
                ps.setNull(7, java.sql.Types.BOOLEAN);
            } else {
                ps.setBoolean(7, row.imposterCaught());
            }
            ps.setTimestamp(8, Timestamp.from(row.createdAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not insert game " + row.gameId(), e);
        }
    }

    public List<GameHistoryRow> findRecent(int limit) throws DataAccessException {
        String sql = """
                SELECT game_id, category, player_count, imposter_count,
                       secret_word, ended, imposter_caught, created_at
                FROM game_history
                ORDER BY created_at DESC
                LIMIT ?
                """;
        List<GameHistoryRow> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read recent games", e);
        }
        return rows;
    }

    public Map<String, Integer> countByCategory() throws DataAccessException {
        String sql = """
                SELECT category, COUNT(*) AS plays
                FROM game_history
                GROUP BY category
                ORDER BY plays DESC, category ASC
                """;
        Map<String, Integer> counts = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                counts.put(rs.getString("category"), rs.getInt("plays"));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not aggregate games by category", e);
        }
        return counts;
    }

    public boolean markEnded(String gameId, boolean imposterCaught) throws DataAccessException {
        String sql = "UPDATE game_history SET ended = TRUE, imposter_caught = ? WHERE game_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, imposterCaught);
            ps.setString(2, gameId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not mark game ended: " + gameId, e);
        }
    }

    public int deleteAll() throws DataAccessException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM game_history")) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not clear game history", e);
        }
    }

    private GameHistoryRow mapRow(ResultSet rs) throws SQLException {
        Object caughtRaw = rs.getObject("imposter_caught");
        Boolean caught = (caughtRaw == null) ? null : rs.getBoolean("imposter_caught");
        Timestamp ts = rs.getTimestamp("created_at");
        Instant createdAt = (ts != null) ? ts.toInstant() : Instant.now();
        return new GameHistoryRow(
                rs.getString("game_id"),
                rs.getString("category"),
                rs.getInt("player_count"),
                rs.getInt("imposter_count"),
                rs.getString("secret_word"),
                rs.getBoolean("ended"),
                caught,
                createdAt);
    }
}
