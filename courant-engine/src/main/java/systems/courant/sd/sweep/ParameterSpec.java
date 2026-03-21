package systems.courant.sd.sweep;

/**
 * Describes a varied parameter in a Monte Carlo simulation, capturing the
 * distribution type and its parameters so that results can be understood
 * and reproduced.
 *
 * @param name             the parameter name
 * @param distributionType human-readable distribution type (e.g. "Normal", "Uniform")
 * @param param1           first distribution parameter (mean or min)
 * @param param2           second distribution parameter (std dev or max)
 * @param param1Label      label for param1 (e.g. "Mean", "Min")
 * @param param2Label      label for param2 (e.g. "Std Dev", "Max")
 */
public record ParameterSpec(
        String name,
        String distributionType,
        double param1,
        double param2,
        String param1Label,
        String param2Label) {
}
