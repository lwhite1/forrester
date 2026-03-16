package systems.courant.sd.io.xmile;

import systems.courant.sd.model.def.ConnectorRoute;
import systems.courant.sd.model.def.ElementPlacement;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.FlowRoute;
import systems.courant.sd.model.def.ViewDef;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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

        for (Element viewElem : getDirectChildren(viewsElement, XmileConstants.VIEW)) {
            String viewName = viewElem.getAttribute(XmileConstants.ATTR_NAME);
            if (viewName.isBlank()) {
                viewName = "View " + (views.size() + 1);
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
        for (Element elem : getDirectChildren(viewElem, tagName)) {
            String name = elem.getAttribute(XmileConstants.ATTR_NAME);
            if (name.isBlank()) {
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
        for (Element elem : getDirectChildren(viewElem, XmileConstants.CONNECTOR)) {
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
        for (Element elem : getDirectChildren(viewElem, XmileConstants.FLOW)) {
            String name = elem.getAttribute(XmileConstants.ATTR_NAME);
            if (name.isBlank()) {
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
        for (Element ptsElem : getDirectChildren(elem, XmileConstants.PTS)) {
            for (Element ptElem : getDirectChildren(ptsElem, XmileConstants.PT)) {
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
        for (Element child : getDirectChildren(parent, tagName)) {
            return Optional.of(child.getTextContent().strip());
        }
        return Optional.empty();
    }

    /**
     * Returns direct child elements of {@code parent} matching the given local tag name,
     * checking both namespaced and non-namespaced variants.
     * Unlike {@code getElementsByTagNameNS}, this does not search the entire subtree.
     */
    private static List<Element> getDirectChildren(Element parent, String localName) {
        List<Element> result = new ArrayList<>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element child && matchesTag(child, localName)) {
                result.add(child);
            }
        }
        return result;
    }

    private static boolean matchesTag(Element elem, String localName) {
        if (localName.equals(elem.getLocalName())) {
            String ns = elem.getNamespaceURI();
            return ns == null || ns.equals(XmileConstants.NAMESPACE_URI);
        }
        // Fallback for non-namespace-aware documents
        return localName.equals(elem.getTagName());
    }
}
