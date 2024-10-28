package com.sd.demo.keyedstate

import com.sd.lib.keyedstate.FKeyedState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class KeyedStateTest {
   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   @Test
   fun `test state`() = runTest {
      val state = FKeyedState<TestKeyedState>()
      val count = AtomicInteger()

      val job1 = launch {
         state.collect("") {
            count.incrementAndGet()
         }
      }

      val job2 = launch {
         state.collect("") {
            count.incrementAndGet()
         }
      }

      runCurrent()

      state.emit("", TestKeyedState())
      runCurrent()
      assertEquals(2, count.get())

      job1.cancelAndJoin()
      state.emit("", TestKeyedState())
      runCurrent()
      job2.cancelAndJoin()
      assertEquals(3, count.get())
   }

   @Test
   fun `test state replay`() = runTest {
      val state = FKeyedState<TestKeyedState>()
      val count = AtomicInteger()

      state.emit("", TestKeyedState())
      runCurrent()

      val job = launch {
         state.collect("") {
            count.incrementAndGet()
         }
      }

      runCurrent()
      assertEquals(1, count.get())
      job.cancelAndJoin()
   }

   @Test
   fun `test release`() = runTest {
      val state = FKeyedState<TestKeyedState>()
      val count = AtomicInteger()

      run {
         state.emit("", TestKeyedState())
         state.release("")
         runCurrent()

         launch {
            state.collect("") {
               count.incrementAndGet()
            }
         }.let { job ->
            runCurrent()
            assertEquals(0, count.get())
            job.cancelAndJoin()
         }
      }

      run {
         state.emit("", TestKeyedState())
         runCurrent()

         launch {
            state.collect("") {
               count.incrementAndGet()
            }
         }.let { job ->
            runCurrent()
            state.release("")
            runCurrent()
            assertEquals(1, count.get())
            job.cancelAndJoin()
         }

         launch {
            state.collect("") {
               count.incrementAndGet()
            }
         }.let { job ->
            runCurrent()
            assertEquals(1, count.get())
            job.cancelAndJoin()
         }
      }
   }
}

private class TestKeyedState