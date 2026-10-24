package de.musterversicherung.quote.pricing;

/**
 * Versicherungsart. Der Enum-Name ist der Wert in der API (z. B. "TEILKASKO").
 */
public enum Coverage {
    HAFTPFLICHT("Haftpflicht"),
    TEILKASKO("Teilkasko"),
    VOLLKASKO("Vollkasko");

    private final String displayName;

    Coverage(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
