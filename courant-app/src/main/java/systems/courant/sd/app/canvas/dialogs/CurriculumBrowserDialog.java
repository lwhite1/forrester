package systems.courant.sd.app.canvas.dialogs;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import systems.courant.sd.app.TutorialProgressStore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A two-panel curriculum browser that replaces the flat
 * {@link TutorialChooserDialog}. Left panel shows a tree of tracks,
 * tiers, and tutorials with progress indicators. Right panel shows
 * detail for the selected item with a launch button.
 */
public class CurriculumBrowserDialog extends Stage {

    private final StackPane detailPane;
    private final Map<String, TutorialContent> tutorialCache = new LinkedHashMap<>();
    private Consumer<String> onLaunchTutorial;

    public CurriculumBrowserDialog() {
        setTitle("Tutorial Curriculum");

        List<CurriculumLoader.Track> tracks = CurriculumLoader.load();

        TreeView<Object> treeView = buildTreeView(tracks);
        treeView.setPrefWidth(220);
        treeView.setMinWidth(180);
        treeView.setShowRoot(false);
        treeView.setId("curriculumTree");

        detailPane = new StackPane();
        detailPane.setPadding(new Insets(24));
        detailPane.setStyle("-fx-background-color: #F5F6F8;");

        BorderPane root = new BorderPane();
        root.setLeft(treeView);
        root.setCenter(detailPane);

        Scene scene = new Scene(root, 700, 520);
        setScene(scene);

        // Show detail when selection changes
        treeView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldItem, newItem) -> {
                    if (newItem != null && newItem.getValue() != null) {
                        Object value = newItem.getValue();
                        if (value instanceof TutorialEntry entry) {
                            showTutorialDetail(entry);
                        } else if (value instanceof TierEntry tier) {
                            showTierDetail(tier);
                        } else if (value instanceof TrackEntry track) {
                            showTrackDetail(track);
                        }
                    }
                });

        // Show welcome by default
        showWelcome();
    }

    /**
     * Sets the callback invoked when the user clicks "Start" on a tutorial.
     * The parameter is the tutorial JSON resource path
     * (e.g., {@code "modeling/first-model.json"}).
     */
    public void setOnLaunchTutorial(Consumer<String> handler) {
        this.onLaunchTutorial = handler;
    }

    // ── Tree value types ─────────────────────────────────────────

    record TrackEntry(String name) {
    }

    record TierEntry(CurriculumLoader.Tier tier, String trackId) {
    }

    record TutorialEntry(String tutorialId, String trackId) {
    }

    // ── Tree construction ────────────────────────────────────────

    private TreeView<Object> buildTreeView(List<CurriculumLoader.Track> tracks) {
        TreeItem<Object> root = new TreeItem<>();

        for (CurriculumLoader.Track track : tracks) {
            TreeItem<Object> trackItem = new TreeItem<>(new TrackEntry(track.name()));
            trackItem.setExpanded(true);

            for (CurriculumLoader.Tier tier : track.tiers()) {
                TreeItem<Object> tierItem = new TreeItem<>(
                        new TierEntry(tier, track.id()));
                tierItem.setExpanded(true);

                for (String tutorialId : tier.tutorialIds()) {
                    tierItem.getChildren().add(new TreeItem<>(
                            new TutorialEntry(tutorialId, track.id())));
                }
                trackItem.getChildren().add(tierItem);
            }
            root.getChildren().add(trackItem);
        }

        TreeView<Object> tree = new TreeView<>(root);
        tree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else if (item instanceof TrackEntry track) {
                    setText(track.name());
                    setGraphic(null);
                    setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
                } else if (item instanceof TierEntry tierEntry) {
                    setGraphic(buildTierCell(tierEntry));
                    setText(null);
                    setStyle("");
                } else if (item instanceof TutorialEntry entry) {
                    boolean completed = TutorialProgressStore.isCompleted(entry.tutorialId());
                    String check = completed ? "\u2713 " : "   ";
                    TutorialContent content = loadTutorial(entry);
                    setText(check + (content != null ? content.title() : entry.tutorialId()));
                    setGraphic(null);
                    setStyle(completed
                            ? "-fx-text-fill: #27AE60;"
                            : "");
                }
            }
        });
        return tree;
    }

    private HBox buildTierCell(TierEntry tierEntry) {
        CurriculumLoader.Tier tier = tierEntry.tier();
        int total = tier.tutorialIds().size();
        int completed = TutorialProgressStore.getCompletedCount(tier.tutorialIds());

        Label nameLabel = new Label(tier.name());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        Label countLabel = new Label(completed + "/" + total);
        countLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7F8C8D;");

        HBox cell = new HBox(6, nameLabel, countLabel);
        cell.setAlignment(Pos.CENTER_LEFT);
        return cell;
    }

    // ── Detail panels ────────────────────────────────────────────

    private void showWelcome() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        Label heading = new Label("Tutorial Curriculum");
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        Label subtitle = new Label("Select a tutorial from the tree to get started.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #7F8C8D;");
        box.getChildren().addAll(heading, subtitle);
        detailPane.getChildren().setAll(box);
    }

    private void showTrackDetail(TrackEntry track) {
        VBox box = new VBox(12);
        box.setAlignment(Pos.TOP_LEFT);
        Label heading = new Label(track.name());
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        Label subtitle = new Label("Select a tier or tutorial below.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #7F8C8D;");
        box.getChildren().addAll(heading, subtitle);
        detailPane.getChildren().setAll(box);
    }

    private void showTierDetail(TierEntry tierEntry) {
        CurriculumLoader.Tier tier = tierEntry.tier();
        int total = tier.tutorialIds().size();
        int completed = TutorialProgressStore.getCompletedCount(tier.tutorialIds());

        VBox box = new VBox(16);
        box.setAlignment(Pos.TOP_LEFT);

        Label heading = new Label(tier.name());
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        Label desc = new Label(tier.description());
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #5A6A7A;");
        desc.setWrapText(true);

        box.getChildren().addAll(heading, desc);

        if (total > 0) {
            ProgressBar progressBar = new ProgressBar(
                    (double) completed / total);
            progressBar.setPrefWidth(250);
            progressBar.setMaxWidth(250);

            Label progressLabel = new Label(completed + " of " + total + " completed");
            progressLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7F8C8D;");

            box.getChildren().addAll(progressBar, progressLabel);

            // List tutorials in this tier
            for (String tutorialId : tier.tutorialIds()) {
                TutorialContent content = loadTutorial(
                        new TutorialEntry(tutorialId, tierEntry.trackId()));
                if (content != null) {
                    boolean done = TutorialProgressStore.isCompleted(tutorialId);
                    String check = done ? "\u2713 " : "\u2022 ";
                    Label item = new Label(check + content.title());
                    item.setStyle(done
                            ? "-fx-font-size: 12px; -fx-text-fill: #27AE60;"
                            : "-fx-font-size: 12px; -fx-text-fill: #2C3E50;");
                    box.getChildren().add(item);
                }
            }
        } else {
            Label empty = new Label("No tutorials available yet.");
            empty.setStyle("-fx-font-size: 12px; -fx-text-fill: #7F8C8D; -fx-font-style: italic;");
            box.getChildren().add(empty);
        }

        if (!tier.recommendedPrerequisites().isEmpty()) {
            Label prereqLabel = new Label(
                    "Recommended prerequisite: " + String.join(", ", tier.recommendedPrerequisites()));
            prereqLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #95A5A6; -fx-font-style: italic;");
            prereqLabel.setWrapText(true);
            box.getChildren().add(prereqLabel);
        }

        detailPane.getChildren().setAll(box);
    }

    private void showTutorialDetail(TutorialEntry entry) {
        TutorialContent content = loadTutorial(entry);
        if (content == null) {
            showWelcome();
            return;
        }

        boolean completed = TutorialProgressStore.isCompleted(content.id());
        boolean hasResume = TutorialProgressStore.getResumePoint()
                .filter(rp -> rp.tutorialId().equals(content.id()))
                .isPresent();

        VBox box = new VBox(14);
        box.setAlignment(Pos.TOP_LEFT);

        // Title row
        Label title = new Label(content.title());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        // Badges row
        String diffColor = switch (content.difficulty()) {
            case "intermediate" -> "#F59E0B";
            case "advanced" -> "#E74C3C";
            default -> "#27AE60";
        };
        Label diffBadge = new Label(capitalize(content.difficulty()));
        diffBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + diffColor + ";");

        Label timeBadge = new Label("\u23F1 " + content.estimatedMinutes() + " min");
        timeBadge.setStyle("-fx-font-size: 11px; -fx-text-fill: #7F8C8D;");

        HBox badges = new HBox(12, diffBadge, timeBadge);
        if (completed) {
            Label doneBadge = new Label("\u2713 Completed");
            doneBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #27AE60;");
            badges.getChildren().add(doneBadge);
        }
        badges.setAlignment(Pos.CENTER_LEFT);

        // Description
        Label desc = new Label(content.description());
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #5A6A7A;");
        desc.setWrapText(true);

        // Steps preview
        Label stepsHeading = new Label("Steps (" + content.steps().size() + ")");
        stepsHeading.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        VBox stepsList = new VBox(4);
        for (int i = 0; i < content.steps().size(); i++) {
            Label step = new Label((i + 1) + ". " + content.steps().get(i).title());
            step.setStyle("-fx-font-size: 12px; -fx-text-fill: #5A6A7A;");
            stepsList.getChildren().add(step);
        }

        // Prerequisites
        box.getChildren().addAll(title, badges, desc, stepsHeading, stepsList);

        if (!content.recommendedPrerequisites().isEmpty()) {
            Label prereq = new Label(
                    "Recommended: " + String.join(", ", content.recommendedPrerequisites()));
            prereq.setStyle("-fx-font-size: 11px; -fx-text-fill: #95A5A6; -fx-font-style: italic;");
            box.getChildren().add(prereq);
        }

        // Launch button
        String buttonText = completed ? "Review" : (hasResume ? "Resume" : "Start");
        Button launchButton = new Button(buttonText);
        launchButton.setId("launchTutorialButton");
        launchButton.setStyle(
                "-fx-background-color: #4A90D9; -fx-text-fill: white; -fx-font-size: 13px;"
                        + " -fx-font-weight: bold; -fx-padding: 8 24; -fx-background-radius: 4;"
                        + " -fx-cursor: hand;");
        String jsonPath = entry.trackId() + "/" + entry.tutorialId() + ".json";
        launchButton.setOnAction(e -> {
            if (onLaunchTutorial != null) {
                close();
                onLaunchTutorial.accept(jsonPath);
            }
        });

        VBox buttonBox = new VBox(launchButton);
        buttonBox.setPadding(new Insets(8, 0, 0, 0));
        HBox.setHgrow(buttonBox, Priority.ALWAYS);

        box.getChildren().add(buttonBox);

        detailPane.getChildren().setAll(box);
    }

    // ── Helpers ──────────────────────────────────────────────────

    private TutorialContent loadTutorial(TutorialEntry entry) {
        String jsonPath = entry.trackId() + "/" + entry.tutorialId() + ".json";
        return tutorialCache.computeIfAbsent(jsonPath, path -> {
            try {
                return TutorialContentLoader.load(path);
            } catch (Exception e) {
                return null;
            }
        });
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
