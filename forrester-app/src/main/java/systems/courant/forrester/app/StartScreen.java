package systems.courant.forrester.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Landing screen shown when the application opens. Offers options to create a new model,
 * open an existing file, view the getting-started guide, or explore example models.
 */
final class StartScreen extends VBox {

    private static final Logger log = LoggerFactory.getLogger(StartScreen.class);

    private Runnable onNewModel;
    private Runnable onOpenFile;
    private Runnable onGettingStarted;
    private BiConsumer<String, String> onOpenExample;

    StartScreen() {
        setAlignment(Pos.TOP_CENTER);
        setStyle("-fx-background-color: #F5F6F8;");
        setPadding(new Insets(0));

        ScrollPane scroll = new ScrollPane(buildContent());
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #F5F6F8; -fx-background-color: #F5F6F8;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().add(scroll);
    }

    private VBox buildContent() {
        VBox content = new VBox(32);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(60, 40, 40, 40));
        content.setMaxWidth(800);

        // Header
        VBox header = buildHeader();

        // Action cards
        HBox actions = buildActionCards();

        // Examples section
        VBox examples = buildExamplesSection();

        content.getChildren().addAll(header, actions, examples);
        return content;
    }

    private VBox buildHeader() {
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);

        Label title = new Label("Forrester");
        title.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        Label subtitle = new Label("System Dynamics Modeling");
        subtitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #7F8C8D;");

        header.getChildren().addAll(title, subtitle);
        return header;
    }

    private HBox buildActionCards() {
        HBox cards = new HBox(16);
        cards.setAlignment(Pos.CENTER);
        cards.setMaxWidth(720);

        VBox newModelCard = buildActionCard("New Model",
                "Start with a blank canvas",
                "#2C3E50",
                () -> { if (onNewModel != null) onNewModel.run(); });
        newModelCard.setId("startNewModel");

        VBox openModelCard = buildActionCard("Open Model",
                "Open a saved model file",
                "#2C3E50",
                () -> { if (onOpenFile != null) onOpenFile.run(); });
        openModelCard.setId("startOpenModel");

        VBox gettingStartedCard = buildActionCard("Getting Started",
                "Learn the basics step by step",
                "#2C3E50",
                () -> { if (onGettingStarted != null) onGettingStarted.run(); });
        gettingStartedCard.setId("startGettingStarted");

        cards.getChildren().addAll(newModelCard, openModelCard, gettingStartedCard);

        return cards;
    }

    private VBox buildActionCard(String title, String description, String accentColor,
                                 Runnable action) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(24, 20, 24, 20));
        card.setPrefWidth(220);
        card.setMaxWidth(220);
        card.setCursor(Cursor.HAND);

        String baseStyle = "-fx-background-color: white; -fx-background-radius: 8;"
                + " -fx-border-color: #DDE1E6; -fx-border-radius: 8; -fx-border-width: 1;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 4, 0, 0, 2);";
        String hoverStyle = "-fx-background-color: white; -fx-background-radius: 8;"
                + " -fx-border-color: " + accentColor + "; -fx-border-radius: 8; -fx-border-width: 1.5;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 3);";

        card.setStyle(baseStyle);
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));
        card.setOnMouseClicked(e -> action.run());

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7F8C8D;");
        descLabel.setWrapText(true);

        card.getChildren().addAll(titleLabel, descLabel);
        return card;
    }

    private VBox buildExamplesSection() {
        VBox section = new VBox(16);
        section.setAlignment(Pos.TOP_CENTER);

        Label sectionTitle = new Label("Example Models");
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        Label sectionSubtitle = new Label("Explore models across ecology, economics, epidemiology, and more");
        sectionSubtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #7F8C8D;");

        VBox headerBox = new VBox(4, sectionTitle, sectionSubtitle);
        headerBox.setAlignment(Pos.CENTER);

        FlowPane grid = new FlowPane(12, 12);
        grid.setAlignment(Pos.CENTER);
        grid.setMaxWidth(720);

        List<ExampleEntry> examples = loadExampleCatalog();
        for (ExampleEntry example : examples) {
            grid.getChildren().add(buildExampleCard(example));
        }

        section.getChildren().addAll(headerBox, grid);
        return section;
    }

    private Region buildExampleCard(ExampleEntry example) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(12, 14, 12, 14));
        card.setPrefWidth(228);
        card.setMaxWidth(228);
        card.setCursor(Cursor.HAND);

        String difficultyColor = switch (example.difficulty) {
            case "introductory" -> "#27AE60";
            case "intermediate" -> "#F59E0B";
            case "advanced" -> "#E74C3C";
            default -> "#7F8C8D";
        };

        String baseStyle = "-fx-background-color: white; -fx-background-radius: 6;"
                + " -fx-border-color: #E8EAED; -fx-border-radius: 6; -fx-border-width: 1;";
        String hoverStyle = "-fx-background-color: #FAFBFC; -fx-background-radius: 6;"
                + " -fx-border-color: #4A90D9; -fx-border-radius: 6; -fx-border-width: 1;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 2);";

        card.setStyle(baseStyle);
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));
        card.setOnMouseClicked(e -> {
            if (onOpenExample != null) {
                onOpenExample.accept(example.name, example.path);
            }
        });

        Label nameLabel = new Label(example.name);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        Label descLabel = new Label(example.description);
        descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7F8C8D;");
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(36);

        HBox meta = new HBox(8);
        meta.setAlignment(Pos.CENTER_LEFT);
        meta.setPadding(new Insets(4, 0, 0, 0));

        Label diffLabel = new Label(capitalize(example.difficulty));
        diffLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + difficultyColor
                + "; -fx-font-weight: bold;");

        Label catLabel = new Label(formatCategory(example.category));
        catLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #95A5A6;");

        meta.getChildren().addAll(diffLabel, catLabel);

        card.getChildren().addAll(nameLabel, descLabel, meta);
        return card;
    }

    private List<ExampleEntry> loadExampleCatalog() {
        List<ExampleEntry> entries = new ArrayList<>();
        try (InputStream in = getClass().getResourceAsStream("/models/catalog.json")) {
            if (in == null) {
                return entries;
            }
            ObjectMapper om = new ObjectMapper();
            JsonNode root = om.readTree(in);
            JsonNode models = root.get("models");
            if (models == null || !models.isArray()) {
                return entries;
            }
            for (JsonNode model : models) {
                String name = model.path("name").asText(null);
                String description = model.path("description").asText("");
                String category = model.path("category").asText("");
                String difficulty = model.path("difficulty").asText("");
                String path = model.path("path").asText(null);
                if (name != null && path != null) {
                    entries.add(new ExampleEntry(name, description, category, difficulty, path));
                }
            }
        } catch (IOException ex) {
            log.warn("Failed to load examples catalog for start screen", ex);
        }
        // Sort: introductory first, then intermediate, then advanced; alphabetical within
        entries.sort((a, b) -> {
            int da = difficultyOrder(a.difficulty);
            int db = difficultyOrder(b.difficulty);
            if (da != db) {
                return Integer.compare(da, db);
            }
            return a.name.compareToIgnoreCase(b.name);
        });
        return entries;
    }

    private static int difficultyOrder(String difficulty) {
        return switch (difficulty) {
            case "introductory" -> 0;
            case "intermediate" -> 1;
            case "advanced" -> 2;
            default -> 3;
        };
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String formatCategory(String category) {
        if (category == null || category.isEmpty()) {
            return "";
        }
        return category.replace('-', ' ');
    }

    void setOnNewModel(Runnable handler) {
        this.onNewModel = handler;
    }

    void setOnOpenFile(Runnable handler) {
        this.onOpenFile = handler;
    }

    void setOnGettingStarted(Runnable handler) {
        this.onGettingStarted = handler;
    }

    void setOnOpenExample(BiConsumer<String, String> handler) {
        this.onOpenExample = handler;
    }

    private record ExampleEntry(String name, String description, String category,
                                String difficulty, String path) {
    }
}
