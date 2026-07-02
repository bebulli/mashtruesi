package al.unyt.mashtruesi.util;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

public final class CollectionUtils {
    private CollectionUtils() {
    }

    public static <T extends Comparable<T>> List<T> sortedDistinct(Collection<T> input) {
        TreeSet<T> sorted = new TreeSet<>(input);
        return List.copyOf(sorted);
    }
}
