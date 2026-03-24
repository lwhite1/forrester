package systems.courant.sd.app.canvas.dialogs;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * A dialog that presents the available tutorials as clickable cards.
 * Replaces the three separate tutorial cards on the start screen top row.
 */
public class TutorialChooserDialog extends Stage {

    private Runnable onGettingStarted;
    private Runnable onSirTutorial;
    private Runnable onSupplyChainTutorial;
    private Runnable onCldTutorial;

    public TutorialChooserDialog() {
        setTitle("Tutorials");
        initModality(Modality.APPLICATION_MODAL);

        VBox content = new VBox(24);
        content.setPadding(new Insets(32, 36, 32, 36));
        content.setAlignment(Pos.TOP_CENTER);
        content.setStyle("-fx-background-color: #F5F6F8;");

        Label heading = new Label("Choose a Tutorial");
        heading.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        Label subtitle = new Label("Step-by-step guides to learn system dynamics modeling");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #7F8C8D;");

        VBox header = new VBox(6, heading, subtitle);
        header.setAlignment(Pos.CENTER);

        VBox gettingStarted = buildTutorialCard(
                "Getting Started",
                "Build your first model — a simple coffee cooling simulation. "
                        + "Learn to place stocks, flows, and variables, write equations, and run a simulation.",
                "Beginner",
                "#27AE60",
                () -> {
                    if (onGettingStarted != null) {
                        close();
                        onGettingStarted.run();
                    }
                });
        gettingStarted.setId("tutorialGettingStarted");

        VBox sir = buildTutorialCard(
                "SIR Epidemic Model",
                "Model infectious disease spread with Susceptible, Infected, and Recovered populations. "
                        + "Explore reinforcing feedback, balancing loops, and S-shaped growth.",
                "Beginner",
                "#27AE60",
                () -> {
                    if (onSirTutorial != null) {
                        close();
                        onSirTutorial.run();
                    }
                });
        sir.setId("tutorialSir");

        VBox supplyChain = buildTutorialCard(
                "Supply Chain Model",
                "Build a supply chain with inventory, orders, and delivery delays. "
                        + "Discover oscillation, overshoot, and the bullwhip effect.",
                "Intermediate",
                "#F59E0B",
                () -> {
                    if (onSupplyChainTutorial != null) {
                        close();
                        onSupplyChainTutorial.run();
                    }
                });
        supplyChain.setId("tutorialSupplyChain");

        VBox cld = buildTutorialCard(
                "Causal Loop Diagrams",
                "Map cause-and-effect with CLD variables and causal links. "
                        + "Learn polarity, identify reinforcing and balancing feedback loops, "
                        + "and transition from qualitative to quantitative modeling.",
                "Beginner",
                "#27AE60",
                () -> {
                    if (onCldTutorial != null) {
                        close();
                        onCldTutorial.run();
                    }
                });
        cld.setId("tutorialCld");

        VBox cards = new VBox(12, gettingStarted, sir, supplyChain, cld);
        cards.setAlignment(Pos.TOP_CENTER);

        content.getChildren().addAll(header, cards);

        Scene scene = new Scene(content, 480, 510);
        setScene(scene);
    }

    private VBox buildTutorialCard(String title, String description, String level,
                                    String levelColor, Runnable action) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setCursor(Cursor.HAND);
        card.setMaxWidth(Double.MAX_VALUE);

        String baseStyle = "-fx-background-color: white; -fx-background-radius: 8;"
                + " -fx-border-color: #DDE1E6; -fx-border-radius: 8; -fx-border-width: 1;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 4, 0, 0, 2);";
        String hoverStyle = "-fx-background-color: white; -fx-background-radius: 8;"
                + " -fx-border-color: #4A90D9; -fx-border-radius: 8; -fx-border-width: 1.5;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 3);";

        card.setStyle(baseStyle);
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));
        card.setOnMouseClicked(e -> action.run());

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5A6A7A;");
        descLabel.setWrapText(true);

        Label levelLabel = new Label(level);
        levelLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + levelColor + ";");

        HBox topRow = new HBox(titleLabel);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        topRow.getChildren().add(levelLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(topRow, descLabel);
        return card;
    }

    public void setOnGettingStarted(Runnable handler) {
        this.onGettingStarted = handler;
    }

    public void setOnSirTutorial(Runnable handler) {
        this.onSirTutorial = handler;
    }

    public void setOnSupplyChainTutorial(Runnable handler) {
        this.onSupplyChainTutorial = handler;
    }

    public void setOnCldTutorial(Runnable handler) {
        this.onCldTutorial = handler;
    }
}
