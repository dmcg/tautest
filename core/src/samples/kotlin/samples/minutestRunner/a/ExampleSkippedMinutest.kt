// explicit name for generated class so that tests are not coupled to the behaviour of the kotlin compiler
@file:JvmName("ExampleSkippedMinutest")

package samples.minutestRunner.a

import org.junit.jupiter.api.Assertions.fail
import uk.org.minutest.experimental.SKIP
import uk.org.minutest.rootContext


fun `example skipped context`() = SKIP - rootContext<Unit> {
    test("skip is honoured") {
        fail("skip wasn't honoured")
    }
}

