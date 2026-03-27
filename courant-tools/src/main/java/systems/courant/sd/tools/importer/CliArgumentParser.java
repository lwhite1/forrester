package systems.courant.sd.tools.importer;

/**
 * Shared CLI argument parsing utilities for importer command-line tools.
 */
public final class CliArgumentParser {

    private CliArgumentParser() {}

    /**
     * Returns the value for the flag at {@code args[flagIndex]}, which is expected
     * to be at {@code args[flagIndex + 1]}.
     *
     * @param args      the full argument array
     * @param flagIndex the index of the flag that requires a value
     * @return the value immediately following the flag
     * @throws IllegalArgumentException if no value follows the flag
     */
    public static String requireValue(String[] args, int flagIndex) {
        int valueIndex = flagIndex + 1;
        if (valueIndex >= args.length) {
            throw new IllegalArgumentException(
                    args[flagIndex] + " requires a value");
        }
        return args[valueIndex];
    }
}
