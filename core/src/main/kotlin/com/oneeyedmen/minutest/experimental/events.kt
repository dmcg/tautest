package com.oneeyedmen.minutest.experimental

import com.oneeyedmen.minutest.*
import com.oneeyedmen.minutest.internal.RuntimeContextWrapper
import org.opentest4j.IncompleteExecutionException
import org.opentest4j.TestAbortedException
import org.opentest4j.TestSkippedException


interface TestEventListener {
    fun <F> testStarting(fixture: F, testDescriptor: TestDescriptor) {}
    fun <F> testComplete(fixture: F, testDescriptor: TestDescriptor) {}
    fun <F> testSkipped(fixture: F, testDescriptor: TestDescriptor, t: IncompleteExecutionException) {}
    fun <F> testAborted(fixture: F, testDescriptor: TestDescriptor, t: TestAbortedException) {}
    fun <F> testFailed(fixture: F, testDescriptor: TestDescriptor, t: Throwable) {}
    fun <PF, F> contextClosed(context: Context<PF, F>) {}
}

class Telling(private val listener: TestEventListener) : TestAnnotation, NodeTransform {
    override fun <F> applyTo(node: Node<F>): Node<F> = node.telling(listener)
}

fun <F> telling(listener: TestEventListener): (Node<F>) -> Node<F> = { context ->
    context.telling(listener)
}

private fun <PF, F> Context<PF, F>.telling(listener: TestEventListener): Context<PF, F> =
    RuntimeContextWrapper(this,
        children = children.map { it.telling(listener) },
        onClose = { listener.contextClosed(this@telling) }
    )

private fun <F> Test<F>.telling(listener: TestEventListener) = copy(
    f = { fixture, testDescriptor ->
        listener.testStarting(fixture, testDescriptor)
        try {
            this(fixture, testDescriptor).also {
                listener.testComplete(fixture, testDescriptor)
            }
        } catch (skipped: TestSkippedException) {
            listener.testSkipped(fixture, testDescriptor, skipped)
            throw skipped
        } catch (skipped: MinutestSkippedException) {
            listener.testSkipped(fixture, testDescriptor, skipped)
            throw skipped
        } catch (aborted: TestAbortedException) {
            listener.testAborted(fixture, testDescriptor, aborted)
            throw aborted
        } catch (t: Throwable) {
            listener.testFailed(fixture, testDescriptor, t)
            throw t
        }
    }
)

private fun <F> Node<F>.telling(listener: TestEventListener): Node<F> =
    when (this) {
        is Test<F> -> this.telling(listener)
        is Context<F, *> -> this.telling(listener)
    }

