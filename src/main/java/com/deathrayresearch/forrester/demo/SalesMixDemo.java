package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.archetypes.SimpleLinearChange;
import com.deathrayresearch.forrester.io.CsvSubscriber;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Formula;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.flows.FlowPerDay;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import static com.deathrayresearch.forrester.measure.Units.DIMENSIONLESS;
import static com.deathrayresearch.forrester.measure.Units.PEOPLE;
import static com.deathrayresearch.forrester.measure.Units.US_DOLLAR;
import static com.deathrayresearch.forrester.measure.Units.WEEK;

/**
 * Models the evolving sales mix between hardware and recurring service revenue.
 *
 * <p>A Customers stock grows linearly at 10 per day. Hardware sales are a one-time amount per
 * new customer, while service sales accumulate over the full customer base. Over time the
 * proportion of hardware revenue falls as the recurring service base grows, illustrating how
 * stock-dependent flows can shift a business's revenue composition.
 */
public class SalesMixDemo {

    public static void main(String[] args) {
        new SalesMixDemo().run();
    }

    public void run() {
        Model model = new Model("Hardware/software sales mix");

        Constant hardwareSalesCustomer = new Constant("Hardware sales per new customer",
                US_DOLLAR, 1000);

        Constant serviceSalesCustomerMonth = new Constant("Service sales per customer per month",
                US_DOLLAR, 10);

        Stock customers = new Stock("customers", 0, PEOPLE);

        Flow acquisitionRate = new FlowPerDay("New customers") {
            @Override
            protected Quantity quantityPerTimeUnit() {
                return SimpleLinearChange.from(customers, 10);
            }
        };

        Formula hardwareSalesFormula = () -> hardwareSalesCustomer.getValue()
                * acquisitionRate.flowPerTimeUnit(WEEK).getValue();

        Variable hardwareSales = new Variable("Hardware sales", US_DOLLAR, hardwareSalesFormula);

        Formula serviceSalesFormula = () -> serviceSalesCustomerMonth.getValue()
                * customers.getQuantity().getValue();

        Variable serviceSales = new Variable("Service sales", US_DOLLAR, serviceSalesFormula);

        Formula totalSalesFormula = () -> hardwareSales.getValue() + serviceSales.getValue();

        Variable totalSales = new Variable("Total sales", US_DOLLAR, totalSalesFormula);

        Variable proportionHardwareSales = new Variable("Proportion hardware", DIMENSIONLESS,
                () -> hardwareSales.getValue() / totalSales.getValue());

        customers.addInflow(acquisitionRate);

        model.addStock(customers);
        model.addVariable(hardwareSales);
        model.addVariable(serviceSales);
        model.addVariable(proportionHardwareSales);

        Simulation run = new Simulation(model, WEEK, Times.years( 10));

        run.addEventHandler(new StockLevelChartViewer());
        run.addEventHandler(new CsvSubscriber(System.getProperty("java.io.tmpdir") + "/forrester-run1out.csv"));
        run.execute();
    }
}
