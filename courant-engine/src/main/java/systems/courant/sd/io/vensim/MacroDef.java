package systems.courant.sd.io.vensim;

import java.util.List;

/**
 * A parsed Vensim macro definition ({@code :MACRO:} to {@code :END OF MACRO:}).
 *
 * <p>Vensim macros define reusable sub-models with local variables and parameters.
 * The last parameter(s) that appear as LHS names in the body are outputs; the rest
 * are inputs.
 *
 * @param name the macro name (original Vensim form, may contain spaces)
 * @param inputParams formal input parameter names (original Vensim form)
 * @param outputParams formal output parameter names (original Vensim form)
 * @param bodyEquations the equations inside the macro body
 */
public record MacroDef(
        String name,
        List<String> inputParams,
        List<String> outputParams,
        List<MdlEquation> bodyEquations
) {

    public MacroDef {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Macro name must not be blank");
        }
        inputParams = List.copyOf(inputParams);
        outputParams = List.copyOf(outputParams);
        bodyEquations = List.copyOf(bodyEquations);
    }
}
