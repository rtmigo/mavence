package tools

import java.lang.IllegalStateException

//inline fun <R> rethrowing(
//    error: () -> Throwable,
//    call: () -> R,
//): R {
//    try {
//        return call()
//    } catch (e: Throwable) {
//        throw error()
//    }
//}

inline fun <R> rethrowingState(
    error: () -> String,
    call: () -> R,
): R {
    try {
        return call()
    } catch (e: Throwable) {
        throw IllegalStateException(error(), e)
    }
}