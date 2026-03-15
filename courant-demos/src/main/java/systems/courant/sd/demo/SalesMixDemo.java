/*
 * Copyright (c) 2026 Courant Systems
 * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.
 */

package systems.courant.sd.demo;

import systems.courant.sd.Simulation;
import systems.courant.sd.io.CsvSubscriber;
import systems.courant.sd.measure.units.time.Times;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Flows;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.ModelMetadata;
import systems.courant.sd.model.Stock;
import systems.courant.sd.model.Variable;
import systems.courant.sd.ui.StockLevelChartViewer;

import static systems.courant.sd.measure.Units.DAY;
import static systems.courant.sd.measure.Units.DIMENSIONLESS;
import static systems.courant.sd.measure.Units.PEOPLE;
import static systems.courant.sd.measure.Units.US_DOLLAR;
import static systems.courant.sd.measure.Units.WEEK;

/**
 * Models the evolving sales mix between hardware and recurring service revenue.
 *
 * <p>A Customers stock grows linearly. Hardware sales are a one-time amount per
 * new customer, while service sales accumulate over the full customer base. Over time the
 * proportion of hardware revenue falls as the recurring service base grows.
 */
public class SalesMixDemo {

    public static void main(String[] args) {
        double initialCustomers = 0;
        double newCustomersPerDay = 10;
        double hardwareSalesPerCustomer = 1000;  // dollars
        double serviceSalesPerCustomerPerMonth = 10;  // dollars
        double durationYears = 10;

        new SalesMixDemo().run(initialCustomers, newCustomersPerDay,
                hardwareSalesPerCustomer, serviceSalesPerCustomerPerMonth, durationYears);
    }

    public void run(double initialCustomers, double newCustomersPerDay,
                    double hardwareSalesPerCustomer, double serviceSalesPerCustomerPerMonth,
                    double durationYears) {
        Model model = getModel(initialCustomers, newCustomersPerDay,
                hardwareSalesPerCustomer, serviceSalesPerCustomerPerMonth);

        Simulation run = new Simulation(model, WEEK, Times.years(durationYears));

        run.addEventHandler(new StockLevelChartViewer());
        run.addEventHandler(new CsvSubscriber(
                System.getProperty("java.io.tmpdir") + "/courant-salesmix.csv"));
        run.execute();
    }

    public Model getModel() {
        return getModel(0, 10, 1000, 10);
    }

    public Model getModel(double initialCustomers, double newCustomersPerDay,
                           double hardwareSalesPerCustomer,
                           double serviceSalesPerCustomerPerMonth) {
        Model model = new Model("Hardware/software sales mix");
        model.setMetadata(ModelMetadata.builder()
                .license("CC-BY-SA-4.0")
                .build());

        Stock customers = new Stock("customers", initialCustomers, PEOPLE);

        Flow acquisitionRate = Flows.linearGrowth("New customers", DAY, customers,
                newCustomersPerDay);

        Variable hardwareSales = new Variable("Hardware sales", US_DOLLAR,
                () -> hardwareSalesPerCustomer * acquisitionRate.flowPerTimeUnit(WEEK).getValue());

        double weeksPerMonth = 52.0 / 12.0;
        Variable serviceSales = new Variable("Service sales", US_DOLLAR,
                () -> serviceSalesPerCustomerPerMonth / weeksPerMonth * customers.getValue());

        Variable totalSales = new Variable("Total sales", US_DOLLAR,
                () -> hardwareSales.getValue() + serviceSales.getValue());

        Variable proportionHardwareSales = new Variable("Proportion hardware", DIMENSIONLESS,
                () -> totalSales.getValue() == 0 ? 0 : hardwareSales.getValue() / totalSales.getValue());

        customers.addInflow(acquisitionRate);

        model.addStock(customers);
        model.addVariable(hardwareSales);
        model.addVariable(serviceSales);
        model.addVariable(totalSales);
        model.addVariable(proportionHardwareSales);

        return model;
    }
}
