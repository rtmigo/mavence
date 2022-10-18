//import com.github.pgreze.process.InputSource
//import com.github.pgreze.process.Redirect
//import com.github.pgreze.process.process
//import java.io.File
//import java.nio.charset.Charset
//
///** The same as [com.github.pgreze.process.process], but does NOT print anything to `stdout`. It
// * redirects both `stderr` and `stdout` to `stderr`, and also captures them independently. */
//@Deprecated("Oops")
//suspend fun runClean(
//    vararg command: String,
//    stdin: InputSource? = null,
//    charset: Charset = Charsets.UTF_8,
//    print: Boolean = true,
//    /** Extend with new environment variables during this process's invocation. */
//    env: Map<String, String>? = null,
//    /** Override the process working directory. */
//    directory: File? = null,
//    /** Determine if process should be destroyed forcibly on job cancellation. */
//    destroyForcibly: Boolean = false,
//    /** Consume without delay all streams configured with [Redirect.CAPTURE]. */
//    consumer: suspend (String) -> Unit = {},
//    checkCode: Boolean = true,
//): ProcessResultEx {
//    val stdoutLines = mutableListOf<String>()
//    val stderrLines = mutableListOf<String>()
//    val outputLines = mutableListOf<String>()
//    val procResult = process(
//        command = command,
//        stdin = stdin,
////        stdout = Redirect.Consume { flow ->
////            flow.collect {
////                stdoutLines.add(it)
////                outputLines.add(it)
////                //if (print)
////                //    System.err.println(it)
////            }
////        },
////        stderr = Redirect.Consume { flow ->
////            flow.collect {
////                stderrLines.add(it)
////                outputLines.add(it)
////                if (print)
////                    System.err.println(it)
////            }
////        },
//        charset = charset,
//        env = env,
//        directory = directory,
//        destroyForcibly = destroyForcibly,
//        consumer = consumer
//    )
//    if (checkCode && procResult.resultCode != 0)
//        throw Exception("Result code is ${procResult.resultCode}")
//    return ProcessResultEx(
//        output = outputLines,
//        stdout = stdoutLines,
//        stderr = stderrLines,
//        resultCode = procResult.resultCode
//    )
//}
//
//data class ProcessResultEx(
//    val output: List<String>,
//    val stdout: List<String>,
//    val stderr: List<String>,
//    val resultCode: Int,
//) {
//    //val output: String by lazy { outputB.toString() }
//    //val stdout: String by lazy { stdoutB.toString() }
//    //val stderr: String by lazy { stderrB.toString() }
//}