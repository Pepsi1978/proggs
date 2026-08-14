package de.frank.stacklabor.werftstudio.service.codex

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

class InvalidCodexJsonException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

object CodexJson {
    private val topLevelKeys = setOf("zellen", "konkurrenzen", "antworten", "gesamt", "hinweise")
    private val cellKeys = setOf("nem", "ziel", "wirkung", "staerke", "grund")
    private val competitionKeys = setOf("nem_a", "nem_b", "art", "schwere", "grund")
    private val answerKeys = setOf("frage", "text")

    fun parse(
        raw: String,
        supplementIds: Set<String>,
        goalIds: Set<String>,
        questionIds: Set<String>,
    ): CodexEvaluation {
        val root = try {
            val tokener = JSONTokener(raw)
            val value = tokener.nextValue() as? JSONObject
                ?: throw InvalidCodexJsonException("Die Antwort ist kein JSON-Objekt.")
            if (tokener.nextClean().code != 0) {
                throw InvalidCodexJsonException("Nach dem JSON-Objekt stehen weitere Zeichen.")
            }
            value
        } catch (error: Exception) {
            throw InvalidCodexJsonException("Die Antwort ist kein gültiges JSON-Objekt.", error)
        }
        requireExactKeys(root, topLevelKeys, "Antwort")
        val seenCells = mutableSetOf<Pair<String, String>>()
        val cells = root.requiredArray("zellen").mapObjects("zellen") { item, index ->
            requireExactKeys(item, cellKeys, "zellen[$index]")
            val supplementId = item.requiredString("nem")
            val goalId = item.requiredString("ziel")
            require(supplementId in supplementIds) { "zellen[$index].nem ist unbekannt: $supplementId" }
            require(goalId in goalIds) { "zellen[$index].ziel ist unbekannt: $goalId" }
            require(seenCells.add(supplementId to goalId)) { "Doppelte Bewertungszelle für $supplementId/$goalId." }
            EvaluationCell(
                supplementId = supplementId,
                goalId = goalId,
                effect = enumByWire<CellEffect>(item.requiredString("wirkung"), "zellen[$index].wirkung"),
                strength = item.requiredInt("staerke", 1..3),
                reason = item.requiredString("grund", maxLength = 140),
            )
        }
        val seenCompetitions = mutableSetOf<String>()
        val competitions = root.requiredArray("konkurrenzen").mapObjects("konkurrenzen") { item, index ->
            requireExactKeys(item, competitionKeys, "konkurrenzen[$index]")
            val a = item.requiredString("nem_a")
            val b = item.requiredString("nem_b")
            require(a in supplementIds && b in supplementIds) { "konkurrenzen[$index] enthält unbekannte Mittel." }
            require(a != b) { "konkurrenzen[$index] vergleicht ein Mittel mit sich selbst." }
            val pairKey = listOf(a, b).sorted().joinToString("\u0000")
            require(seenCompetitions.add(pairKey)) { "Doppelte Konkurrenz für $a/$b." }
            Competition(
                supplementAId = a,
                supplementBId = b,
                type = enumByWire<CompetitionType>(item.requiredString("art"), "konkurrenzen[$index].art"),
                severity = item.requiredInt("schwere", 1..3),
                reason = item.requiredString("grund"),
            )
        }
        val seenQuestions = mutableSetOf<String>()
        val answers = root.requiredArray("antworten").mapObjects("antworten") { item, index ->
            requireExactKeys(item, answerKeys, "antworten[$index]")
            val questionId = item.requiredString("frage")
            require(questionId in questionIds) { "antworten[$index].frage ist unbekannt: $questionId" }
            require(seenQuestions.add(questionId)) { "Doppelte Antwort für Frage $questionId." }
            QuestionAnswer(questionId, item.requiredString("text"))
        }
        if (seenQuestions != questionIds) {
            invalid("Antworten fehlen für Fragen: ${questionIds - seenQuestions}")
        }
        val notices = root.requiredArray("hinweise").mapStrings("hinweise")
        return CodexEvaluation(cells, competitions, answers, root.requiredString("gesamt"), notices)
    }

