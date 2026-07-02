package al.unyt.mashtruesi.model;

import al.unyt.mashtruesi.exception.InvalidSettingsException;
import al.unyt.mashtruesi.exception.NotEnoughPlayersException;

import java.util.ArrayList;
import java.util.List;

public final class GameSettings {
    public static final int MIN_PLAYERS = 4;

    public static final int TWO_IMPOSTER_THRESHOLD = 8;

    private final List<String> playerNames;
    private final String category;
    private final int imposterCount;
    private final boolean hintEnabled;
    private final double imposterFirstWeight;

    private GameSettings(Builder b) {
        this.playerNames = List.copyOf(b.playerNames);
        this.category = b.category;
        this.imposterCount = b.imposterCount;
        this.hintEnabled = b.hintEnabled;
        this.imposterFirstWeight = b.imposterFirstWeight;
    }

    public List<String> getPlayerNames() {
        return playerNames;
    }

    public String getCategory() {
        return category;
    }

    public int getImposterCount() {
        return imposterCount;
    }

    public boolean isHintEnabled() {
        return hintEnabled;
    }

    public double getImposterFirstWeight() {
        return imposterFirstWeight;
    }

    public int getPlayerCount() {
        return playerNames.size();
    }

    public static int defaultImposterCount(int playerCount) {
        return playerCount >= TWO_IMPOSTER_THRESHOLD ? 2 : 1;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<String> playerNames = new ArrayList<>();
        private String category;
        private Integer imposterCount;
        private boolean hintEnabled = true;
        private double imposterFirstWeight = 0.25;

        private Builder() {
        }

        public Builder players(List<String> names) {
            this.playerNames.clear();
            if (names != null) {
                this.playerNames.addAll(names);
            }
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder imposterCount(Integer count) {
            this.imposterCount = count;
            return this;
        }

        public Builder hintEnabled(boolean enabled) {
            this.hintEnabled = enabled;
            return this;
        }

        public Builder imposterFirstWeight(double weight) {
            this.imposterFirstWeight = weight;
            return this;
        }

        public GameSettings build() {
            if (category == null || category.isBlank()) {
                throw new InvalidSettingsException("Duhet zgjedhur nje kategori (no category selected).");
            }

            List<String> cleaned = new ArrayList<>();
            for (String raw : playerNames) {
                if (raw == null) {
                    continue;
                }
                String name = raw.trim();
                if (name.isEmpty()) {
                    continue;
                }
                boolean duplicate = cleaned.stream().anyMatch(n -> n.equalsIgnoreCase(name));
                if (duplicate) {
                    throw new InvalidSettingsException("Emer i dyfishuar: '" + name + "'.");
                }
                cleaned.add(name);
            }
            this.playerNames.clear();
            this.playerNames.addAll(cleaned);

            int count = cleaned.size();
            if (count < MIN_PLAYERS) {
                throw new NotEnoughPlayersException(
                        "Duhen te pakten " + MIN_PLAYERS + " lojtare (got " + count + ").");
            }

            if (imposterCount == null) {
                imposterCount = defaultImposterCount(count);
            }
            if (imposterCount < 1) {
                throw new InvalidSettingsException("Duhet te kete te pakten 1 mashtrues.");
            }

            if (imposterCount * 2 >= count) {
                throw new InvalidSettingsException(
                        "Numri i mashtruesve (" + imposterCount + ") duhet te jete me pak se gjysma e lojtareve.");
            }

            if (imposterFirstWeight <= 0.0 || imposterFirstWeight > 1.0) {
                throw new InvalidSettingsException(
                        "imposterFirstWeight duhet te jete ne intervalin (0, 1].");
            }

            return new GameSettings(this);
        }
    }
}
