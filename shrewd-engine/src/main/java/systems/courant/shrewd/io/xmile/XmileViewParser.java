package systems.courant.shrewd.io.xmile;

import systems.courant.shrewd.model.def.ConnectorRoute;
import systems.courant.shrewd.model.def.ElementPlacement;
import systems.courant.shrewd.model.def.ElementType;
import systems.courant.shrewd.model.def.FlowRoute;
import systems.courant.shrewd.model.def.ViewDef;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Parses XMILE {@code <view>} elements into {@link ViewDef} records.
 *
 * <p>Reads element placements (stocks, flows, variables with x/y coordinates),
 * connectors (dependency arrows), and flow graphical routes.
 */
public final class XmileViewParser {

    private static final Logger log = LoggerFactory.getLogger(XmileViewParser.class);

    private XmileViewParser() {
    }

    /**
     * Parses all {@code <view>} elements under the given {@code <views>} element.
     *
     * @param viewsElement the {@code <views>} parent element
     * @param stockNames names of stocks in the model (for type resolution)
     * @param flowNames names of flows in the model (for type resolution)
     * @param lookupNames names of lookup tables in the model (for type resolution)
     * @param warnings list to collect non-fatal warnings
     * @return list of parsed ViewDef records
     */
    public static List<ViewDef> parse(Element viewsElement, Set<String> stockNames,
                                       Set<String> flowNames, Set<String> lookupNames,
                                       List<String> warnings) {
        List<ViewDef> views = new ArrayList<>();
        NodeList viewNodes = viewsElement.getElementsByTagNameNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.VIEW);

        // Also try without namespace for compatibility
        if (viewNodes.getLength() == 0) {
            viewNodes = viewsElement.getElementsByTagName(XmileConstants.VIEW);
        }

        for (int i = 0; i < viewNodes.getLength(); i++) {
            if (!(viewNodes.item(i) instanceof Element viewElem)) {
                continue;
            }
            // Only parse direct children of <views>
            if (viewElem.getParentNode() != viewsElement) {
                continue;
            }
            String viewName = viewElem.getAttribute(XmileConstants.ATTR_NAME);
            if (viewName == null || viewName.isBlank()) {
                viewName = "View " + (i + 1);
            }

            List<ElementPlacement> elements = new ArrayList<>();
            List<ConnectorRoute> connectors = new ArrayList<>();
            List<FlowRoute> flowRoutes = new ArrayList<>();

            parseElements(viewElem, XmileConstants.STOCK, ElementType.STOCK, elements);
            parseElements(viewElem, XmileConstants.FLOW, ElementType.FLOW, elements);
            parseFlowRoutes(viewElem, flowRoutes);
            parseElements(viewElem, XmileConstants.AUX, null, elements,
                    stockNames, flowNames, lookupNames);
            parseConnectors(viewElem, connectors);

            views.add(new ViewDef(viewName, elements, connectors, flowRoutes));
        }

