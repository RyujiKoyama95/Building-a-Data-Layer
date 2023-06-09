/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.architecture.blueprints.todoapp.data.source.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class TaskNetworkDataSource @Inject constructor() {
    private val accessMutex = Mutex()
    private var networkTasks = listOf<NetworkTask>(
        NetworkTask(
            "1",
            "task1",
            "aaa",
            1
        ),
        NetworkTask(
            "2",
            "task2",
            "bbb",
            2
        )
    )

    // Mutex().withLock{}で排他制御
    // withLock{}のブロック内の処理は１つのスレッドでのみ動く、競合が発生している場合は、待つようになる
    suspend fun loadTasks(): List<NetworkTask> = accessMutex.withLock {
        delay(2000L)
        return networkTasks
    }

    suspend fun saveTasks(newTasks: List<NetworkTask>) = accessMutex.withLock {
        delay(2000L)
        networkTasks = newTasks
    }
}