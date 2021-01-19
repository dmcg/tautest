package dev.minutest.experimental

import dev.minutest.Context
import dev.minutest.Node
import dev.minutest.Test
import dev.minutest.TestDescriptor
import dev.minutest.experimental.TestLogger.EventType.*
import org.opentest4j.IncompleteExecutionException
import org.opentest4j.TestAbortedException
import java.util.Collections.synchronizedList

class TestLogger(
    val prefixer: (EventType) -> String = EventType::prefix
) : TestEventListener {

    private val events: MutableList<TestEvent> = synchronizedList(mutableListOf<TestEvent>())

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

    override fun <PF, F> contextOpened(context: Context<PF, F>, testDescriptor: TestDescriptor) {
        add(TestEvent(CONTEXT_OPENED, context, testDescriptor))
    }

    override fun <F> testStarting(test: Test<F>, fixture: F, testDescriptor: TestDescriptor) {
        add(TestEvent(TEST_STARTING, test, testDescriptor))
    }

    override fun <F> testComplete(test: Test<F>, fixture: F, testDescriptor: TestDescriptor) {
        add(TestEvent(TEST_COMPLETE, test, testDescriptor))
    }

    override fun <F> testFailed(test: Test<F>, fixture: F, testDescriptor: TestDescriptor, t: Throwable) {
        add(TestEvent(TEST_FAILED, test, testDescriptor))
    }

    override fun <F> testAborted(test: Test<F>, fixture: F, testDescriptor: TestDescriptor, t: TestAbortedException) {
        add(TestEvent(TEST_ABORTED, test, testDescriptor))
    }

    override fun <F> testSkipped(
        test: Test<F>,
        fixture: F,
        testDescriptor: TestDescriptor,
        t: IncompleteExecutionException
    ) {
        add(TestEvent(TEST_SKIPPED, test, testDescriptor))
    }

    override fun <PF, F> contextClosed(context: Context<PF, F>, testDescriptor: TestDescriptor) {
        add(TestEvent(CONTEXT_CLOSED, context, testDescriptor))
    }

    private fun add(testEvent: TestEvent) {
        events.add(testEvent)
    }

    data class TestEvent(
        val eventType: EventType,
        val node: Node<*>,
        val testDescriptor: TestDescriptor
    )

    fun toStrings(): List<String> {
        return events.mapNotNull { (eventType, _, testDescriptor) ->
            when (eventType) {
                CONTEXT_OPENED -> (prefixer(eventType) + testDescriptor.pathAsString())
                CONTEXT_CLOSED -> null
                TEST_STARTING -> null
                else -> (prefixer(eventType) + testDescriptor.pathAsString())
            }
        }
    }
}