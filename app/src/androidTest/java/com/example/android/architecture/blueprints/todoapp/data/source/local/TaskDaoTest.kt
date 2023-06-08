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

package com.example.android.architecture.blueprints.todoapp.data.source.local

import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class TaskDaoTest {
    companion object {
        const val TAG = "TaskDaoTest"
    }

    private lateinit var database: ToDoDatabase

    // setUpDb()でインメモリデータベースを作成
    // ディスクベースのデータベースよりも高速。テストより長くデータを保持する必要のない自動テストに向いている
    @Before
    fun setUpDb() {
        database = Room.inMemoryDatabaseBuilder(
            getApplicationContext(),
            ToDoDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    /**
     * ダミーのLocalTaskオブジェクトを用意
     * TaskDaoのAPIを呼び出す insert → get
     * getしたデータが用意したLocalTaskオブジェクトと一致しているかをassert
     * TaskDaoのAPIを呼び出す delete → get
     * getしたデータのサイズが0になっているかをassert
     */
    @Test
    fun test_insertTask_getTask_deleteTask() {
        Log.d(TAG, "test_insertTask_getTask_deleteTask start")
        runTest {
            val expected = LocalTask(
                "999",
                "dummyTask",
                "This is dummy task",
                false
            )

            val taskDao = database.taskDao()
            // insert task
            taskDao.upsert(expected)
            // get task
            var tasks = taskDao.observeAll().first()
            Assert.assertEquals(expected, tasks[0])

            // delete task
            taskDao.deleteAll()
            tasks = taskDao.observeAll().first()
            Assert.assertEquals(0, tasks.size)
            Log.d(TAG, "test_insertTask_getTask_deleteTask end")
        }
    }
}