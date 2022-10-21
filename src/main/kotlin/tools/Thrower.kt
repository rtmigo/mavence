package tools

inline fun <R> rethrowingState(
    error: () -> String,
    call: () -> R,
): R {
    try {
        return call()
    } catch (e: Exception) {
        throw IllegalStateException(error(), e)
    }
}