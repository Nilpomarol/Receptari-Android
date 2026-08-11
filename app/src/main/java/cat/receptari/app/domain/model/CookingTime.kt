package cat.receptari.app.domain.model

/**
 * Which of the three stored durations to print when there is room for only one.
 *
 * A recipe can record prep time, cook time and a total, and sources fill in whichever
 * combination they feel like — a website with JSON-LD often gives all three, a photographed
 * page usually gives one. The library card has room for a single figure, so the choice has
 * to be made somewhere; making it here keeps it out of the UI and under test.
 *
 * A stored total always wins, even when it disagrees with the sum of the parts. Recipes
 * legitimately say "1 h 30 min total" over steps that add to less, because resting, cooling
 * and marinating are time you wait rather than time you work. Quietly replacing the number
 * the user typed with arithmetic would be the app deciding it knows better.
 */
object CookingTime {

    /**
     * The whole time, from whatever is recorded: a stored total, else prep plus cook, else
     * whichever single figure exists.
     */
    fun effective(total: Int?, prep: Int?, cook: Int?): Int? =
        total ?: derivedTotal(prep, cook) ?: cook ?: prep

    /**
     * The total implied by the parts, or null.
     *
     * Narrower than [effective] on purpose: a lone cook time is the whole time, but it is
     * not a *total*, and printing "Total 30 min" under "Cocció 30 min" says nothing twice.
     */
    fun derivedTotal(prep: Int?, cook: Int?): Int? =
        if (prep != null && cook != null) prep + cook else null
}
