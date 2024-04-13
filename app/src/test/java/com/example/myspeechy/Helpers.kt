package com.example.myspeechy

import com.google.android.gms.tasks.Task
import com.google.firebase.storage.StorageReference
import io.mockk.every
import io.mockk.mockk

//Can take any possible kind of result, such as AuthResult, DocumentSnapshot, QuerySnapshot
val userId = "userId"
val username = "SomeName"
fun mockStorage(mockedStorage: StorageReference): StorageReference {
    for (quality in listOf("normalQuality", "lowQuality")) {
        every { mockedStorage.child("profilePics").child(userId).child(quality).child("$userId.jpg").delete() } returns mockTask()
    }
    return mockedStorage
}
inline fun <reified T> mockTask(result: T? = null, exception: Exception? = null): Task<T> {
    val task = mockk<Task<T>>()
    every { task.isComplete } returns true
    every { task.result } returns result
    every { task.exception } returns exception
    every { task.isCanceled } returns false
    return task
}