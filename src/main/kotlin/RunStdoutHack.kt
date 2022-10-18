import com.github.pgreze.process.InputSource
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import java.io.File
import java.nio.charset.Charset

/** The same as [com.github.pgreze.process.process], but does NOT print anything to `stdout`. It
 * redirects both `stderr` and `stdout` to `stderr`, and also captures them independently. */
suspend fun runClean(
    vararg command: String,
    stdin: InputSource? = null,
    charset: Charset = Charsets.UTF_8,
    /** Extend with new environment variables during this process's invocation. */
    env: Map<String, String>? = null,
    /** Override the process working directory. */
    directory: File? = null,
    /** Determine if process should be destroyed forcibly on job cancellation. */
    destroyForcibly: Boolean = false,
    /** Consume without delay all streams configured with [Redirect.CAPTURE]. */
    consumer: suspend (String) -> Unit = {},
    checkCode: Boolean = true
): ProcessResultEx {
    val stdoutBuilder = StringBuilder()
    val stderrBuilder = StringBuilder()
    val outputBuilder = StringBuilder()
    val procResult = process(
        command = command,
        stdin = stdin,
        stdout = Redirect.Consume { flow ->
            flow.collect {
                stdoutBuilder.append(it)
                outputBuilder.append(it)
                System.err.println(it)
            }
        },
        stderr = Redirect.Consume { flow ->
            flow.collect {
                stderrBuilder.append(it)
                outputBuilder.append(it)
                System.err.println(it)
            }
        },
        charset = charset,
        env = env,
        directory = directory,
        destroyForcibly = destroyForcibly,
        consumer = consumer
    )
    if (checkCode && procResult.resultCode != 0)
        throw Exception("Result code is ${procResult.resultCode}")
    return ProcessResultEx(
        outputB = outputBuilder,
        stdoutB = stdoutBuilder,
        stderrB = stderrBuilder,
        resultCode = procResult.resultCode
    )
}

data class ProcessResultEx(
    private val outputB: StringBuilder,
    private val stdoutB: StringBuilder,
    private val stderrB: StringBuilder,
    val resultCode: Int
) {
    val output: String by lazy { outputB.toString() }
    val stdout: String by lazy { stdoutB.toString() }
    val stderr: String by lazy { stderrB.toString() }
}