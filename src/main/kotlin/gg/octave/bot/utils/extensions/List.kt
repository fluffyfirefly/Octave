package gg.octave.bot.utils.extensions

fun <T> List<T>.section(): Pair<T, List<T>> = Pair(first(), drop(1))

fun <T> List<T>?.sectionOrNull(): Pair<T?, List<T>?> = this?.section() ?: Pair(null, null)

fun <T> Iterable<T>.iterate(range: IntRange) = sequence {
    for (i in range.first until range.last) {
        yield(Pair(i, this@iterate.elementAt(i)))
    }
}
