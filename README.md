# minutest

[ ![Download](https://api.bintray.com/packages/dmcg/oneeyedmen-mvn/minutest/images/download.svg) ](https://bintray.com/dmcg/oneeyedmen-mvn/minutest/_latestVersion)

Minutest brings Spec-style testing to JUnit 5 and Kotlin.

## Installation
Life is too short to jump through the hoops to Maven Central, but you can pick up builds on [JCenter](https://bintray.com/dmcg/oneeyedmen-mvn/minutest)

## Usage

minutest See [ExampleTests](src/test/kotlin/com/oneeyedmen/minutest/ExampleTests.kt), viz:

```kotlin
package com.oneeyedmen.minutest

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import java.util.*


object ExampleTests {

    // In the simplest case, make the fixture the thing that you are testing
    @TestFactory fun `stack is our fixture`() = context<Stack<String>> {

        // define the fixture for enclosed scopes
        fixture { Stack() }

        context("an empty stack") {

            test("is empty") {
                assertEquals(0, size) // note that the fixture is 'this'
                assertThrows<EmptyStackException> { peek() }
            }

            test("can have an item pushed") {
                push("one")
                assertEquals("one", peek())
                assertEquals(1, size)
            }
        }

        context("a stack with one item") {

            // we can modify the outer fixture
            modifyFixture { push("one") }

            test("is not empty") {
                assertEquals(1, size)
                assertEquals("one", peek())
            }

            test("removes and returns item on pop") {
                assertEquals("one", pop())
                assertEquals(0, size)
            }
        }
    }

    // If you have more state, make a separate fixture class
    class Fixture {
        val stack1 = Stack<String>()
        val stack2 = Stack<String>()
    }

    // and then use it in your tests
    @TestFactory fun `separate fixture class`() = context<Fixture> {

        fixture { Fixture() }

        context("stacks with no items") {
            test("error to try to swap") {
                assertThrows<EmptyStackException> {
                    stack1.swapTop(stack2)
                }
            }
        }

        context("stacks with items") {
            modifyFixture {
                stack1.push("on 1")
                stack2.push("on 2")
            }

            test("swap top items") {
                stack1.swapTop(stack2)
                assertEquals("on 2", stack1.peek())
                assertEquals("on 1", stack2.peek())
            }
        }
    }

    // You can modify the fixture before, and inspect it after
    @TestFactory fun `before and after`() = context<Fixture> {
        fixture { Fixture() }

        before {
            stack1.push("on 1")
        }

        before {
            stack2.push("on 2")
        }

        after {
            println("in after")
            assertTrue(stack1.isEmpty())
        }

        test("before was called") {
            assertEquals("on 1", stack1.peek())
            assertEquals("on 2", stack2.peek())
            stack1.pop()
        }
    }

    // You can also work with an immutable fixture
    @TestFactory fun `immutable fixture`() = context<List<String>> {
        fixture { emptyList() }

        // test_ allows you to return the fixture
        test_("add an item and return the fixture") {
            val newList = this + "item"
            assertEquals("item", newList.first())
            newList
        }

        // which will be available for inspection in after
        after {
            println("in after")
            assertEquals("item", first())
        }

        // there is also before_ and after_ which return new fixtures
    }
}

private fun <E> Stack<E>.swapTop(otherStack: Stack<E>) {
    val myTop = pop()
    push(otherStack.pop())
    otherStack.push(myTop)
}
```

## More Advanced Use

The key to minutest is that by separating the fixture from the test code, both are made available to manipulate as data. 

So if you want to reuse the same test for different concrete implementations, define the test with a function and call it for subclasses.

```kotlin
package com.oneeyedmen.minutest

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.TestFactory
import java.util.*


// To run the same tests against different implementations, first define a function taking the implementation and
// returning a TestContext
fun TestContext<MutableCollection<String>>.behavesAsMutableCollection(
    collectionName: String,
    factory: () -> MutableCollection<String>
) {
    context("check $collectionName") {

        fixture { factory() }

        test("is empty") {
            assertTrue(isEmpty())
        }

        test("can add") {
            add("item")
            assertEquals("item", first())
        }
    }
}

// Now tests can invoke the function to define a context to be run

object ArrayListTests {
    @TestFactory fun tests() = context<MutableCollection<String>> {
        behavesAsMutableCollection("ArrayList") { ArrayList() }
    }
}

object LinkedListTets{
    @TestFactory fun tests() = context<MutableCollection<String>> {
        behavesAsMutableCollection("LinkedList") { LinkedList() }
    }
}
```

Unleash the power of Kotlin to generate your tests on the fly.

```kotlin
package com.oneeyedmen.minutest

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import java.util.*


private typealias StringStack = Stack<String>

object GeneratingExampleTests {

    // We can define functions that return tests for later injection

    private fun TestContext<StringStack>.isEmpty(isEmpty: Boolean) =
        test("is " + (if (isEmpty) "" else "not ") + "empty") {
            assertEquals(isEmpty, size == 0)
            if (isEmpty)
                assertThrows<EmptyStackException> { peek() }
            else
                assertNotNull(peek())
        }

    private fun TestContext<StringStack>.canPush() =
        test("can push") {
            val initialSize = size
            val item = "*".repeat(initialSize + 1)
            push(item)
            assertEquals(item, peek())
            assertEquals(initialSize + 1, size)
        }

    private fun TestContext<StringStack>.canPop() =
        test("can pop") {
            val initialSize = size
            val top = peek()
            assertEquals(top, pop())
            assertEquals(initialSize - 1, size)
            if (size > 0)
                assertNotEquals(top, peek())
        }

    private fun TestContext<StringStack>.cantPop() =
        test("cant pop") {
            assertThrows<EmptyStackException> { pop() }
        }

    @TestFactory fun `invoke functions to inject tests`() = context<StringStack> {

        fixture { StringStack() }

        context("an empty stack") {
            isEmpty(true)
            canPush()
            cantPop()
        }

        context("a stack with one item") {
            modifyFixture { push("one") }

            isEmpty(false)
            canPush()

            test("has the item on top") {
                assertEquals("one", pop())
            }

            canPop()
        }
    }

    @TestFactory fun `generate contexts to test with multiple values`() = context<StringStack> {

        fun TestContext<StringStack>.canPop(canPop: Boolean) = if (canPop) canPop() else cantPop()

        (0..3).forEach { itemCount ->
            context("stack with $itemCount items") {

                fixture {
                    StringStack().apply {
                        (1..itemCount).forEach { add(it.toString()) }
                    }
                }

                isEmpty(itemCount == 0)
                canPush()
                canPop(itemCount > 0)
            }
        }
    }
}
```