package systems.courant.forrester.io.xmile;

import systems.courant.forrester.model.def.ConnectorRoute;
import systems.courant.forrester.model.def.ElementPlacement;
import systems.courant.forrester.model.def.ElementType;
import systems.courant.forrester.model.def.FlowRoute;
import systems.courant.forrester.model.def.ViewDef;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("XmileViewWriter")
class XmileViewWriterTest {

    @Test
    void shouldWriteAndParseBackElementPlacements() throws Exception {
        ViewDef view = new ViewDef("Main", List.of(
                new ElementPlacement("Pop", ElementType.STOCK, 100, 200),
                new ElementPlacement("rate", ElementType.AUX, 300, 400)
        ), List.of(), List.of());

        Document doc = createDocument();
        Element parent = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, "model");
        doc.getDocumentElement().appendChild(parent);

        XmileViewWriter.write(doc, parent, List.of(view));

        // Parse back
        Element viewsElem = (Element) parent.getElementsByTagNameNS(
                XmileConstants.NAMESPACE_URI, "views").item(0);
        assertThat(viewsElem).isNotNull();

        List<String> warnings = new ArrayList<>();
        List<ViewDef> parsed = XmileViewParser.parse(
                viewsElem, Set.of("Pop"), Set.of(), Set.of(), warnings);

        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0).name()).isEqualTo("Main");
        assertThat(parsed.get(0).elements()).hasSize(2);

        ElementPlacement pop = parsed.get(0).elements().stream()
                .filter(e -> e.name().equals("Pop")).findFirst().orElseThrow();
        assertThat(pop.x()).isEqualTo(100.0);
        assertThat(pop.y()).isEqualTo(200.0);
    }

    @Test
    void shouldWriteAndParseBackConnectors() throws Exception {
        ViewDef view = new ViewDef("Main", List.of(),
                List.of(new ConnectorRoute("a", "b")), List.of());

        Document doc = createDocument();
        Element parent = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, "model");
        doc.getDocumentElement().appendChild(parent);

        XmileViewWriter.write(doc, parent, List.of(view));

        Element viewsElem = (Element) parent.getElementsByTagNameNS(
                XmileConstants.NAMESPACE_URI, "views").item(0);
        List<String> warnings = new ArrayList<>();
        List<ViewDef> parsed = XmileViewParser.parse(
                viewsElem, Set.of(), Set.of(), Set.of(), warnings);

        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0).connectors()).hasSize(1);
        assertThat(parsed.get(0).connectors().get(0).from()).isEqualTo("a");
        assertThat(parsed.get(0).connectors().get(0).to()).isEqualTo("b");
    }

    @Test
    void shouldWriteFlowRoutes() throws Exception {
        List<double[]> points = List.of(new double[]{10, 20}, new double[]{30, 40});
        ViewDef view = new ViewDef("Main", List.of(),
                List.of(), List.of(new FlowRoute("myFlow", points)));

        Document doc = createDocument();
        Element parent = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, "model");
        doc.getDocumentElement().appendChild(parent);

        XmileViewWriter.write(doc, parent, List.of(view));

        Element viewsElem = (Element) parent.getElementsByTagNameNS(
                XmileConstants.NAMESPACE_URI, "views").item(0);
        List<String> warnings = new ArrayList<>();
        List<ViewDef> parsed = XmileViewParser.parse(
                viewsElem, Set.of(), Set.of("myFlow"), Set.of(), warnings);

        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0).flowRoutes()).hasSize(1);
        assertThat(parsed.get(0).flowRoutes().get(0).flowName()).isEqualTo("myFlow");
        assertThat(parsed.get(0).flowRoutes().get(0).points()).hasSize(2);
    }

    @Test
    void shouldSkipEmptyViewsList() throws Exception {
        Document doc = createDocument();
        Element parent = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, "model");
        doc.getDocumentElement().appendChild(parent);

        XmileViewWriter.write(doc, parent, List.of());

        NodeList viewsNodes = parent.getElementsByTagNameNS(
                XmileConstants.NAMESPACE_URI, "views");
        assertThat(viewsNodes.getLength()).isEqualTo(0);
    }

    private static Document createDocument() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        Element root = doc.createElementNS(XmileConstants.NAMESPACE_URI, "xmile");
        doc.appendChild(root);
        return doc;
    }
}
