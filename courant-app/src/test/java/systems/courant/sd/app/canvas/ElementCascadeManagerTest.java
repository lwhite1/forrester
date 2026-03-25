package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.CldVariableDef;
import systems.courant.sd.model.def.CommentDef;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.LookupTableDef;
import systems.courant.sd.model.def.ModuleInstanceDef;
import systems.courant.sd.model.NegativeValuePolicy;
import systems.courant.sd.model.def.StockDef;
import systems.courant.sd.model.def.VariableDef;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ElementCascadeManager (#1365)")
class ElementCascadeManagerTest {

    @Test
    @DisplayName("rename should preserve stock subscripts and initialExpression")
    void renameShouldPreserveStockSubscriptsAndInitialExpression() {
        List<String> subscripts = List.of("Region", "Product");
        StockDef stock = new StockDef("Inventory", null, 0,
                "Region_Count * 100", "Widget", NegativeValuePolicy.CLAMP_TO_ZERO.name(),
                subscripts);

        List<StockDef> stocks = new ArrayList<>(List.of(stock));
        Set<String> names = new LinkedHashSet<>(List.of("Inventory"));
        ElementCascadeManager mgr = createManager(stocks, names);

        boolean renamed = mgr.rename("Inventory", "Stock Level");

        assertThat(renamed).isTrue();
        StockDef result = stocks.getFirst();
        assertThat(result.name()).isEqualTo("Stock Level");
        assertThat(result.subscripts()).isEqualTo(subscripts);
        assertThat(result.initialExpression()).isEqualTo("Region_Count * 100");
        assertThat(result.unit()).isEqualTo("Widget");
        assertThat(result.negativeValuePolicy()).isEqualTo(NegativeValuePolicy.CLAMP_TO_ZERO.name());
        assertThat(result.initialValue()).isEqualTo(0);
    }

    @Test
    @DisplayName("rename should preserve stock with no subscripts or initialExpression")
    void renameShouldPreserveScalarStock() {
        StockDef stock = new StockDef("Population", null, 1000,
                null, "Person", null, List.of());

        List<StockDef> stocks = new ArrayList<>(List.of(stock));
        Set<String> names = new LinkedHashSet<>(List.of("Population"));
        ElementCascadeManager mgr = createManager(stocks, names);

        boolean renamed = mgr.rename("Population", "People");

        assertThat(renamed).isTrue();
        StockDef result = stocks.getFirst();
        assertThat(result.name()).isEqualTo("People");
        assertThat(result.subscripts()).isEmpty();
        assertThat(result.initialExpression()).isNull();
        assertThat(result.initialValue()).isEqualTo(1000);
    }

    private ElementCascadeManager createManager(List<StockDef> stocks, Set<String> names) {
        return new ElementCascadeManager(
                stocks,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                names,
                new EquationReferenceManager(new ArrayList<>(), new ArrayList<>())
        );
    }
}
