package su.starlight.da.util;

import java.util.Collection;
import java.util.Random;

public final class RandomUtil {

    private RandomUtil() {}

    public static double nextDouble(double base, double bound) {
        return base + (new Random().nextDouble() * (bound - base));
    }

    public static double nextDouble() {
        return new Random().nextDouble();
    }

    public static boolean nextBoolean(double chance) {
        return new Random().nextDouble() > chance;
    }

    public static int nextInt(int bound) {
        return new Random().nextInt(bound);
    }

    public static <E> E chooseRandomly(Collection<E> collection) {
        return chooseRandomly((E[]) collection.toArray());
    }

    @SafeVarargs
    public static <E> E chooseRandomly(E... es) {
        if(es.length == 0) throw new IllegalArgumentException("There must be at least one object");
        else if(es.length == 1) return es[0];
        return es[new Random().nextInt(es.length)];
    }

}
