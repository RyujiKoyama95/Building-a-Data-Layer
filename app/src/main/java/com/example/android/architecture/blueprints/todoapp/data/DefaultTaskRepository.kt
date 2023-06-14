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

package com.example.android.architecture.blueprints.todoapp.data

import com.example.android.architecture.blueprints.todoapp.data.source.local.TaskDao
import com.example.android.architecture.blueprints.todoapp.data.source.local.toExternal
import com.example.android.architecture.blueprints.todoapp.data.source.local.toNetwork
import com.example.android.architecture.blueprints.todoapp.data.source.network.TaskNetworkDataSource
import com.example.android.architecture.blueprints.todoapp.data.source.network.toLocal
import com.example.android.architecture.blueprints.todoapp.di.ApplicationScope
import com.example.android.architecture.blueprints.todoapp.di.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * Task Repository
 *
 * リポジトリでは、データを得るためのデータソースの利用は(複数あっても)1つにする
 * TaskDao.observeAll()ではLocalTaskが取得できるが、
 * LocalTaskモデルは他のアーキテクチャに公開する必要はないため、
 * 取得するLocalTaskはTaskに変換する。
 */
class DefaultTaskRepository @Inject constructor(
    private val localDataSource: TaskDao,
    private val networkDataSource: TaskNetworkDataSource,
    // Todo: dispatcherとInjectがまだよくわからんので確認
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
    @ApplicationScope private val scope: CoroutineScope
) {
    companion object {
        const val COMPLETED = true
    }
    fun observeAll(): Flow<List<Task>> {
        return localDataSource.observeAll().map { tasks ->
            tasks.toExternal()
        }
    }

    // TaskDao.upsert()を使うため、Task→LocalTaskへ変換
    suspend fun create(title: String, description: String): String {
        // withContext(Dispatcher.Default)は引数に指定されたディスパッチャにコンテキストを切り替える
        // (新たにコンテキストを生成するわけではないので、メモリを食いづらい)
        val taskId = withContext(dispatcher) {
            createTaskId()
        }
        val task = Task(
            title = title,
            description = description,
            id = taskId
        )
        localDataSource.upsert(task.toLocal())
        savedTaskToNetwork()
        return taskId
    }

    suspend fun complete(taskId: String) {
        localDataSource.updateCompleted(taskId, COMPLETED)
        savedTaskToNetwork()
    }

    suspend fun refresh() {
        val networkTask = networkDataSource.loadTasks()
        localDataSource.deleteAll()
        // toLocal()のマッピング処理はタスク数が多数になり計算コストが高くなる可能性があるため、
        // withContext()を利用
        val localTask = withContext(dispatcher) {
            networkTask.toLocal()
        }
        localDataSource.upsertAll(localTask)
    }

    private suspend fun savedTaskToNetwork() {
        scope.launch {
            val localTasks = localDataSource.observeAll().first()
            val networkTasks = withContext(dispatcher) {
                localTasks.toNetwork()
            }
            networkDataSource.saveTasks(networkTasks)
        }
    }


    private fun createTaskId(): String {
        return UUID.randomUUID().toString()
    }
}