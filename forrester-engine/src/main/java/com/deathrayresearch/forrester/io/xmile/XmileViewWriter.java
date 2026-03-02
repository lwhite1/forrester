package com.deathrayresearch.forrester.io.xmile;

import com.deathrayresearch.forrester.model.def.ConnectorRoute;
import com.deathrayresearch.forrester.model.def.ElementPlacement;
import com.deathrayresearch.forrester.model.def.FlowRoute;
import com.deathrayresearch.forrester.model.def.ViewDef;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Locale;

/**
 * Writes {@link ViewDef} records to XMILE {@code <views>} XML elements.
 *
 * <p>Reverse of {@link XmileViewParser} — serializes element placements,
 * connectors, and flow routes to the XMILE view XML format.
 */
public final class XmileViewWriter {

    private XmileViewWriter() {
    }

    /**
     * Writes views to a {@code <views>} element under the given parent.
     *
     * @param doc the XML document
     * @param parent the parent element to append {@code <views>} to
     * @param views the view definitions to write
     */
    public static void write(Document doc, Element parent, List<ViewDef> views) {
        if (views == null || views.isEmpty()) {
            return;
        }

        Element viewsElem = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.VIEWS);
        parent.appendChild(viewsElem);

        for (ViewDef view : views) {
            writeView(doc, viewsElem, view);
        }
    }

    private static void writeView(Document doc, Element viewsElem, ViewDef view) {
        Element viewElem = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.VIEW);
        viewElem.setAttribute(XmileConstants.ATTR_NAME, view.name());
        viewsElem.appendChild(viewElem);

        // Write element placements
        for (ElementPlacement ep : view.elements()) {
            writeElementPlacement(doc, viewElem, ep);
        }

        // Write flow routes
        for (FlowRoute fr : view.flowRoutes()) {
            writeFlowRoute(doc, viewElem, fr);
        }

        // Write connectors
        for (ConnectorRoute cr : view.connectors()) {
            writeConnector(doc, viewElem, cr);
        }
    }

    private static void writeElementPlacement(Document doc, Element viewElem,
                                               ElementPlacement ep) {
        String tagName = mapTypeToTag(ep.type());
        Element elem = doc.createElementNS(XmileConstants.NAMESPACE_URI, tagName);
        elem.setAttribute(XmileConstants.ATTR_NAME, ep.name());
        elem.setAttribute(XmileConstants.ATTR_X, formatDouble(ep.x()));
        elem.setAttribute(XmileConstants.ATTR_Y, formatDouble(ep.y()));
        viewElem.appendChild(elem);
    }

    private static void writeFlowRoute(Document doc, Element viewElem, FlowRoute fr) {
        Element flowElem = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.FLOW);
        flowElem.setAttribute(XmileConstants.ATTR_NAME, fr.flowName());

        if (!fr.points().isEmpty()) {
            Element ptsElem = doc.createElementNS(
                    XmileConstants.NAMESPACE_URI, XmileConstants.PTS);
            for (double[] point : fr.points()) {
                Element ptElem = doc.createElementNS(
                        XmileConstants.NAMESPACE_URI, XmileConstants.PT);
                ptElem.setAttribute(XmileConstants.ATTR_X, formatDouble(point[0]));
                ptElem.setAttribute(XmileConstants.ATTR_Y, formatDouble(point[1]));
                ptsElem.appendChild(ptElem);
            }
            flowElem.appendChild(ptsElem);
        }

        viewElem.appendChild(flowElem);
    }

    private static void writeConnector(Document doc, Element viewElem, ConnectorRoute cr) {
        Element connElem = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.CONNECTOR);

        Element fromElem = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.FROM);
        fromElem.setTextContent(cr.from());
        connElem.appendChild(fromElem);

        Element toElem = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.TO);
        toElem.setTextContent(cr.to());
        connElem.appendChild(toElem);

        if (!cr.controlPoints().isEmpty()) {
            Element ptsElem = doc.createElementNS(
                    XmileConstants.NAMESPACE_URI, XmileConstants.PTS);
            for (double[] point : cr.controlPoints()) {
                Element ptElem = doc.createElementNS(
                        XmileConstants.NAMESPACE_URI, XmileConstants.PT);
                ptElem.setAttribute(XmileConstants.ATTR_X, formatDouble(point[0]));
                ptElem.setAttribute(XmileConstants.ATTR_Y, formatDouble(point[1]));
                ptsElem.appendChild(ptElem);
            }
            connElem.appendChild(ptsElem);
        }

        viewElem.appendChild(connElem);
    }

    private static String mapTypeToTag(String type) {
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "stock" -> XmileConstants.STOCK;
            case "flow" -> XmileConstants.FLOW;
            default -> XmileConstants.AUX;
        };
    }

    private static String formatDouble(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
