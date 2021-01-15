package dev.minutest.experimental

import dev.minutest.Context
import dev.minutest.Node
import dev.minutest.Test
import dev.minutest.TestDescriptor
import dev.minutest.experimental.TestLogger.EventType.*
import org.opentest4j.IncompleteExecutionException
import org.opentest4j.TestAbortedException

class TestLogger(
    private val log: MutableList<String> = mutableListOf(),
    val indent: String = "  ",
    val prefixer: (EventType) -> String = EventType::prefix
) : TestEventListener {

    val events = mutableListOf<TestEvent>()

    enum class EventType(val prefix: String) {
        CONTEXT_OPENED("▾ "),
        TEST_STARTING("⋯ "),
        TEST_COMPLETE("✓ "),
        TEST_FAILED("X "),
        TEST_ABORTED("- "),
        TEST_SKIPPED("- "),
        CONTEXT_CLOSED("▴ "),
    }

    companion object {
        val noSymbols: (EventType) -> String = { "" }
    }

    private var currentIndent = ""

    override fun <PF, F> contextOpened(context: Context<PF, F>, testDescriptor: TestDescriptor) {
        addAndLog(TestEvent(CONTEXT_OPENED, context, testDescriptor))
    }

    override fun <F> testStarting(test: Test<F>, fixture: F, testDescriptor: TestDescriptor) {
        add(TestEvent(TEST_STARTING, test, testDescriptor))
    }

    override fun <F> testComplete(test: Test<F>, fixture: F, testDescriptor: TestDescriptor) {
        addAndLog(TestEvent(TEST_COMPLETE, test, testDescriptor))
    }

    override fun <F> testFailed(test: Test<F>, fixture: F, testDescriptor: TestDescriptor, t: Throwable) {
        addAndLog(TestEvent(TEST_FAILED, test, testDescriptor))
    }

    override fun <F> testAborted(test: Test<F>, fixture: F, testDescriptor: TestDescriptor, t: TestAbortedException) {
        addAndLog(TestEvent(TEST_ABORTED, test, testDescriptor))
    }

    override fun <F> testSkipped(test: Test<F>, fixture: F, testDescriptor: TestDescriptor, t: IncompleteExecutionException) {
        addAndLog(TestEvent(TEST_SKIPPED, test, testDescriptor))
    }

    override fun <PF, F> contextClosed(context: Context<PF, F>, testDescriptor: TestDescriptor) {
        currentIndent = currentIndent.substring(indent.length)
        add(TestEvent(CONTEXT_CLOSED, context, testDescriptor))
    }

    private fun addAndLog(testEvent: TestEvent) {
        add(testEvent)
        log(testEvent)
    }

    private fun add(testEvent: TestEvent) {
        events.add(testEvent)
    }

    private fun log(testEvent: TestEvent) {
        val (eventType, _, testDescriptor) = testEvent
        log.add(currentIndent + prefixer(eventType) + testDescriptor.name)
        if (eventType == CONTEXT_OPENED)
            currentIndent += indent
    }

    data class TestEvent(
        val eventType: EventType,
        val node: Node<*>,
        val testDescriptor: TestDescriptor
    )

    fun toStrings(): List<String> {
        var currentIndent = ""
        return events.mapNotNull { (eventType, _, testDescriptor) ->
            when (eventType) {
                CONTEXT_OPENED -> (currentIndent + prefixer(eventType) + testDescriptor.name).also {
                    currentIndent += indent
                }
                CONTEXT_CLOSED -> {
                    currentIndent = currentIndent.substring(indent.length)
                    null
                }
                TEST_STARTING -> null
                else -> (currentIndent + prefixer(eventType) + testDescriptor.name)
            }

        }
    }
}