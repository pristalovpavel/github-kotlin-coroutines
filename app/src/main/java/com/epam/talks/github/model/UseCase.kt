package com.epam.talks.github.model

import android.util.Log
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * Abstract class for a Use Case (Interactor in terms of Clean Architecture).
 * This interface represents a execution unit for different use cases (this means any use case
 * in the application should implement this contract).
 * <p>
 * By convention each UseCase implementation will return the result using a coroutine
 * that will execute its job in a runAsync thread and will post the result in the UI thread.
 */
typealias CompletionBlock<T> = UseCase.Request<T>.() -> Unit

abstract class UseCase<T> {

    private var parentJob: Job = Job()
    var backgroundContext: CoroutineContext = Dispatchers.IO
    var foregroundContext: CoroutineContext = Dispatchers.Main

    protected abstract suspend fun executeOnBackground(): T

    fun execute(block: CompletionBlock<T>) {
        val response = Request<T>().apply { block() }
        unsubscribe()
        parentJob = Job()
        CoroutineScope(foregroundContext + parentJob).launch {
            try {
                val result = withContext(backgroundContext) {
                    executeOnBackground()
                }
                response(result)
            } catch (cancellationException: CancellationException) {
                Log.d("UseCase", "canceled by user")
                response(cancellationException)
            } catch (e: Exception) {
                Log.e("UseCase", "exception", e)
                response(e)
            }
        }
    }

    protected suspend fun <X> runAsync(context: CoroutineContext = backgroundContext, block: suspend () -> X): Deferred<X> {
        return CoroutineScope(context + parentJob).async {
            block.invoke()
        }
    }

    fun unsubscribe() {
        parentJob.apply {
            cancelChildren()
            cancel()
        }
    }


    class Request<T> {
        private var onComplete: ((T) -> Unit)? = null
        private var onError: ((Throwable) -> Unit)? = null
        private var onCancel: ((CancellationException) -> Unit)? = null

        fun onComplete(block: (T) -> Unit) {
            onComplete = block
        }

        fun onError(block: (Throwable) -> Unit) {
            onError = block
        }

        fun onCancel(block: (CancellationException) -> Unit) {
            onCancel = block
        }

        operator fun invoke(result: T) {
            onComplete?.let {
                it.invoke(result)
            }
        }

        operator fun invoke(error: Throwable) {
            onError?.let {
                it.invoke(error)
            }
        }

        operator fun invoke(error: CancellationException) {
            onCancel?.let {
                it.invoke(error)
            }
        }
    }
}