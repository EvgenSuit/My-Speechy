package com.example.myspeechy

import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ExampleUnitTest {
    private val email = "some@gmail.com"
    private val password = "somePassword"
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

}