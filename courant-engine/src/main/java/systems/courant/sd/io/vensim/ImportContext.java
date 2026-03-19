package systems.courant.sd.io.vensim;

import systems.courant.sd.model.def.ModelDefinitionBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bundles the shared mutable and immutable state threaded through the Vensim import pipeline.
 *
 * <p>This record replaces the 16-parameter data clump that was previously passed to
 * {@code classifyAndBuild}, {@code expandSubscriptedVariable}, and related methods.
 * Fields that are mutated during import (e.g., {@code flowNames}, {@code sketchFlowNames})
 * are mutable collections — the record simply provides a single handle for passing them.
 *
 * @param builder              the model definition builder accumulating results
 * @param vensimNames          all raw Vensim variable names encountered
 * @param stockNames           normalized names classified as stocks (INTEG)
 * @param flowNames            normalized names consumed as flows (mutated during import)
 * @param lookupNames          normalized names classified as lookup tables (mutated during import)
 * @param sketchFlowNames      flow names resolved from sketch valves (mutated during import)
 * @param sketchValveNames     display names of flow valve elements from sketch lines
 * @param equationsByName      index from normalized name to the original {@link MdlEquation}
 * @param constantValues       known numeric constant values by normalized name
 * @param timeUnit             the inferred simulation time unit (e.g., "Year")
 * @param subscriptDimensions  dimension name to normalized label list
 * @param subscriptDisplayLabels dimension name to display label list
 * @param subscriptMappings    dimension name to {@link VensimImporter.SubscriptMapping}
 * @param cldVariableNames     CLD variable names (mutated during CLD import)
 * @param allNormalizedNames   all normalized names seen so far, for duplicate detection (mutated)
 * @param warnings             accumulated import warnings (mutated during import)
 */
record ImportContext(
        ModelDefinitionBuilder builder,
        Set<String> vensimNames,
        Set<String> stockNames,
        Set<String> flowNames,
        Set<String> lookupNames,
        Set<String> sketchFlowNames,
        Set<String> sketchValveNames,
        Map<String, MdlEquation> equationsByName,
        Map<String, Double> constantValues,
        String timeUnit,
        Map<String, List<String>> subscriptDimensions,
        Map<String, List<String>> subscriptDisplayLabels,
        Map<String, VensimImporter.SubscriptMapping> subscriptMappings,
        Set<String> cldVariableNames,
        Set<String> allNormalizedNames,
        List<String> warnings
) {}
