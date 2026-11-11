package cat.receptari.app.domain.model

/**
 * A curated set of pictograms a folder can be marked with — not a free image upload. Declared in
 * the order the picker shows them: general, savoury, then sweet and drink. Stored by name, so a
 * constant is never renamed or removed.
 */
enum class FolderIcon {
    FOLDER,
    RESTAURANT,
    FAMILY,
    CELEBRATION,
    VEGETARIAN,
    BREAKFAST,

    SOUP,
    RAMEN,
    PIZZA,
    LUNCH,
    GRILL,
    SEAFOOD,

    BAKERY,
    CAKE,
    COOKIE,
    ICE_CREAM,
    COFFEE,
    WINE,
}