    fun schema(): JSONObject {
        fun stringEnum(vararg values: String): JSONObject = JSONObject()
            .put("type", "string")
            .put("enum", JSONArray(values))
        fun objectSchema(keys: List<String>, properties: JSONObject): JSONObject = JSONObject()
            .put("type", "object")
            .put("additionalProperties", false)
            .put("required", JSONArray(keys))
            .put("properties", properties)
        fun array(items: JSONObject): JSONObject = JSONObject().put("type", "array").put("items", items)
        val cell = objectSchema(
            cellKeys.toList(),
            JSONObject()
                .put("nem", JSONObject().put("type", "string"))
                .put("ziel", JSONObject().put("type", "string"))
                .put("wirkung", stringEnum("stuetzt", "stoert"))
                .put("staerke", JSONObject().put("type", "integer").put("minimum", 1).put("maximum", 3))
                .put("grund", JSONObject().put("type", "string").put("maxLength", 140)),
        )
        val competition = objectSchema(
            competitionKeys.toList(),
            JSONObject()
                .put("nem_a", JSONObject().put("type", "string"))
                .put("nem_b", JSONObject().put("type", "string"))
                .put("art", stringEnum("aufnahme", "wirkung", "zeitpunkt"))
                .put("schwere", JSONObject().put("type", "integer").put("minimum", 1).put("maximum", 3))
                .put("grund", JSONObject().put("type", "string")),
        )
        val answer = objectSchema(
            answerKeys.toList(),
            JSONObject()
                .put("frage", JSONObject().put("type", "string"))
                .put("text", JSONObject().put("type", "string")),
        )
        return objectSchema(
            topLevelKeys.toList(),
            JSONObject()
                .put("zellen", array(cell))
                .put("konkurrenzen", array(competition))
                .put("antworten", array(answer))
                .put("gesamt", JSONObject().put("type", "string"))
                .put("hinweise", array(JSONObject().put("type", "string"))),
        )
    }

    fun bestEffortNarrative(raw: String): String = decodeStringFieldPrefix(raw, "gesamt")

    internal fun decodeStringFieldPrefix(json: String, key: String): String {
        val keyIndex = json.indexOf("\"$key\"")
        if (keyIndex < 0) return ""
        var index = json.indexOf(':', keyIndex + key.length + 2)
        if (index < 0) return ""
        index++
        while (index < json.length && json[index].isWhitespace()) index++
        if (index >= json.length || json[index] != '"') return ""
        index++
        return buildString {
            while (index < json.length) {
                when (val char = json[index++]) {
                    '"' -> return@buildString
                    '\\' -> {
                        if (index >= json.length) return@buildString
                        when (val escaped = json[index++]) {
                            '"', '\\', '/' -> append(escaped)
                            'b' -> append('\b')
                            'f' -> append('\u000c')
                            'n' -> append('\n')
                            'r' -> append('\r')
                            't' -> append('\t')
                            'u' -> {
                                if (index + 4 > json.length) return@buildString
                                val value = json.substring(index, index + 4).toIntOrNull(16) ?: return@buildString
                                append(value.toChar())
                                index += 4
                            }
                            else -> return@buildString
                        }
                    }
                    else -> append(char)
                }
            }
        }
    }

    private inline fun <reified T : Enum<T>> enumByWire(value: String, path: String): T {
        val match = enumValues<T>().firstOrNull {
            when (it) {
                is CellEffect -> it.wireValue == value
                is CompetitionType -> it.wireValue == value
                else -> false
            }
        }
        return match ?: invalid("$path hat einen ungültigen Wert: $value")
    }

    private fun requireExactKeys(json: JSONObject, expected: Set<String>, path: String) {
        val actual = json.keys().asSequence().toSet()
        if (actual != expected) invalid("$path enthält falsche Felder. Erwartet: $expected, erhalten: $actual")
    }

    private fun JSONObject.requiredArray(key: String): JSONArray = optJSONArray(key)
        ?: invalid("$key muss ein Array sein.")

    private fun JSONObject.requiredString(key: String, maxLength: Int? = null): String {
        if (opt(key) !is String) invalid("$key muss Text sein.")
        val value = getString(key).trim()
        if (value.isEmpty()) invalid("$key darf nicht leer sein.")
        if (maxLength != null && value.length > maxLength) invalid("$key ist länger als $maxLength Zeichen.")
        return value
    }

    private fun JSONObject.requiredInt(key: String, range: IntRange): Int {
        val value = opt(key)
        if (value !is Number || value.toDouble() % 1.0 != 0.0) invalid("$key muss ganzzahlig sein.")
        return value.toInt().also { if (it !in range) invalid("$key liegt nicht in ${range.first}..${range.last}.") }
    }

    private fun <T> JSONArray.mapObjects(path: String, transform: (JSONObject, Int) -> T): List<T> =
        (0 until length()).map { index ->
            val item = optJSONObject(index) ?: invalid("$path[$index] muss ein Objekt sein.")
            try {
                transform(item, index)
            } catch (error: InvalidCodexJsonException) {
                throw error
            } catch (error: IllegalArgumentException) {
                throw InvalidCodexJsonException(error.message ?: "$path[$index] ist ungültig.", error)
            }
        }

    private fun JSONArray.mapStrings(path: String): List<String> = (0 until length()).map { index ->
        val value = opt(index) as? String ?: invalid("$path[$index] muss Text sein.")
        value.trim().takeIf(String::isNotEmpty) ?: invalid("$path[$index] darf nicht leer sein.")
    }

    private fun invalid(message: String): Nothing = throw InvalidCodexJsonException(message)
}
