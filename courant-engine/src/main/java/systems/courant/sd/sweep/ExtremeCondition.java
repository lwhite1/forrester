package systems.courant.sd.sweep;

/**
 * Defines the extreme conditions applied to parameters during extreme-condition testing.
 * Each condition transforms a baseline parameter value into an extreme value designed
 * to stress-test the model.
 */
public enum ExtremeCondition {

    /**
     * Sets the parameter to zero.
     */
    ZERO {
        @Override
        public double apply(double baseline) {
            return 0.0;
        }

        @Override
        public String label() {
            return "Zero";
        }
    },

    /**
     * Sets the parameter to 10x its baseline value.
     * If baseline is zero, uses 10.0 instead.
     */
    TEN_X {
        @Override
        public double apply(double baseline) {
            if (baseline == 0.0) {
                return 10.0;
            }
            return baseline * 10.0;
        }

        @Override
        public String label() {
            return "10x";
        }
    },

    /**
     * Negates the parameter value. If baseline is positive, returns negative (and vice versa).
     * If baseline is zero, uses -1.0.
     */
    NEGATIVE {
        @Override
        public double apply(double baseline) {
            if (baseline == 0.0) {
                return -1.0;
            }
            return -baseline;
        }

        @Override
        public String label() {
            return "Negative";
        }
    };

    /**
     * Computes the extreme value for the given baseline parameter value.
     *
     * @param baseline the original parameter value
     * @return the extreme test value
     */
    public abstract double apply(double baseline);

    /**
     * Returns a human-readable label for this condition.
     *
     * @return the display label
     */
    public abstract String label();
}
