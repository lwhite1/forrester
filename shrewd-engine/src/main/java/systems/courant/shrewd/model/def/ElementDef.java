package systems.courant.shrewd.model.def;

/**
 * Sealed interface for all model element definition types.
 * Provides compile-time type safety when element definitions are stored
 * or passed generically (e.g. in clipboard operations).
 */
public sealed interface ElementDef
        permits StockDef, FlowDef, AuxDef,
                LookupTableDef, ModuleInstanceDef, CldVariableDef, CommentDef {
}
