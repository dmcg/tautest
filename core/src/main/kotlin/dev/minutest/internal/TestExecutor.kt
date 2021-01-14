package dev.minutest.internal

import dev.minutest.*

/**
 * The TestExecutor is built as the test running infrastructure traverses down the
 * context tree. So at a [Context], there is an executor which has a parent which
 * is an executor for the parent context and so up back to the singleton [RootExecutor].
 *
 * This saves [Context]s knowing their parent (and the difficulty of maintaining the
 * relationship as we add wrapping contexts.
 */
internal interface TestExecutor<F> : TestDescriptor {

    fun runTest(test: Test<F>)

    // Run a testlet, which may be a test, or represent a sub-contexts test
    fun runTest(testDescriptor: TestDescriptor, testlet: Testlet<F>)

    // Compose with a child context to get an executor for that child
    fun <G> andThen(childContext: Context<F, G>): TestExecutor<G> =
        ContextExecutor(this, childContext)

    // Allows executor to know when to close its context
    fun onTestComplete(test: Test<F>)
    fun onContextComplete(context: Context<F, *>)
}

internal class ContextExecutor<PF, F>(
    override val parent: TestExecutor<PF>,
    private val context: Context<PF, F>
) : TestExecutor<F> {

    override val name get() = context.name

    private val incompleteTests: MutableList<Test<F>> =
        context.children.filterIsInstance<Test<F>>().toMutableList()
    private val incompleteContexts: MutableList<Context<F, *>> =
        context.children.filterIsInstance<Context<F, *>>().toMutableList()

    override fun runTest(test: Test<F>) {
        try {
            runTest(this.andThenTestName(test.name), test)
        } finally {
            onTestComplete(test)
        }
    }

    /**
     * The parent context's fixture, before, and after code is invoked by getting the
     * parent executor to run a [Testlet] which in turn asks our context to run a test.
     */
    override fun runTest(
        testDescriptor: TestDescriptor,
        testlet: Testlet<F>
    ) {
        parent.runTest(testDescriptor) { fixture, _ /* see below */ ->
            context.runTest(testlet, fixture, testDescriptor)
            fixture
        }
        // NB use the testDescriptor supplied to this runTest so that we always see
        // the longest path - the one the root to this context.
    }

    override fun onTestComplete(test: Test<F>) {
        incompleteTests.makeSureWeRemove(test)
        maybeClose()
    }

    override fun onContextComplete(context: Context<F, *>) {
        incompleteContexts.makeSureWeRemove(context)
        maybeClose()
    }

    private fun maybeClose() {
        if (incompleteTests.isEmpty() && incompleteContexts.isEmpty()) {
            try {
                context.close()
            } finally {
                parent.onContextComplete(this.context)
            }
        }
    }
}

/**
 * The root executor has no [Context], it just supplies the [Unit] fixture.
 */
internal object RootExecutor : TestExecutor<Unit>, RootDescriptor {
    override val name = ""
    override val parent: Nothing? = null

    override fun runTest(
        testDescriptor: TestDescriptor,
        testlet: Testlet<Unit>
    ): Unit =
        testlet(Unit, testDescriptor)

    override fun runTest(test: Test<Unit>) {
        // this ends up being called if you SKIP the root test,
        // as we substitute the context with a test that throws!
        runTest(this.andThenTestName(test.name), test)
    }

    // Doesn't track any particular context, so doesn't care
    override fun onTestComplete(test: Test<Unit>) = Unit
    override fun onContextComplete(context: Context<Unit, *>) = Unit
}

internal fun TestDescriptor.andThenTestName(name: String): TestDescriptor = object : TestDescriptor {
    override val name = name
    override val parent: TestDescriptor = this@andThenTestName
}

private fun <T> MutableList<T>.makeSureWeRemove(item: T) {
    val wasThere = remove(item)
    check(wasThere) { "Item $item was not in list to be removed" }
}
