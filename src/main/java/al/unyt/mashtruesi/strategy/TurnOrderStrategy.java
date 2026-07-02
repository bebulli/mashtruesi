package al.unyt.mashtruesi.strategy;

import al.unyt.mashtruesi.model.PlayerAssignment;

import java.util.List;
import java.util.Random;

public interface TurnOrderStrategy {
    List<PlayerAssignment> order(List<PlayerAssignment> assignments, Random random);

    String name();
}
