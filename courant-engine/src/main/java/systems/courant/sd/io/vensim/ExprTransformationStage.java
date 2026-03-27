package systems.courant.sd.io.vensim;

/**
 * A single stage in the Vensim expression transformation pipeline.
 *
 * <p>Each stage reads and modifies the {@link TranslationContext#expression()},
 * and may append to the context's warnings and lookups lists.
 */
@FunctionalInterface
interface ExprTransformationStage {

    /**
     * Applies this transformation stage to the context.
     *
     * @param ctx the mutable translation context
     */
    void apply(TranslationContext ctx);
}
