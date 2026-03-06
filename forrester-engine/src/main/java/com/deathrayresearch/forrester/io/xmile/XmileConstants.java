package com.deathrayresearch.forrester.io.xmile;

/**
 * Constants for the OASIS XMILE XML format.
 *
 * <p>Centralizes namespace URIs, element names, and attribute names to prevent
 * typos and provide a single source of truth for XMILE XML structure.
 */
public final class XmileConstants {

    private XmileConstants() {
    }

    // --- Namespace ---
    public static final String NAMESPACE_URI = "http://docs.oasis-open.org/xmile/ns/XMILE/v1.0";
    public static final String ISEE_NAMESPACE_URI = "http://iseesystems.com/XMILE";

    // --- Root element ---
    public static final String XMILE = "xmile";
    public static final String ATTR_VERSION = "version";
    public static final String VERSION_1_0 = "1.0";

    // --- Header ---
    public static final String HEADER = "header";
    public static final String NAME = "name";
    public static final String VENDOR = "vendor";
    public static final String PRODUCT = "product";

    // --- Simulation specs ---
    public static final String SIM_SPECS = "sim_specs";
    public static final String ATTR_TIME_UNITS = "time_units";
    public static final String START = "start";
    public static final String STOP = "stop";
    public static final String DT = "dt";

    // --- Model ---
    public static final String MODEL = "model";
    public static final String VARIABLES = "variables";

    // --- Variable elements ---
    public static final String STOCK = "stock";
    public static final String FLOW = "flow";
    public static final String AUX = "aux";
    public static final String ATTR_NAME = "name";

    // --- Variable children ---
    public static final String EQN = "eqn";
    public static final String INFLOW = "inflow";
    public static final String OUTFLOW = "outflow";
    public static final String UNITS = "units";
    public static final String DOC = "doc";
    public static final String NON_NEGATIVE = "non_negative";

    // --- Graphical function (lookup table) ---
    public static final String GF = "gf";
    public static final String XSCALE = "xscale";
    public static final String YSCALE = "yscale";
    public static final String XPTS = "xpts";
    public static final String YPTS = "ypts";
    public static final String ATTR_MIN = "min";
    public static final String ATTR_MAX = "max";

    // --- Views ---
    public static final String VIEWS = "views";
    public static final String VIEW = "view";
    public static final String CONNECTOR = "connector";
    public static final String FROM = "from";
    public static final String TO = "to";
    public static final String ATTR_X = "x";
    public static final String ATTR_Y = "y";
    public static final String ATTR_UID = "uid";
    public static final String PTS = "pts";
    public static final String PT = "pt";

    // --- Graphical function attributes ---
    public static final String ATTR_TYPE = "type";

    // --- Stock attributes (unsupported special types) ---
    public static final String ATTR_CONVEYOR = "conveyor";
    public static final String ATTR_QUEUE = "queue";
    public static final String ATTR_OVEN = "oven";

    // --- Variable children (unsupported but warned) ---
    public static final String RANGE = "range";

    // --- Module elements ---
    public static final String MODULE = "module";
    public static final String CONNECT = "connect";
    public static final String ATTR_TO = "to";
    public static final String ATTR_FROM = "from";

    // --- Unsupported elements (emit warnings) ---
    public static final String GROUP = "group";
    public static final String MACRO = "macro";
    public static final String EVENT_POSTER = "event_poster";
}
