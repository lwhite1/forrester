package com.deathrayresearch.forrester.model.compile;

import com.deathrayresearch.forrester.measure.UnitRegistry;
import com.deathrayresearch.forrester.measure.units.item.ItemUnits;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Formula;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.expr.Expr;
import com.deathrayresearch.forrester.model.expr.ExprParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExprCompiler")
class ExprCompilerTest {

    private CompilationContext context;
    private ExprCompiler compiler;
    private List<Resettable> resettables;
    private int[] step;

    @BeforeEach
    void setUp() {
        step = new int[]{0};
        UnitRegistry registry = new UnitRegistry();
        context = new CompilationContext(registry, () -> step[0]);
        resettables = new ArrayList<>();
        compiler = new ExprCompiler(context, resettables);

        // Register some test elements
        context.addStock("Population", new Stock("Population", 1000, ItemUnits.PEOPLE));
        context.addConstant("Rate", new Constant("Rate", ItemUnits.THING, 0.05));
    }

    @Test
    void shouldCompileLiteral() {
        Formula formula = compiler.compile("42");
        assertEquals(42.0, formula.getCurrentValue());
    }

    @Test
    void shouldCompileRef() {
        Formula formula = compiler.compile("Population");
        assertEquals(1000.0, formula.getCurrentValue());
    }

    @Test
    void shouldCompileAddition() {
        Formula formula = compiler.compile("Population + 100");
        assertEquals(1100.0, formula.getCurrentValue());
    }

    @Test
    void shouldCompileMultiplication() {
        Formula formula = compiler.compile("Population * Rate");
        assertEquals(50.0, formula.getCurrentValue());
    }

    @Test
    void shouldCompileDivision() {
        Formula formula = compiler.compile("Population / 2");
        assertEquals(500.0, formula.getCurrentValue());
    }

    @Test
    void shouldCompilePower() {
        Formula formula = compiler.compile("2 ^ 10");
        assertEquals(1024.0, formula.getCurrentValue());
    }

    @Test
    void shouldCompileNegation() {
        Formula formula = compiler.compile("-Rate");
        assertEquals(-0.05, formula.getCurrentValue(), 1e-10);
    }

    @Test
    void shouldCompileABS() {
        Formula formula = compiler.compile("ABS(-5)");
        assertEquals(5.0, formula.getCurrentValue());
    }

    @Test
    void shouldCompileSQRT() {
        Formula formula = compiler.compile("SQRT(16)");
        assertEquals(4.0, formula.getCurrentValue());
    }

    @Test
    void shouldCompileMIN() {
        Formula formula = compiler.compile("MIN(3, 7)");
        assertEquals(3.0, formula.getCurrentValue());
    }

    @Test
    void shouldCompileMAX() {
        Formula formula = compiler.compile("MAX(3, 7)");
        assertEquals(7.0, formula.getCurrentValue());
    }

    @Test
    void shouldCompileLN() {
        Formula formula = compiler.compile("LN(1)");
        assertEquals(0.0, formula.getCurrentValue(), 1e-10);
    }

    @Test
    void shouldCompileEXP() {
        Formula formula = compiler.compile("EXP(0)");
        assertEquals(1.0, formula.getCurrentValue(), 1e-10);
    }

    @Test
    void shouldCompileTIME() {
        Formula formula = compiler.compile("TIME");
        assertEquals(0.0, formula.getCurrentValue());
        step[0] = 5;
        assertEquals(5.0, formula.getCurrentValue());
    }

    @Test
    void shouldCompileConditional() {
        Formula formula = compiler.compile("IF(Population > 500, 1, 0)");
        assertEquals(1.0, formula.getCurrentValue());
    }

    @Test
    void shouldCompileComparison() {
        Formula formula = compiler.compile("Population == 1000");
        assertEquals(1.0, formula.getCurrentValue());
    }

    @Test
    void shouldResolveUnderscoreAsSpace() {
        // "Population" is already registered; add "Birth Rate" with space
        context.addConstant("Birth Rate",
                new Constant("Birth Rate", ItemUnits.THING, 0.03));
        Formula formula = compiler.compile("Birth_Rate");
        assertEquals(0.03, formula.getCurrentValue(), 1e-10);
    }

    @Test
    void shouldThrowForUnknownRef() {
        assertThrows(CompilationException.class,
                () -> compiler.compile("NonExistent").getCurrentValue());
    }

    @Test
    void shouldThrowForUnknownFunction() {
        assertThrows(CompilationException.class,
                () -> compiler.compile("UNKNOWN_FUNC(x)"));
    }

    @Test
    void shouldCompileComplexExpression() {
        context.addConstant("Infectivity",
                new Constant("Infectivity", ItemUnits.THING, 0.10));
        Formula formula = compiler.compile("Population * Rate * Infectivity");
        assertEquals(1000 * 0.05 * 0.10, formula.getCurrentValue(), 1e-10);
    }
}
