package com.ioline.ithink.ai.domain

import com.ioline.ithink.ai.data.PersonDB
import com.ioline.ithink.ai.data.PersonRecord
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

@Single
class PersonUseCase(
    private val personDB: PersonDB,
) {
    fun addPerson(
        name: String,
        numImages: Long,
    ): Long =
        personDB.addPerson(
            PersonRecord(
                personName = name,
                numImages = numImages,
                addTime = System.currentTimeMillis(),
            ),
        )

    fun removePerson(id: Long) {
        personDB.removePerson(id)
    }

    fun getAll(): Flow<List<PersonRecord>> = personDB.getAll()

    fun getCount(): Long = personDB.getCount()
}
