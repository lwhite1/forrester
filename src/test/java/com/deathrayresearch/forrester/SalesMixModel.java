package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.event.CsvSubscriber;
import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Dimensionless;
import com.deathrayresearch.forrester.measure.dimension.Item;
import com.deathrayresearch.forrester.measure.units.dimensionless.DimensionlessUnit;
import com.deathrayresearch.forrester.measure.units.item.People;
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

/**
 *
 */
public class SalesMixModel {

    @Test
    public void testModel() {
        Model model = new Model("Hardware/software sales mix");

        Constant hardwareSalesCustomer = new Constant("Hardware sales per new customer",
                Sales.getInstance(),
                1000);
        Constant serviceSalesCustomerMonth = new Constant("Service sales per customer per month",
                Sales.getInstance(),
                10);

        Stock customers = new Stock("customers", 0, People.getInstance());

        Rate acquisitionRate = new RatePerDay() {

            @Override
            protected Quantity quantityPerDay() {
                return new Quantity(10, People.getInstance());
            }
        };

        Formula hardwareSalesFormula = () -> hardwareSalesCustomer.getCurrentValue()
                * acquisitionRate.flowPerTimeUnit(Week.getInstance()).getValue();

        Variable hardwareSales = new Variable("Hardware sales", Sales.getInstance(), hardwareSalesFormula);

        Formula serviceSalesFormula = () -> serviceSalesCustomerMonth.getCurrentValue()
                * customers.getCurrentValue().getValue();

        Variable serviceSales = new Variable("Service sales", Sales.getInstance(), serviceSalesFormula);

        Formula totalSalesFormula = () -> hardwareSales.getCurrentValue() + serviceSales.getCurrentValue();

        Variable totalSales = new Variable("Total sales", Sales.getInstance(), totalSalesFormula);

        Variable proportionHardwareSales = new Variable("Proportion hardware", DimensionlessUnit.getInstance(),
                () -> hardwareSales.getCurrentValue() / totalSales.getCurrentValue());

        Flow customerAcquisition = new Flow("Customer acquisition", acquisitionRate);
        customers.addInflow(customerAcquisition);

        model.addStock(customers);
        model.addVariable(hardwareSales);
        model.addVariable(serviceSales);
        model.addVariable(proportionHardwareSales);

        com.deathrayresearch.forrester.Simulation run = new com.deathrayresearch.forrester.Simulation(model, Week.getInstance(), Times.years(10));
        run.addEventHandler(ChartViewer.newInstance(run.getEventBus()));
        run.addEventHandler(CsvSubscriber.newInstance(run.getEventBus(), "/tmp/forrester/run1out.csv"));
        run.execute();
    }

    private static class Sales implements Unit {

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
