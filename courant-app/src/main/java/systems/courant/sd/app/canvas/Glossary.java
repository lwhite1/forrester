package systems.courant.sd.app.canvas;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Loads and provides access to the System Dynamics terminology glossary.
 * Entries are loaded from {@code sd-glossary.json} on the classpath.
 *
 * <p>This class is thread-safe after construction; the glossary is immutable once loaded.
 */
public final class Glossary {

    /**
     * A single glossary entry.
     */
    public record Entry(
            String term,
            List<String> aliases,
            String definition,
            String relevance,
            List<String> related
    ) {
        @JsonCreator
        public Entry(
                @JsonProperty("term") String term,
                @JsonProperty("aliases") List<String> aliases,
                @JsonProperty("definition") String definition,
                @JsonProperty("relevance") String relevance,
                @JsonProperty("related") List<String> related
        ) {
            this.term = term;
            this.aliases = aliases != null ? List.copyOf(aliases) : List.of();
            this.definition = definition;
            this.relevance = relevance;
            this.related = related != null ? List.copyOf(related) : List.of();
        }
    }

    private static final Glossary INSTANCE = load();

    private final List<Entry> entries;
    /** Maps lowercase term/alias to its canonical Entry. */
    private final Map<String, Entry> lookup;

    private Glossary(List<Entry> entries) {
        List<Entry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparing(e -> e.term().toLowerCase(Locale.ROOT)));
        this.entries = Collections.unmodifiableList(sorted);

        Map<String, Entry> map = new LinkedHashMap<>();
        for (Entry entry : this.entries) {
            map.put(entry.term().toLowerCase(Locale.ROOT), entry);
            for (String alias : entry.aliases()) {
                map.putIfAbsent(alias.toLowerCase(Locale.ROOT), entry);
            }
        }
        this.lookup = Collections.unmodifiableMap(map);
    }

    /**
     * Returns the singleton glossary instance.
     */
    public static Glossary instance() {
        return INSTANCE;
    }

    /**
     * Returns all entries in alphabetical order by term.
     */
    public List<Entry> entries() {
        return entries;
    }

    /**
     * Looks up an entry by its term or any alias (case-insensitive).
     */
    public Optional<Entry> lookup(String termOrAlias) {
        if (termOrAlias == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(lookup.get(termOrAlias.toLowerCase(Locale.ROOT)));
    }

    /**
     * Returns all term names and aliases that map to entries (lowercase keys).
     */
    public Map<String, Entry> lookupMap() {
        return lookup;
    }

    /**
     * Searches entries for a query string, matching against term, aliases, and definition.
     * Returns entries in relevance order: term match first, then alias match, then definition match.
     */
    public List<Entry> search(String query) {
        if (query == null || query.isBlank()) {
            return entries;
        }
        String q = query.toLowerCase(Locale.ROOT).trim();
        List<Entry> termMatches = new ArrayList<>();
        List<Entry> aliasMatches = new ArrayList<>();
        List<Entry> definitionMatches = new ArrayList<>();

        for (Entry entry : entries) {
            if (entry.term().toLowerCase(Locale.ROOT).contains(q)) {
                termMatches.add(entry);
            } else if (entry.aliases().stream()
                    .anyMatch(a -> a.toLowerCase(Locale.ROOT).contains(q))) {
                aliasMatches.add(entry);
            } else if (entry.definition().toLowerCase(Locale.ROOT).contains(q)
                    || entry.relevance().toLowerCase(Locale.ROOT).contains(q)) {
                definitionMatches.add(entry);
            }
        }

        List<Entry> result = new ArrayList<>(termMatches.size()
                + aliasMatches.size() + definitionMatches.size());
        result.addAll(termMatches);
        result.addAll(aliasMatches);
        result.addAll(definitionMatches);
        return Collections.unmodifiableList(result);
    }

    private static Glossary load() {
        try (InputStream in = Glossary.class.getResourceAsStream("/sd-glossary.json")) {
            if (in == null) {
                throw new IllegalStateException("sd-glossary.json not found on classpath");
            }
            ObjectMapper mapper = new ObjectMapper();
            Entry[] entries = mapper.readValue(in, Entry[].class);
            return new Glossary(List.of(entries));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load glossary", e);
        }
    }
}
