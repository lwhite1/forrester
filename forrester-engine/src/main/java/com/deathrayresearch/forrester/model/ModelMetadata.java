package com.deathrayresearch.forrester.model;

/**
 * Attribution and licensing metadata for a {@link Model}.
 *
 * <p>All fields are optional — use the {@link Builder} to set only what applies.
 *
 * @param author  the model author or authors
 * @param source  bibliographic reference or origin (e.g. "Abdel-Hamid &amp; Madnick (1991)")
 * @param license the license under which the model is distributed (e.g. "AGPL-3.0")
 * @param url     a URL for the original publication or source material
 */
public record ModelMetadata(
        String author,
        String source,
        String license,
        String url
) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String author;
        private String source;
        private String license;
        private String url;

        private Builder() {
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder license(String license) {
            this.license = license;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public ModelMetadata build() {
            return new ModelMetadata(author, source, license, url);
        }
    }
}
