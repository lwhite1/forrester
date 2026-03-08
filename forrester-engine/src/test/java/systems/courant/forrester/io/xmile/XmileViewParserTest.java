package systems.courant.forrester.io.xmile;

import systems.courant.forrester.model.def.ConnectorRoute;
import systems.courant.forrester.model.def.ElementPlacement;
import systems.courant.forrester.model.def.ElementType;
import systems.courant.forrester.model.def.ViewDef;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("XmileViewParser")
class XmileViewParserTest {

    @Test
    void shouldParseElementPlacements() throws Exception {
        String xml = """
                <views xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0">
                  <view name="Main">
                    <stock name="Population" x="100" y="200"/>
                    <flow name="births" x="200" y="200"/>
                    <aux name="rate" x="200" y="300"/>
                  </view>
                </views>
                """;

        Element viewsElem = parseElement(xml);
        List<String> warnings = new ArrayList<>();
        List<ViewDef> views = XmileViewParser.parse(
                viewsElem, Set.of("Population"), Set.of("births"), Set.of(), warnings);

        assertThat(views).hasSize(1);
        ViewDef view = views.get(0);
        assertThat(view.name()).isEqualTo("Main");
        assertThat(view.elements()).hasSize(3);

        ElementPlacement stock = view.elements().stream()
                .filter(e -> e.name().equals("Population")).findFirst().orElseThrow();
        assertThat(stock.type()).isEqualTo(ElementType.STOCK);
        assertThat(stock.x()).isEqualTo(100.0);
        assertThat(stock.y()).isEqualTo(200.0);
    }

    @Test
    void shouldParseConnectors() throws Exception {
        String xml = """
                <views xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0">
                  <view name="Main">
                    <connector>
                      <from>rate</from>
                      <to>births</to>
                    </connector>
                  </view>
                </views>
                """;

        Element viewsElem = parseElement(xml);
        List<String> warnings = new ArrayList<>();
        List<ViewDef> views = XmileViewParser.parse(
                viewsElem, Set.of(), Set.of(), Set.of(), warnings);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).connectors()).hasSize(1);
        ConnectorRoute conn = views.get(0).connectors().get(0);
        assertThat(conn.from()).isEqualTo("rate");
        assertThat(conn.to()).isEqualTo("births");
    }

    @Test
    void shouldResolveAuxType() throws Exception {
        String xml = """
                <views xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0">
                  <view name="Main">
                    <aux name="my_lookup" x="100" y="100"/>
                    <aux name="regular_aux" x="200" y="200"/>
                  </view>
                </views>
                """;

        Element viewsElem = parseElement(xml);
        List<String> warnings = new ArrayList<>();
        List<ViewDef> views = XmileViewParser.parse(
                viewsElem, Set.of(), Set.of(), Set.of("my_lookup"), warnings);

        assertThat(views).hasSize(1);
        ElementPlacement lookup = views.get(0).elements().stream()
                .filter(e -> e.name().equals("my_lookup")).findFirst().orElseThrow();
        assertThat(lookup.type()).isEqualTo(ElementType.LOOKUP);

        ElementPlacement aux = views.get(0).elements().stream()
                .filter(e -> e.name().equals("regular_aux")).findFirst().orElseThrow();
        assertThat(aux.type()).isEqualTo(ElementType.AUX);
    }

    @Test
    void shouldAssignDefaultViewName() throws Exception {
        String xml = """
                <views xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0">
                  <view>
                    <stock name="S" x="100" y="100"/>
                  </view>
                </views>
                """;

        Element viewsElem = parseElement(xml);
        List<String> warnings = new ArrayList<>();
        List<ViewDef> views = XmileViewParser.parse(
                viewsElem, Set.of("S"), Set.of(), Set.of(), warnings);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).name()).isEqualTo("View 1");
    }

    private static Element parseElement(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));
        return doc.getDocumentElement();
    }
}
