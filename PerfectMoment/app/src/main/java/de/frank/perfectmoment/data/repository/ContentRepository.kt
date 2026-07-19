package de.frank.perfectmoment.data.repository

import de.frank.perfectmoment.data.local.HookEntity
import de.frank.perfectmoment.data.local.PerfectMomentDatabase
import de.frank.perfectmoment.data.local.SkillEntity
import kotlinx.coroutines.flow.Flow

interface ContentRepository {
    fun observeHooks(): Flow<List<HookEntity>>
    fun observeSkills(): Flow<List<SkillEntity>>
    fun observeSkill(skillId: Long): Flow<SkillEntity?>
    suspend fun getHook(hookId: Long): HookEntity?
    suspend fun getSkill(skillId: Long): SkillEntity?
    suspend fun upsertHook(hook: HookEntity): Long
    suspend fun upsertSkill(skill: SkillEntity): Long
    suspend fun deleteHook(hook: HookEntity)
    suspend fun deleteSkill(skill: SkillEntity)
    suspend fun reorderHooks(hookIds: List<Long>)
}

class RoomContentRepository(database: PerfectMomentDatabase) : ContentRepository {
    private val hookDao = database.hookDao()
    private val skillDao = database.skillDao()

    override fun observeHooks(): Flow<List<HookEntity>> = hookDao.observeHooks()

    override fun observeSkills(): Flow<List<SkillEntity>> = skillDao.observeSkills()

    override fun observeSkill(skillId: Long): Flow<SkillEntity?> = skillDao.observeSkill(skillId)

    override suspend fun getHook(hookId: Long): HookEntity? = hookDao.getHook(hookId)

    override suspend fun getSkill(skillId: Long): SkillEntity? = skillDao.getSkill(skillId)

    override suspend fun upsertHook(hook: HookEntity): Long = hookDao.upsertHook(hook)

    override suspend fun upsertSkill(skill: SkillEntity): Long = skillDao.upsertSkill(skill)

    override suspend fun deleteHook(hook: HookEntity) = hookDao.deleteHook(hook)

    override suspend fun deleteSkill(skill: SkillEntity) = skillDao.deleteSkill(skill)

    override suspend fun reorderHooks(hookIds: List<Long>) = hookDao.reorder(hookIds)
}
