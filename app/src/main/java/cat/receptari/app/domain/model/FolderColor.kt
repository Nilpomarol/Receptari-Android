package cat.receptari.app.domain.model

/**
 * The label colour a folder is filed under.
 *
 * A fixed, curated set — not a free colour picker — matching the four accent hues already
 * used throughout the app (ratings, favourites, primary and secondary roles), the same way
 * ADR-009 ties every other colour in the book to one hand-picked palette.
 */
enum class FolderColor {
    OLIVE,
    TERRACOTTA,
    GOLD,
    CLARET,
    FOREST,
    INDIGO,
    COCOA,
    SLATE,
}
