package al.unyt.mashtruesi.strategy;

import al.unyt.mashtruesi.model.PlayerAssignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class UniformTurnOrderStrategy implements TurnOrderStrategy {
    @Override
    public List<PlayerAssignment> order(List<PlayerAssignment> assignments, Random random) {
        List<PlayerAssignment> ordered = new ArrayList<>(assignments);
        Collections.shuffle(ordered, random);
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setTurnPosition(i);
        }
        return ordered;
    }

    @Override
    public String name() {
        return "uniform";
    }
}
