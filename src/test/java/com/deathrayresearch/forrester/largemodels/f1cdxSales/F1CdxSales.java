package com.deathrayresearch.forrester.largemodels.f1cdxSales;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.item.People;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.rate.RatePerYear;
import com.deathrayresearch.forrester.ui.ChartViewer;
import org.junit.Test;

import static com.deathrayresearch.forrester.measure.Units.YEAR;

/**
 *
 */
public class F1CdxSales {

    private static final People PEOPLE = People.getInstance();

    private static final int CANCER_PATIENT_STARTING_POPULATION = 750_000;
    private static final int CGX_PATIENT_STARTING_POPULATION = 300_000;
    private static final int NGS_CANCER_PATIENT_STARTING_POPULATION = 150_000;

    private static final float CGx_TEST_RATE = 0.30f;
    private static final float NGS_TEST_RATE = 0.20f;

    private static final float AVERAGE_PRICE_PHARMA = 4_000f;
    private static final float AVERAGE_PRICE_CLINICAL = 3_000f;

    private static final float PAY_RATE_CLINICAL = 0.35f;

    private static final float FMI_NGS_MARKET_SHARE = 0.50f;

    private static final float CANCER_POPULATION_GROWTH_RATE = 0.02f;


    @Test
    public void testRun1() {

        Model model = new Model("F1 Cdx Sales");

        Stock cancerPatients = new Stock("Cancer Patients", CANCER_PATIENT_STARTING_POPULATION, PEOPLE);

        Stock cgxTestedPatients = new Stock("CGx-tested Cancer Patients", CGX_PATIENT_STARTING_POPULATION, PEOPLE);

        Stock ngsTestedPatients = new Stock("NGS-tested Cancer Patients", NGS_CANCER_PATIENT_STARTING_POPULATION, PEOPLE);

        Flow newCancerPatients = new Flow( new RatePerYear() {
            @Override
            protected Quantity quantityPerYear() {
                return new Quantity("New Cancer Patients",750_000 * (1 + CANCER_POPULATION_GROWTH_RATE), PEOPLE);
            }
        });

        Flow testFlow = new Flow(new RatePerYear() {
            @Override
            protected Quantity quantityPerYear() {
                return new Quantity("CGx Tests",CANCER_PATIENT_STARTING_POPULATION * CGx_TEST_RATE, PEOPLE);
            }
        });

        Flow ngsTestFlow = new Flow(new RatePerYear() {
            @Override
            protected Quantity quantityPerYear() {
                return new Quantity("NGS Tests",CGX_PATIENT_STARTING_POPULATION * NGS_TEST_RATE, PEOPLE);
            }
        });

        cancerPatients.addInflow(newCancerPatients);
        cancerPatients.addOutflow(testFlow);

        cgxTestedPatients.addInflow(testFlow);
        cgxTestedPatients.addOutflow(ngsTestFlow);

        ngsTestedPatients.addInflow(ngsTestFlow);

        model.addStock(cancerPatients);
        model.addStock(cgxTestedPatients);
        model.addStock(ngsTestedPatients);
       // model.addStock(F1CdxTestedPatients);

        Simulation run = new Simulation(model, Day.getInstance(), YEAR,3);
        run.addEventHandler(new ChartViewer());
        run.execute();
    }

}
