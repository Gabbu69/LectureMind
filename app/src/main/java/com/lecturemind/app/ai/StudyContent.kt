package com.lecturemind.app.ai

import org.json.JSONArray
import org.json.JSONObject

data class Speech(val id: String, val startMs: Long, val endMs: Long, val text: String) {
    fun json() = JSONObject().put("id", id).put("startMs", startMs).put("endMs", endMs).put("text", text)
}
data class StudySection(val title: String, val body: String, val sources: List<String>)
data class Question(val prompt: String, val options: List<String>, val answer: Int, val explanation: String, val sources: List<String>)
data class StudyContent(val notes: List<StudySection>, val reviewer: List<StudySection>, val questions: List<Question>, val insufficient: Boolean, val reason: String) {
    fun notesText() = notes.joinToString("\n\n") { "## ${it.title}\n${it.body}\nSources: ${it.sources.joinToString(", ")}" }
    fun reviewerText() = reviewer.joinToString("\n\n") { "## ${it.title}\n${it.body}\nSources: ${it.sources.joinToString(", ")}" }
}
object ContentCodec {
    fun parseObject(text: String): JSONObject {
        require(text.length <= 1_000_000) { "AI output is too large. Try a shorter recording." }
        val trimmed = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return try { JSONObject(trimmed) } catch (_: Exception) { error("The provider returned an unreadable response. Retry to generate valid study content.") }
    }
    fun speech(text: String): List<Speech> {
        if (text.isBlank()) return emptyList()
        val array = JSONArray(text)
        return (0 until array.length()).map { index -> array.getJSONObject(index).let { Speech(it.getString("id"), it.getLong("startMs"), it.getLong("endMs"), it.getString("text")) } }
    }
    fun speechJson(segments: List<Speech>) = JSONArray().apply { segments.forEach { put(it.json()) } }.toString()
    private fun strings(array: JSONArray): List<String> = (0 until array.length()).map { array.getString(it) }
    fun validate(raw: String, validIds: Set<String>, requested: Int): StudyContent {
        val obj = parseObject(raw)
        fun sources(item: JSONObject): List<String> {
            val ids = strings(item.getJSONArray("sources"))
            require(ids.isNotEmpty() && ids.size <= 20 && ids.all { it in validIds }) { "AI cited a missing transcript section. Retry generation." }
            return ids.distinct()
        }
        fun sections(name: String): List<StudySection> {
            val a = obj.getJSONArray(name); require(a.length() <= 60)
            return (0 until a.length()).map { i -> val item = a.getJSONObject(i)
                val title=item.getString("title"); val body=item.getString("body")
                require(title.isNotBlank() && title.length <= 200 && body.isNotBlank() && body.length <= 15000)
                StudySection(title, body, sources(item))
            }
        }
        val insufficient=obj.getBoolean("insufficient")
        val questions = obj.getJSONArray("questions")
        require(questions.length() <= requested) { "The AI returned too many questions." }
        val parsed=(0 until questions.length()).map { i ->
            val q=questions.getJSONObject(i);val options=strings(q.getJSONArray("options"));val answer=q.getInt("answerIndex")
            require(q.get("answerIndex") is Number && q.getDouble("answerIndex") == answer.toDouble()) { "The AI returned an invalid answer index." }
            require(options.size in 2..4 && options.distinct().size==options.size && options.all { it.isNotBlank() && it.length <= 2000 } && answer in options.indices)
            val prompt=q.getString("prompt");val explanation=q.getString("explanation")
            require(prompt.isNotBlank() && prompt.length <= 2000 && explanation.isNotBlank() && explanation.length <= 5000)
            Question(prompt,options,answer,explanation,sources(q))
        }
        val notes=sections("notes"); val reviewer=sections("reviewer")
        require(insufficient || (notes.isNotEmpty() && reviewer.isNotEmpty() && parsed.isNotEmpty())) { "Not enough study content was returned. Retry generation." }
        require(parsed.map { it.prompt.lowercase().trim() }.distinct().size == parsed.size) { "The provider repeated a question. Retry generation." }
        return StudyContent(notes, reviewer, parsed, insufficient, obj.getString("reason").take(500))
    }
    fun quiz(raw: String): List<Question> {
        if(raw.isBlank()) return emptyList()
        val a=JSONArray(raw)
        return (0 until a.length()).map { i -> val q=a.getJSONObject(i); Question(q.getString("prompt"), strings(q.getJSONArray("options")), q.getInt("answerIndex"), q.getString("explanation"), strings(q.getJSONArray("sources"))) }
    }
    fun quizJson(questions: List<Question>) = JSONArray().apply { questions.forEach { q -> put(JSONObject().put("prompt",q.prompt).put("options",JSONArray(q.options)).put("answerIndex",q.answer).put("explanation",q.explanation).put("sources",JSONArray(q.sources))) } }.toString()
    fun score(questions: List<Question>, answers: List<Int>) = questions.indices.count { answers.getOrNull(it) == questions[it].answer }

    private fun type(name: String)=JSONObject().put("type", name)
    private fun array(items: JSONObject)=type("array").put("items", items)
    private fun obj(vararg fields: Pair<String,JSONObject>) = type("object").put("properties",JSONObject().apply { fields.forEach { put(it.first,it.second) } }).put("required",JSONArray(fields.map { it.first })).put("additionalProperties",false)
    val speechSchema get() = obj("segments" to array(obj("startSeconds" to type("number"), "endSeconds" to type("number"), "text" to type("string"))))
    val studySchema get(): JSONObject {
        val section=obj("title" to type("string"),"body" to type("string"),"sources" to array(type("string")))
        val question=obj("prompt" to type("string"),"options" to array(type("string")),"answerIndex" to type("integer"),"explanation" to type("string"),"sources" to array(type("string")))
        return obj("notes" to array(section),"reviewer" to array(section),"questions" to array(question),"insufficient" to type("boolean"),"reason" to type("string"))
    }
}
