package systems.courant.sd.demo;

import systems.courant.sd.Simulation;
import systems.courant.sd.measure.units.time.TimeUnits;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.Stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static systems.courant.sd.measure.Units.WEEK;

@DisplayName("SalesMixDemo (#572)")
class SalesMixDemoTest {

    @Test
    @DisplayName("customers should grow over time with constant acquisition")
    void shouldGrowCustomers() {
        Model model = buildModel(0, 10, 1000, 10, 1);
        runModel(model, 52);

        double customers = stockValue(model, "customers");
        assertThat(customers)
                .as("Customer base should grow with constant acquisition rate")
                .isGreaterThan(0);
    }

    @Test
    @DisplayName("hardware proportion should decrease as service revenue grows")
    void shouldShiftToServiceRevenue() {
        Model model = buildModel(0, 10, 1000, 10, 5);
        Simulation sim = new Simulation(model, WEEK, WEEK, 52 * 5);
        sim.execute();

        double proportion = model.getVariable("Proportion hardware")
                .orElseThrow().getValue();
        assertThat(proportion)
                .as("Hardware proportion should be below 100% as service revenue grows")
                .isLessThan(1.0);
    }

    @Test
    @DisplayName("total sales should equal hardware plus service sales")
    void shouldSumSalesCorrectly() {
        Model model = buildModel(100, 10, 1000, 10, 1);
        runModel(model, 10);

        double hardware = model.getVariable("Hardware sales").orElseThrow().getValue();
        double service = model.getVariable("Service sales").orElseThrow().getValue();
        double total = model.getVariable("Total sales").orElseThrow().getValue();
        assertThat(total).isCloseTo(hardware + service,
                org.assertj.core.data.Offset.offset(0.01));
    }

    private static Model buildModel(double initialCustomers, double newCustomersPerDay,
                                     double hardwareSalesPerCustomer,
                                     double serviceSalesPerMonth, double durationYears) {
        // Build only the model part without chart/CSV subscribers
        Model model = new Model("Hardware/software sales mix");
        Stock customers = new Stock("customers", initialCustomers,
                systems.courant.sd.measure.Units.PEOPLE);

        systems.courant.sd.model.Flow acquisitionRate =
                systems.courant.sd.model.Flows.linearGrowth("New customers",
                        systems.courant.sd.measure.Units.DAY, customers, newCustomersPerDay);

        systems.courant.sd.model.Variable hardwareSales = new systems.courant.sd.model.Variable(
                "Hardware sales", systems.courant.sd.measure.Units.US_DOLLAR,
                () -> hardwareSalesPerCustomer
                        * acquisitionRate.flowPerTimeUnit(WEEK).getValue());

        double weeksPerMonth = 52.0 / 12.0;
        systems.courant.sd.model.Variable serviceSales = new systems.courant.sd.model.Variable(
                "Service sales", systems.courant.sd.measure.Units.US_DOLLAR,
                () -> serviceSalesPerMonth / weeksPerMonth * customers.getValue());

        systems.courant.sd.model.Variable totalSales = new systems.courant.sd.model.Variable(
                "Total sales", systems.courant.sd.measure.Units.US_DOLLAR,
                () -> hardwareSales.getValue() + serviceSales.getValue());

        systems.courant.sd.model.Variable proportionHardware = new systems.courant.sd.model.Variable(
                "Proportion hardware", systems.courant.sd.measure.Units.DIMENSIONLESS,
                () -> totalSales.getValue() == 0 ? 0
                        : hardwareSales.getValue() / totalSales.getValue());

        customers.addInflow(acquisitionRate);
        model.addStock(customers);
        model.addVariable(hardwareSales);
        model.addVariable(serviceSales);
        model.addVariable(totalSales);
        model.addVariable(proportionHardware);

        return model;
    }

    private static void runModel(Model model, int weeks) {
        Simulation sim = new Simulation(model, WEEK, WEEK, weeks);
        sim.execute();
    }

    private static double stockValue(Model model, String name) {
        return model.getStocks().stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Stock not found: " + name))
                .getValue();
    }
}
