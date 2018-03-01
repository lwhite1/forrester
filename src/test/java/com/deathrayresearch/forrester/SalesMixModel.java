package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.archetypes.SimpleLinearChange;
import com.deathrayresearch.forrester.io.CsvSubscriber;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Formula;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.rate.Flow;
import com.deathrayresearch.forrester.rate.FlowPerDay;
import com.deathrayresearch.forrester.ui.ChartViewer;
import org.junit.Test;

import static com.deathrayresearch.forrester.measure.Units.*;

/**
 *
 */
public class SalesMixModel {

    @Test
    public void testModel() {
        Model model = new Model("Hardware/software sales mix");

        Constant hardwareSalesCustomer = new Constant("Hardware sales per new customer",
                US_DOLLAR, 1000);

        Constant serviceSalesCustomerMonth = new Constant("Service sales per customer per month",
                US_DOLLAR, 10);

        Stock customers = new Stock("customers", 0, PEOPLE);

        Flow acquisitionRate = new FlowPerDay("New customers") {
            @Override
            protected Quantity quantityPerDay() {
                return SimpleLinearChange.from(customers, 10);
            }
        };

        Formula hardwareSalesFormula = () -> hardwareSalesCustomer.getCurrentValue()
                * acquisitionRate.flowPerTimeUnit(WEEK).getValue();

        Variable hardwareSales = new Variable("Hardware sales", US_DOLLAR, hardwareSalesFormula);

        Formula serviceSalesFormula = () -> serviceSalesCustomerMonth.getCurrentValue()
                * customers.getCurrentValue().getValue();

        Variable serviceSales = new Variable("Service sales", US_DOLLAR, serviceSalesFormula);

        Formula totalSalesFormula = () -> hardwareSales.getCurrentValue() + serviceSales.getCurrentValue();

        Variable totalSales = new Variable("Total sales", US_DOLLAR, totalSalesFormula);

        Variable proportionHardwareSales = new Variable("Proportion hardware", DIMENSIONLESS,
                () -> hardwareSales.getCurrentValue() / totalSales.getCurrentValue());

        customers.addInflow(acquisitionRate);

        model.addStock(customers);
        model.addVariable(hardwareSales);
        model.addVariable(serviceSales);
        model.addVariable(proportionHardwareSales);

        Simulation run = new Simulation(model, WEEK, Times.years( 10));

        run.addEventHandler(new ChartViewer());
        run.addEventHandler(new CsvSubscriber("/tmp/forrester/run1out.csv"));
        run.execute();
    }
}
