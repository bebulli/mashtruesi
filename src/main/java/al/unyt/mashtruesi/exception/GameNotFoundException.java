package al.unyt.mashtruesi.exception;

public class GameNotFoundException extends GameException {
    public GameNotFoundException(String gameId) {
        super("Nuk u gjet loja me id: " + gameId);
    }
}
