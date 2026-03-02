package com.deathrayresearch.forrester.model.def;

import java.util.Arrays;
import java.util.List;

/**
 * Shared utility for comparing lists of coordinate point arrays.
 */
final class PointListUtil {

    private PointListUtil() {
    }

    static boolean pointListEquals(List<double[]> a, List<double[]> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!Arrays.equals(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }
}