        return views;
    }

    private static void parseElements(Element viewElem, String tagName, ElementType fixedType,
                                       List<ElementPlacement> elements) {
        parseElements(viewElem, tagName, fixedType, elements, Set.of(), Set.of(), Set.of());
    }

    private static void parseElements(Element viewElem, String tagName, ElementType fixedType,
                                       List<ElementPlacement> elements,
                                       Set<String> stockNames, Set<String> flowNames,
                                       Set<String> lookupNames) {
        NodeList nodes = viewElem.getElementsByTagNameNS(
                XmileConstants.NAMESPACE_URI, tagName);
        if (nodes.getLength() == 0) {
            nodes = viewElem.getElementsByTagName(tagName);
        }

        for (int i = 0; i < nodes.getLength(); i++) {
            if (!(nodes.item(i) instanceof Element elem)) {
                continue;
            }
            String name = elem.getAttribute(XmileConstants.ATTR_NAME);
            if (name == null || name.isBlank()) {
                continue;
            }

            String xStr = elem.getAttribute(XmileConstants.ATTR_X);
            String yStr = elem.getAttribute(XmileConstants.ATTR_Y);
            if (xStr.isBlank() || yStr.isBlank()) {
                continue;
            }

            try {
                double x = Double.parseDouble(xStr);
                double y = Double.parseDouble(yStr);
                ElementType type = fixedType;
                if (type == null) {
                    type = resolveAuxType(name, stockNames, flowNames, lookupNames);
                }
                elements.add(new ElementPlacement(name, type, x, y));
            } catch (NumberFormatException ex) {
                log.debug("Skip malformed coordinates for element '{}': x='{}', y='{}'", name, xStr, yStr, ex);
            }
        }
    }

    private static void parseConnectors(Element viewElem, List<ConnectorRoute> connectors) {
        NodeList nodes = viewElem.getElementsByTagNameNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.CONNECTOR);
        if (nodes.getLength() == 0) {
            nodes = viewElem.getElementsByTagName(XmileConstants.CONNECTOR);
        }

        for (int i = 0; i < nodes.getLength(); i++) {
            if (!(nodes.item(i) instanceof Element elem)) {
                continue;
            }
            String from = getChildText(elem, XmileConstants.FROM).orElse(null);
            String to = getChildText(elem, XmileConstants.TO).orElse(null);
            if (from == null || from.isBlank() || to == null || to.isBlank()) {
                continue;
            }

            List<double[]> controlPoints = parseControlPoints(elem);
            connectors.add(new ConnectorRoute(from, to, controlPoints));
        }
    }

    private static void parseFlowRoutes(Element viewElem, List<FlowRoute> flowRoutes) {
        NodeList nodes = viewElem.getElementsByTagNameNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.FLOW);
        if (nodes.getLength() == 0) {
            nodes = viewElem.getElementsByTagName(XmileConstants.FLOW);
        }

        for (int i = 0; i < nodes.getLength(); i++) {
            if (!(nodes.item(i) instanceof Element elem)) {
                continue;
            }
            String name = elem.getAttribute(XmileConstants.ATTR_NAME);
            if (name == null || name.isBlank()) {
                continue;
            }

            List<double[]> points = parseControlPoints(elem);
            if (!points.isEmpty()) {
                flowRoutes.add(new FlowRoute(name, points));
            }
        }
    }

    private static List<double[]> parseControlPoints(Element elem) {
        List<double[]> points = new ArrayList<>();
        NodeList ptsNodes = elem.getElementsByTagNameNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.PTS);
        if (ptsNodes.getLength() == 0) {
            ptsNodes = elem.getElementsByTagName(XmileConstants.PTS);
        }

        for (int i = 0; i < ptsNodes.getLength(); i++) {
            if (!(ptsNodes.item(i) instanceof Element ptsElem)) {
                continue;
            }
            NodeList ptNodes = ptsElem.getElementsByTagNameNS(
                    XmileConstants.NAMESPACE_URI, XmileConstants.PT);
            if (ptNodes.getLength() == 0) {
                ptNodes = ptsElem.getElementsByTagName(XmileConstants.PT);
            }
            for (int j = 0; j < ptNodes.getLength(); j++) {
                if (!(ptNodes.item(j) instanceof Element ptElem)) {
                    continue;
                }
                String xStr = ptElem.getAttribute(XmileConstants.ATTR_X);
                String yStr = ptElem.getAttribute(XmileConstants.ATTR_Y);
                if (!xStr.isBlank() && !yStr.isBlank()) {
                    try {
                        points.add(new double[]{
                                Double.parseDouble(xStr), Double.parseDouble(yStr)});
                    } catch (NumberFormatException ex) {
                        log.debug("Skip malformed control point: x='{}', y='{}'", xStr, yStr, ex);
                    }
                }
            }
        }
        return points;
    }

    private static ElementType resolveAuxType(String name, Set<String> stockNames,
                                               Set<String> flowNames, Set<String> lookupNames) {
        if (stockNames.contains(name)) {
            return ElementType.STOCK;
        }
        if (flowNames.contains(name)) {
            return ElementType.FLOW;
        }
        if (lookupNames.contains(name)) {
            return ElementType.LOOKUP;
        }
        return ElementType.AUX;
    }

    private static Optional<String> getChildText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagNameNS(
                XmileConstants.NAMESPACE_URI, tagName);
        if (nodes.getLength() == 0) {
            nodes = parent.getElementsByTagName(tagName);
        }
        if (nodes.getLength() > 0 && nodes.item(0) instanceof Element child) {
            return Optional.of(child.getTextContent().strip());
        }
        return Optional.empty();
    }
}
