package systems.courant.sd.io.vensim;

import java.util.List;

/**
 * Ordered pipeline of transformation stages for translating Vensim expressions
 * to Courant syntax.
 *
 * <p>The default pipeline runs four stages:
 * <ol>
 *   <li>{@link NameNormalizationStage} — quoted names, WITH LOOKUP, multi-word names</li>
 *   <li>{@link OperatorTransformationStage} — IF THEN ELSE, logical ops, comparison ops, power</li>
 *   <li>{@link FunctionTranslationStage} — XIDZ/ZIDZ, SMOOTH/DELAY/RANDOM, pass-throughs, GET functions</li>
 *   <li>{@link SubscriptAndCleanupStage} — Time variable, vector functions, subscripts, lookup calls</li>
 * </ol>
 * followed by a final consecutive-identifier cleanup pass.
 */
final class VensimExprTransformationPipeline {

    private final NameNormalizationStage nameStage;
    private final List<ExprTransformationStage> stages;

    private VensimExprTransformationPipeline(NameNormalizationStage nameStage,
                                              List<ExprTransformationStage> stages) {
        this.nameStage = nameStage;
        this.stages = List.copyOf(stages);
    }

    static VensimExprTransformationPipeline defaultPipeline() {
        NameNormalizationStage nameStage = new NameNormalizationStage();
        return new VensimExprTransformationPipeline(nameStage, List.of(
                nameStage,
                new OperatorTransformationStage(),
                new FunctionTranslationStage(),
                new SubscriptAndCleanupStage()
        ));
    }

    void transform(TranslationContext ctx) {
        for (ExprTransformationStage stage : stages) {
            stage.apply(ctx);
        }
        // Final cleanup: underscore consecutive identifiers (must run after all stages)
        nameStage.applyFinalCleanup(ctx);
    }
}
