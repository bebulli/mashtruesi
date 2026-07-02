package al.unyt.mashtruesi.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

public class WeightedRandomPicker<T> {
    private final List<T> items;
    private final Function<? super T, Double> weightFunction;

    public WeightedRandomPicker(Collection<? extends T> items,
                                Function<? super T, Double> weightFunction) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("WeightedRandomPicker requires a non-empty collection.");
        }
        if (weightFunction == null) {
            throw new IllegalArgumentException("weightFunction must not be null.");
        }
        this.items = new ArrayList<>(items);
        this.weightFunction = weightFunction;
    }

    public T pick(Random random) {
        double total = 0.0;
        for (T item : items) {
            double w = weightFunction.apply(item);
            if (w < 0) {
                throw new IllegalStateException("Negative weight for element: " + item);
            }
            total += w;
        }
        if (total <= 0.0) {
            return items.get(random.nextInt(items.size()));
        }

        double roll = random.nextDouble() * total;
        double running = 0.0;
        for (T item : items) {
            running += weightFunction.apply(item);
            if (roll < running) {
                return item;
            }
        }

        return items.get(items.size() - 1);
    }
}
