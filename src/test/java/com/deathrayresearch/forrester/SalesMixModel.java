package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.io.CsvSubscriber;
import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Item;
import com.deathrayresearch.forrester.measure.units.dimensionless.DimensionlessUnit;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.measure.units.time.Week;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Formula;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.rate.RatePerDay;
import com.deathrayresearch.forrester.rate.Rate;
import com.deathrayresearch.forrester.ui.ChartViewer;
import org.junit.Test;

import static com.deathrayresearch.forrester.SalesMixModel.Sales.SALES;
import static com.deathrayresearch.forrester.measure.Units.PEOPLE;
import static com.deathrayresearch.forrester.measure.Units.WEEK;

/**
 *
 */
public class SalesMixModel {

    @Test
    public void testModel() {
        Model model = new Model("Hardware/software sales mix");

        Constant hardwareSalesCustomer = new Constant("Hardware sales per new customer",
                SALES,
                1000);

        Constant serviceSalesCustomerMonth = new Constant("Service sales per customer per month",
                SALES,
                10);

        Stock customers = new Stock("customers", 0, PEOPLE);

        Rate acquisitionRate = new RatePerDay() {
            @Override
            protected Quantity quantityPerDay() {
                return new Quantity("New customers", 10, PEOPLE);
            }
        };

        Formula hardwareSalesFormula = () -> hardwareSalesCustomer.getCurrentValue()
                * acquisitionRate.flowPerTimeUnit(WEEK).getValue();

        Variable hardwareSales = new Variable("Hardware sales", SALES, hardwareSalesFormula);

        Formula serviceSalesFormula = () -> serviceSalesCustomerMonth.getCurrentValue()
                * customers.getCurrentValue().getValue();

        Variable serviceSales = new Variable("Service sales", SALES, serviceSalesFormula);

        Formula totalSalesFormula = () -> hardwareSales.getCurrentValue() + serviceSales.getCurrentValue();

        Variable totalSales = new Variable("Total sales", SALES, totalSalesFormula);

        Variable proportionHardwareSales = new Variable("Proportion hardware", DimensionlessUnit.getInstance(),
                () -> hardwareSales.getCurrentValue() / totalSales.getCurrentValue());

        Flow customerAcquisition = new Flow(acquisitionRate);
        customers.addInflow(customerAcquisition);

        model.addStock(customers);
        model.addVariable(hardwareSales);
        model.addVariable(serviceSales);
        model.addVariable(proportionHardwareSales);

        Simulation run = new Simulation(model, Week.getInstance(), Times.years("Simulation Duration", 10));

        run.addEventHandler(new ChartViewer());
        run.addEventHandler(new CsvSubscriber("/tmp/forrester/run1out.csv"));
        run.execute();
    }

    static class Sales implements Unit {

        static Sales SALES = Sales.getInstance();

        private static final Sales instance = new Sales();

        @Override
        public String getName() {
            return "Sale";
        }

        @Override
        public Dimension getDimension() {
            return Item.getInstance();
        }

        @Override
        public double ratioToBaseUnit() {
            return 1.0;
        }

        static Sales getInstance() {
            return instance;
        }
    }
}
