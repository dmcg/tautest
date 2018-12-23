package com.oneeyedmen.minutest.internal

import com.oneeyedmen.minutest.Named
import com.oneeyedmen.minutest.RuntimeContext
import com.oneeyedmen.minutest.Test
import com.oneeyedmen.minutest.TestDescriptor

interface TestExecutor<F> : Named {

    fun runTest(test: Test<F>, testDescriptor: TestDescriptor)

    fun <G> andThen(nextContext: RuntimeContext<F, G>): TestExecutor<G> = object: TestExecutor<G> {
        override val name = nextContext.name
        override val parent = this@TestExecutor

        override fun runTest(test: Test<G>, testDescriptor: TestDescriptor) {
            // NB use the top testDescriptor so that we always see the longest path - the one from the
            // bottom of the stack.
            val testForParent: Test<F> = { fixture, _ ->
                nextContext.runTest(test, fixture, testDescriptor)
                fixture
            }
            return parent.runTest(testForParent, testDescriptor)
        }
    }
}

internal object RootContext : TestExecutor<Unit> {
    override val name = ""
    override val parent: Nothing? = null
    override fun runTest(test: Test<Unit>, testDescriptor: TestDescriptor): Unit = test(Unit, testDescriptor)
}