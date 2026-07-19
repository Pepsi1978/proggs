package de.frank.perfectmoment.auth

import org.json.JSONArray

internal class StreamingQuestionDecoder {
    private val buffer = StringBuilder()
    private var emittedCount = 0

    fun append(delta: String): List<String> {
        if (delta.isEmpty()) return emptyList()
        buffer.append(delta)
        val completeQuestions = extractCompleteQuestions(buffer.toString())
        if (completeQuestions.size <= emittedCount) return emptyList()
        return completeQuestions.subList(emittedCount, completeQuestions.size).also {
            emittedCount = completeQuestions.size
        }
    }

    private fun extractCompleteQuestions(json: String): List<String> {
        val keyIndex = json.indexOf(QUESTIONS_KEY)
        if (keyIndex < 0) return emptyList()
        val arrayStart = json.indexOf('[', keyIndex + QUESTIONS_KEY.length)
        if (arrayStart < 0) return emptyList()

        val questions = mutableListOf<String>()
        var index = arrayStart + 1
        while (index < json.length) {
            while (index < json.length && (json[index].isWhitespace() || json[index] == ',')) index++
            if (index >= json.length || json[index] == ']') break
            if (json[index] != '"') break

            val tokenStart = index
            index++
            var escaped = false
            var complete = false
            while (index < json.length) {
                val char = json[index]
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> {
                        val token = json.substring(tokenStart, index + 1)
                        questions += JSONArray("[$token]").getString(0)
                        index++
                        complete = true
                        break
                    }
                }
                index++
            }
            if (!complete) break
        }
        return questions
    }

    private companion object {
        const val QUESTIONS_KEY = "\"fragen\""
    }
}
