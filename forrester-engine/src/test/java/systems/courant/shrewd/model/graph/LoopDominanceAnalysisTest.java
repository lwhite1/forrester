package systems.courant.forrester.model.graph;

import systems.courant.forrester.model.def.CausalLinkDef;
import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.ModelDefinitionBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoopDominanceAnalysis")
class LoopDominanceAnalysisTest {

    @Nested
    @DisplayName("compute")
    class Compute {

        @Test
        void shouldReturnNullForNoLoops() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("No Loops")
                    .stock("S", 100, "Thing")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);
            List<String> columns = List.of("Step", "S");
            List<double[]> rows = List.of(
                    new double[]{0, 100},
                    new double[]{1, 90});

            LoopDominanceAnalysis result = LoopDominanceAnalysis.compute(columns, rows, analysis);

            assertThat(result).isNull();
        }

        @Test
        void shouldReturnNullForTooFewSteps() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("PP")
                    .stock("Prey", 100, "Animal")
                    .stock("Predators", 20, "Animal")
                    .flow("Predation", "Prey * Predators * 0.01", "Day", "Prey", "Predators")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);
            List<String> columns = List.of("Step", "Prey", "Predators");
            List<double[]> rows = List.of(new double[]{0, 100, 20});

            LoopDominanceAnalysis result = LoopDominanceAnalysis.compute(columns, rows, analysis);

            assertThat(result).isNull();
        }

        @Test
        void shouldComputeActivityForSingleLoop() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("PP")
                    .stock("Prey", 100, "Animal")
                    .stock("Predators", 20, "Animal")
                    .flow("Predation", "Prey * Predators * 0.01", "Day", "Prey", "Predators")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);
            List<String> columns = List.of("Step", "Prey", "Predators");
            List<double[]> rows = List.of(
                    new double[]{0, 100, 20},
                    new double[]{1, 80, 40},
                    new double[]{2, 70, 50});

            LoopDominanceAnalysis result = LoopDominanceAnalysis.compute(columns, rows, analysis);

            assertThat(result).isNotNull();
            assertThat(result.loopCount()).isEqualTo(1);
            assertThat(result.stepCount()).isEqualTo(3);

            // Step 0: no delta
            assertThat(result.score(0, 0)).isEqualTo(0);

            // Step 1: |80-100| + |40-20| = 20 + 20 = 40
            assertThat(result.score(0, 1)).isEqualTo(40.0);

            // Step 2: |70-80| + |50-40| = 10 + 10 = 20
            assertThat(result.score(0, 2)).isEqualTo(20.0);
        }

        @Test
        void shouldIdentifyDominantLoop() {
            // Two independent loops, one changing faster
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Two Loops")
                    .stock("A", 100, "Thing")
                    .stock("B", 50, "Thing")
                    .stock("C", 200, "Thing")
                    .stock("D", 30, "Thing")
                    .flow("F1", "A * B * 0.01", "Day", "A", "B")
                    .flow("F2", "C * D * 0.02", "Day", "C", "D")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);
            assertThat(analysis.loopCount()).isEqualTo(2);

            // Simulate: loop 1 (A,B) changes by 10, loop 2 (C,D) changes by 50
            List<String> columns = List.of("Step", "A", "B", "C", "D");
            List<double[]> rows = List.of(
                    new double[]{0, 100, 50, 200, 30},
                    new double[]{1, 95, 55, 150, 80});

            LoopDominanceAnalysis result = LoopDominanceAnalysis.compute(columns, rows, analysis);

            assertThat(result).isNotNull();
            assertThat(result.loopCount()).isEqualTo(2);

            int dominant = result.dominantLoopAt(1);
            // Loop 2 (C,D) has more activity: |150-200|+|80-30| = 100 vs |95-100|+|55-50| = 10
            assertThat(result.score(dominant, 1)).isGreaterThan(result.score(1 - dominant, 1));
        }

        @Test
        void shouldReturnMinusOneForZeroActivity() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("PP")
                    .stock("Prey", 100, "Animal")
                    .stock("Predators", 20, "Animal")
                    .flow("Predation", "Prey * Predators * 0.01", "Day", "Prey", "Predators")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);
            List<String> columns = List.of("Step", "Prey", "Predators");
            // No change between steps
            List<double[]> rows = List.of(
                    new double[]{0, 100, 20},
                    new double[]{1, 100, 20});

            LoopDominanceAnalysis result = LoopDominanceAnalysis.compute(columns, rows, analysis);
            assertThat(result).isNotNull();
            assertThat(result.dominantLoopAt(1)).isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("normalized scores")
    class NormalizedScores {

        @Test
        void shouldSumToOne() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Two Loops")
                    .stock("A", 100, "Thing")
                    .stock("B", 50, "Thing")
                    .stock("C", 200, "Thing")
                    .stock("D", 30, "Thing")
                    .flow("F1", "A * B * 0.01", "Day", "A", "B")
                    .flow("F2", "C * D * 0.02", "Day", "C", "D")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);
            List<String> columns = List.of("Step", "A", "B", "C", "D");
            List<double[]> rows = List.of(
                    new double[]{0, 100, 50, 200, 30},
                    new double[]{1, 95, 55, 150, 80});

            LoopDominanceAnalysis result = LoopDominanceAnalysis.compute(columns, rows, analysis);
            assertThat(result).isNotNull();

            double[] scores = result.normalizedScoresAt(1);
            double sum = 0;
            for (double s : scores) {
                sum += s;
                assertThat(s).isBetween(0.0, 1.0);
            }
            assertThat(sum).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.001));
        }

        @Test
        void shouldReturnZerosForNoActivity() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("PP")
                    .stock("Prey", 100, "Animal")
                    .stock("Predators", 20, "Animal")
                    .flow("Predation", "Prey * Predators * 0.01", "Day", "Prey", "Predators")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);
            List<String> columns = List.of("Step", "Prey", "Predators");
            List<double[]> rows = List.of(
                    new double[]{0, 100, 20},
                    new double[]{1, 100, 20});

            LoopDominanceAnalysis result = LoopDominanceAnalysis.compute(columns, rows, analysis);
            double[] scores = result.normalizedScoresAt(1);
            for (double s : scores) {
                assertThat(s).isEqualTo(0.0);
            }
        }
    }

    @Nested
    @DisplayName("CLD loops")
    class CldLoops {

        @Test
        void shouldComputeActivityForCldLoopsWithMatchingColumns() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("CLD")
                    .cldVariable("X")
                    .cldVariable("Y")
                    .causalLink("X", "Y", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("Y", "X", CausalLinkDef.Polarity.POSITIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            // Simulate X and Y as columns (as if they were tracked variables)
            List<String> columns = List.of("Step", "X", "Y");
            List<double[]> rows = List.of(
                    new double[]{0, 10, 5},
                    new double[]{1, 15, 8},
                    new double[]{2, 22, 12});

            LoopDominanceAnalysis result = LoopDominanceAnalysis.compute(columns, rows, analysis);

            assertThat(result).isNotNull();
            assertThat(result.loopCount()).isGreaterThan(0);
            assertThat(result.score(0, 1)).isGreaterThan(0);
        }

        @Test
        void shouldReturnNullWhenNoColumnsMatchCldVariables() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("CLD")
                    .cldVariable("X")
                    .cldVariable("Y")
                    .causalLink("X", "Y", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("Y", "X", CausalLinkDef.Polarity.POSITIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            // No matching columns
            List<String> columns = List.of("Step", "Unrelated");
            List<double[]> rows = List.of(
                    new double[]{0, 10},
                    new double[]{1, 15});

            LoopDominanceAnalysis result = LoopDominanceAnalysis.compute(columns, rows, analysis);

            assertThat(result).isNull();
        }
    }
}
