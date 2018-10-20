package com.oneeyedmen.minutest

import com.oneeyedmen.minutest.junit.junitTests
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.TestFactory


object NullableFixtureTests {

    @TestFactory fun nullableFixture() = junitTests<String?> {

        context("with null fixture") {
            fixture { null }
            test("fixture is null") {
                assertNull(this)
            }
        }

        context("with non-null fixture") {
            fixture { "banana" }
            test("fixture is not null") {
                val copy: String = this ?: "was null"
                assertEquals("banana", copy)

            }
        }
    }
}