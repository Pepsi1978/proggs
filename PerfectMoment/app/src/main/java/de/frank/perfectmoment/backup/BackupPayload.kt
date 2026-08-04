package de.frank.perfectmoment.backup

import de.frank.perfectmoment.data.local.HookEntity
import de.frank.perfectmoment.data.local.QuestionEntity
import de.frank.perfectmoment.data.local.SessionEntity
import de.frank.perfectmoment.data.local.SessionWithQuestions
import de.frank.perfectmoment.data.local.SkillEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * What travels to Google Drive: the hooks, skills and sessions the user created.
 *
 * Written as plain JSON rather than a copy of the database file, so a backup taken on one device
 * can be read by an app version with a different schema — a raw .db would have to match exactly.
 *
 * [dataVersion] counts up with every save. Before overwriting the copy in Drive the app compares
 * it, so a second device cannot silently discard newer data.
 */
data class BackupPayload(
    val dataVersion: Long,
    val createdAt: Long,
    val device: String,
    val hooks: List<HookEntity>,
    val skills: List<SkillEntity>,
    val sessions: List<SessionWithQuestions>,
) {

    fun toJson(): String = JSONObject().apply {
        put("formatVersion", FORMAT_VERSION)
        put("dataVersion", dataVersion)
        put("createdAt", createdAt)
        put("device", device)
        put(
            "hooks",
            JSONArray().apply {
                hooks.forEach { hook ->
                    put(
                        JSONObject().apply {
                            put("id", hook.id)
                            put("emoji", hook.emoji)
                            put("text", hook.text)
                            put("sortIndex", hook.sortIndex)
                        },
                    )
                }
            },
        )
        put(
            "skills",
            JSONArray().apply {
                skills.forEach { skill ->
                    put(
                        JSONObject().apply {
                            put("id", skill.id)
                            put("name", skill.name)
                            put("text", skill.text)
                            put("createdAt", skill.createdAt)
                        },
                    )
                }
            },
        )
        put(
            "sessions",
            JSONArray().apply {
                sessions.forEach { entry ->
                    put(
                        JSONObject().apply {
                            val session = entry.session
                            put("id", session.id)
                            put("topic", session.topic)
                            put("startedAt", session.startedAt)
                            put("durationMin", session.durationMin)
                            put("voiceName", session.voiceName)
                            put("providerId", session.providerId)
                            put("pauseRep", session.pauseRep)
                            put("pauseNext", session.pauseNext)
                            put("reps", session.reps)
                            put("questionCount", session.questionCount)
                            put("introContext", session.introContext)
                            put("entranceQuestion", session.entranceQuestion)
                            put("playCount", session.playCount)
                            put("lastPlayedAt", session.lastPlayedAt)
                            put("summary", session.summary)
                            put(
                                "questions",
                                JSONArray().apply {
                                    entry.questions.sortedBy(QuestionEntity::orderIndex).forEach { question ->
                                        put(
                                            JSONObject().apply {
                                                put("orderIndex", question.orderIndex)
                                                put("emoji", question.emoji)
                                                put("text", question.text)
                                            },
                                        )
                                    }
                                },
                            )
                        },
                    )
                }
            },
        )
    }.toString()

    companion object {
        const val FORMAT_VERSION = 1
        const val FILE_NAME = "perfectmoment-sicherung.json"

        /** Returns null when the text is not a backup this version understands. */
        fun fromJson(raw: String): BackupPayload? = runCatching {
            val root = JSONObject(raw)
            if (root.optInt("formatVersion", 0) > FORMAT_VERSION) return null

            val hooks = root.optJSONArray("hooks").map { item ->
                HookEntity(
                    id = item.optLong("id"),
                    emoji = item.optString("emoji"),
                    text = item.optString("text"),
                    sortIndex = item.optInt("sortIndex"),
                )
            }
            val skills = root.optJSONArray("skills").map { item ->
                SkillEntity(
                    id = item.optLong("id"),
                    name = item.optString("name"),
                    text = item.optString("text"),
                    createdAt = item.optLong("createdAt"),
                )
            }
            val sessions = root.optJSONArray("sessions").map { item ->
                val session = SessionEntity(
                    id = item.optLong("id"),
                    topic = item.optString("topic"),
                    startedAt = item.optLong("startedAt"),
                    durationMin = item.optInt("durationMin"),
                    voiceName = item.optString("voiceName"),
                    providerId = item.optString("providerId"),
                    pauseRep = item.optInt("pauseRep"),
                    pauseNext = item.optInt("pauseNext"),
                    reps = item.optInt("reps"),
                    questionCount = item.optInt("questionCount"),
                    introContext = item.optString("introContext"),
                    entranceQuestion = item.optString("entranceQuestion"),
                    playCount = item.optInt("playCount", 1),
                    lastPlayedAt = item.optLong("lastPlayedAt", item.optLong("startedAt")),
                    summary = item.optString("summary"),
                )
                val questions = item.optJSONArray("questions").map { entry ->
                    QuestionEntity(
                        sessionId = session.id,
                        orderIndex = entry.optInt("orderIndex"),
                        emoji = entry.optString("emoji"),
                        text = entry.optString("text"),
                    )
                }
                SessionWithQuestions(session, questions)
            }
            BackupPayload(
                dataVersion = root.optLong("dataVersion"),
                createdAt = root.optLong("createdAt"),
                device = root.optString("device"),
                hooks = hooks,
                skills = skills,
                sessions = sessions,
            )
        }.getOrNull()

        private inline fun <T> JSONArray?.map(transform: (JSONObject) -> T): List<T> {
            if (this == null) return emptyList()
            return (0 until length()).mapNotNull { index ->
                optJSONObject(index)?.let(transform)
            }
        }
    }
}
