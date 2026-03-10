package systems.courant.forrester.model.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Analyzes loop dominance over a simulation run by measuring per-step
 * stock activity for each feedback loop group.
 *
 * <p>For each stock-and-flow loop, the "activity" at each time step is
 * the sum of absolute changes in stock values belonging to that loop.
 * The loop with the highest activity at a given step is considered dominant.
 *
 * <p>This is a heuristic approximation. More rigorous methods (eigenvalue
 * analysis, pathway participation) exist in the SD literature, but this
 * simple measure captures the essential insight of which loop is driving
 * the most change at any given moment.
 */
public record LoopDominanceAnalysis(
        List<String> loopLabels,
        List<FeedbackAnalysis.LoopType> loopTypes,
        int stepCount,
        double[][] activity
) {

    /**
     * Computes loop dominance from simulation result data and loop analysis.
     *
     * @param columnNames  ordered column names from the simulation result (Step, stocks, variables...)
     * @param rows         per-step data rows from the simulation result
     * @param analysis     the feedback loop analysis for the model
     * @return a LoopDominanceAnalysis, or null if there are no analyzable loops
     */
    public static LoopDominanceAnalysis compute(List<String> columnNames,
            List<double[]> rows, FeedbackAnalysis analysis) {
        if (analysis == null || analysis.loopCount() == 0 || rows.size() < 2) {
            return null;
        }

        // Build column index lookup (name → column index in rows)
        Map<String, Integer> colIndex = new LinkedHashMap<>();
        for (int i = 0; i < columnNames.size(); i++) {
            colIndex.put(columnNames.get(i), i);
        }

        // Collect analyzable loops (only S&F groups and CLD loops with stocks in simulation)
        List<String> labels = new ArrayList<>();
        List<FeedbackAnalysis.LoopType> types = new ArrayList<>();
        List<List<Integer>> loopStockColumns = new ArrayList<>();

        int loopCount = analysis.loopCount();
        for (int i = 0; i < loopCount; i++) {
            List<Integer> stockCols = new ArrayList<>();
            FeedbackAnalysis.LoopInfo info = analysis.loopInfo(i).orElse(null);
            if (info == null) {
                continue;
            }

            Set<String> elements = Set.copyOf(info.path());

            for (String name : elements) {
                Integer col = colIndex.get(name);
                if (col != null) {
                    stockCols.add(col);
                }
            }

            if (!stockCols.isEmpty()) {
                labels.add(info.label());
                types.add(info.type());
                loopStockColumns.add(stockCols);
            }
        }

        if (labels.isEmpty()) {
            return null;
        }

        // Compute per-step activity for each loop
        int steps = rows.size();
        double[][] activity = new double[labels.size()][steps];

        for (int loopIdx = 0; loopIdx < labels.size(); loopIdx++) {
            List<Integer> cols = loopStockColumns.get(loopIdx);
            activity[loopIdx][0] = 0; // no delta at step 0
            for (int step = 1; step < steps; step++) {
                double[] prev = rows.get(step - 1);
                double[] curr = rows.get(step);
                double sum = 0;
                for (int col : cols) {
                    sum += Math.abs(curr[col] - prev[col]);
                }
                activity[loopIdx][step] = sum;
            }
        }

        return new LoopDominanceAnalysis(
                Collections.unmodifiableList(labels),
                Collections.unmodifiableList(types),
                steps,
                activity);
    }

    /**
     * Returns the index of the dominant loop at the given step, or -1 if
     * all loops have zero activity.
     */
    public int dominantLoopAt(int step) {
        if (step < 0 || step >= stepCount || activity.length == 0) {
            return -1;
        }
        int maxIdx = -1;
        double maxVal = 0;
        for (int i = 0; i < activity.length; i++) {
            if (activity[i][step] > maxVal) {
                maxVal = activity[i][step];
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    /**
     * Returns the activity score for a specific loop at a specific step.
     */
    public double score(int loopIndex, int step) {
        if (loopIndex < 0 || loopIndex >= activity.length
                || step < 0 || step >= stepCount) {
            return 0;
        }
        return activity[loopIndex][step];
    }

    /**
     * Returns normalized dominance scores (0..1) for all loops at a given step.
     * The scores sum to 1.0 unless all activity is zero (then all scores are 0).
     */
    public double[] normalizedScoresAt(int step) {
        double[] scores = new double[activity.length];
        if (step < 0 || step >= stepCount) {
            return scores;
        }
        double total = 0;
        for (int i = 0; i < activity.length; i++) {
            scores[i] = activity[i][step];
            total += scores[i];
        }
        if (total > 0) {
            for (int i = 0; i < scores.length; i++) {
                scores[i] /= total;
            }
        }
        return scores;
    }

    /**
     * Returns the number of analyzable loops.
     */
    public int loopCount() {
        return loopLabels.size();
    }
}
