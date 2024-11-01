package com.sd.lib.keyedstate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FKeyedState<T> {
   private val _flows: MutableMap<String, FlowHolder<T>> = mutableMapOf()
   private val _dispatcher = runCatching { Dispatchers.Main.immediate }.getOrDefault(Dispatchers.Main)
   private val _scope = CoroutineScope(SupervisorJob() + _dispatcher)

   fun emit(key: String, state: T) {
      _scope.launch {
         val holder = _flows.getOrPut(key) {
            FlowHolder(key = key, releaseAble = false)
         }
         holder.releaseAble = false
         holder.flow.emit(state)
      }
   }

   fun release(key: String) {
      _scope.launch {
         _flows[key]?.let { holder ->
            holder.releaseAble = true
            holder.release()
         }
      }
   }

   suspend fun collect(
      key: String,
      block: suspend (T) -> Unit,
   ) {
      withContext(_dispatcher) {
         val holder = _flows.getOrPut(key) {
            FlowHolder(key = key, releaseAble = true)
         }
         try {
            holder.flow
               .distinctUntilChanged()
               .collect { block(it) }
         } finally {
            holder.release()
         }
      }
   }

   private inner class FlowHolder<T>(
      private val key: String,
      var releaseAble: Boolean,
   ) {
      val flow: MutableSharedFlow<T> = MutableSharedFlow(replay = 1)

      fun release() {
         if (releaseAble && flow.subscriptionCount.value == 0) {
            _flows.remove(key)
         }
      }
   }
}