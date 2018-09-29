package com.oneeyedmen.minutest.junit

import com.oneeyedmen.minutest.*
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.stream.Stream
import kotlin.reflect.KProperty1
import kotlin.streams.asStream

fun <F> junitTests(builder: TestContext<F>.() -> Unit): Stream<out DynamicNode> =
    (rootContext("ignored", builder) as MiContext<F>).build(Operations<F>()).children

// These are defined as extensions to avoid taking a dependency on JUnit in the main package

private fun <F> MiContext<F>.build(parentOperations: Operations<F>): DynamicContainer = dynamicContainer(name,
    children.asSequence().map { dynamicNodeFor(it, parentOperations) }.asStream())

private fun <F> MiContext<F>.dynamicNodeFor(
    node: Node<F>,
    parentOperations: Operations<F>
) = when (node) {
    is MinuTest<F> -> DynamicTest.dynamicTest(node.name) { runTest(node, parentOperations) }
    is MiContext<F> -> node.build(parentOperations + operations)
    else -> error("Unexpected test node type")
}

fun <T, R: TestRule> TestContext<T>.applyRule(property: KProperty1<T, R>) {
    addTransform { test ->
        MinuTest(test.name) {
            this.also { fixture ->
                val statement = object : Statement() {
                    override fun evaluate() {
                        test(this@MinuTest)
                    }
                }
                property.get(fixture).apply(statement, Description.createTestDescription("GENERATED TEST", name))
            }
        }
    }
}