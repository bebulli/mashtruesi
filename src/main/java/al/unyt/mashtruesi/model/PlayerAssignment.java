package al.unyt.mashtruesi.model;

public class PlayerAssignment {
    private final String playerName;
    private final Role role;

    private int turnPosition;

    private boolean revealed;

    public PlayerAssignment(String playerName, Role role) {
        this.playerName = playerName;
        this.role = role;
    }

    public boolean isImposter() {
        return role == Role.IMPOSTER;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Role getRole() {
        return role;
    }

    public int getTurnPosition() {
        return turnPosition;
    }

    public void setTurnPosition(int turnPosition) {
        this.turnPosition = turnPosition;
    }

    public boolean isRevealed() {
        return revealed;
    }

    public void setRevealed(boolean revealed) {
        this.revealed = revealed;
    }

    @Override
    public String toString() {
        return "PlayerAssignment{name=" + playerName + ", role=" + role
                + ", turn=" + turnPosition + '}';
    }
}
