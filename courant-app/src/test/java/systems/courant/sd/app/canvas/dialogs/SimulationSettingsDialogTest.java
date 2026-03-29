package systems.courant.sd.app.canvas.dialogs;

import systems.courant.sd.Simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SimulationSettingsDialog (unit tests)")
class SimulationSettingsDialogTest {

    @Nested
    @DisplayName("estimateSteps")
    class EstimateSteps {

        @Test
        @DisplayName("matching units with dt=1 returns duration as step count")
        void shouldReturnDuration_whenUnitsMatchAndDtIsOne() {
            long steps = SimulationSettingsDialog.estimateSteps("Day", 100, "Day", 1.0);
            assertThat(steps).isEqualTo(100);
        }

        @Test
        @DisplayName("Minute step with Day duration produces 1440 steps per day")
        void shouldComputeCorrectSteps_whenMinuteStepDayDuration() {
            // 1 day = 86400 seconds, 1 minute = 60 seconds → 86400/60 = 1440 steps per day
            long steps = SimulationSettingsDialog.estimateSteps("Minute", 1, "Day", 1.0);
            assertThat(steps).isEqualTo(1440);
        }

        @Test
        @DisplayName("Second step with Day duration produces 86400 steps per day")
        void shouldComputeCorrectSteps_whenSecondStepDayDuration() {
            long steps = SimulationSettingsDialog.estimateSteps("Second", 1, "Day", 1.0);
            assertThat(steps).isEqualTo(86400);
        }

        @Test
        @DisplayName("fractional dt multiplies step count")
        void shouldMultiplySteps_whenDtIsFractional() {
            // 100 days with dt=0.25 → 100/0.25 = 400 steps
            long steps = SimulationSettingsDialog.estimateSteps("Day", 100, "Day", 0.25);
            assertThat(steps).isEqualTo(400);
        }

        @Test
        @DisplayName("Year step with Year duration")
        void shouldComputeCorrectSteps_whenYearStepYearDuration() {
            long steps = SimulationSettingsDialog.estimateSteps("Year", 50, "Year", 1.0);
            assertThat(steps).isEqualTo(50);
        }

        @Test
        @DisplayName("Hour step with Week duration")
        void shouldComputeCorrectSteps_whenHourStepWeekDuration() {
            // 1 week = 604800s, 1 hour = 3600s → 604800/3600 = 168 steps per week
            long steps = SimulationSettingsDialog.estimateSteps("Hour", 1, "Week", 1.0);
            assertThat(steps).isEqualTo(168);
        }

        @Test
        @DisplayName("Minute step with 100 Day duration exceeds warning threshold")
        void shouldExceedWarningThreshold_whenMinuteStepHundredDayDuration() {
            // 100 * 86400 / 60 = 144,000 > 100,000
            long steps = SimulationSettingsDialog.estimateSteps("Minute", 100, "Day", 1.0);
            assertThat(steps).isGreaterThan(SimulationSettingsDialog.WARNING_THRESHOLD);
        }

        @Test
        @DisplayName("Second step with 200 Day duration exceeds MAX_STEPS")
        void shouldExceedMaxSteps_whenSecondStepLargeDayDuration() {
            // 200 * 86400 = 17,280,000 > 10,000,000
            long steps = SimulationSettingsDialog.estimateSteps("Second", 200, "Day", 1.0);
            assertThat(steps).isGreaterThan(Simulation.MAX_STEPS);
        }
    }
}
