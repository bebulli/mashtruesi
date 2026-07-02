package al.unyt.mashtruesi.strategy;

import al.unyt.mashtruesi.model.PlayerAssignment;
import al.unyt.mashtruesi.util.WeightedRandomPicker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class WeightedFirstPlayerStrategy implements TurnOrderStrategy {
    private static final double CREW_WEIGHT = 1.0;

    private final double imposterFirstWeight;

    public WeightedFirstPlayerStrategy(double imposterFirstWeight) {
        if (imposterFirstWeight <= 0.0 || imposterFirstWeight > 1.0) {
            throw new IllegalArgumentException("imposterFirstWeight must be in (0, 1].");
        }
        this.imposterFirstWeight = imposterFirstWeight;
    }

    @Override
    public List<PlayerAssignment> order(List<PlayerAssignment> assignments, Random random) {
        WeightedRandomPicker<PlayerAssignment> picker = new WeightedRandomPicker<>(
                assignments,
                pa -> pa.isImposter() ? imposterFirstWeight : CREW_WEIGHT);

        PlayerAssignment first = picker.pick(random);

        List<PlayerAssignment> rest = new ArrayList<>(assignments);
        rest.remove(first);
        Collections.shuffle(rest, random);

        List<PlayerAssignment> ordered = new ArrayList<>(assignments.size());
        ordered.add(first);
        ordered.addAll(rest);

        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setTurnPosition(i);
        }
        return ordered;
    }

    @Override
    public String name() {
        return "weighted-first(w=" + imposterFirstWeight + ")";
    }
}
