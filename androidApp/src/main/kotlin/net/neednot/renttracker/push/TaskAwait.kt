package net.neednot.renttracker.push

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal suspend fun <T> Task<T>.awaitResult(): Result<T, String> =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            continuation.resume(
                if (task.isSuccessful) Ok(task.result) else Err(task.exception?.message ?: "Task was cancelled"),
            )
        }
    }
