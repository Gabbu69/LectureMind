package com.lecturemind.app.ai

/** Shared names keep saved preferences, the upload disclosure and AI instructions consistent. */
internal object LectureLanguages {
    val outputOptions=listOf("English","Filipino / Tagalog","Ilonggo (Hiligaynon)","Bisaya (Cebuano)")

    fun canonicalOutput(value: String): String? = when(value.trim().lowercase()) {
        "english","en" -> outputOptions[0]
        "filipino","tagalog","filipino / tagalog","fil","tl" -> outputOptions[1]
        "ilonggo","hiligaynon","ilonggo (hiligaynon)","hil" -> outputOptions[2]
        "bisaya","cebuano","bisaya (cebuano)","ceb" -> outputOptions[3]
        else -> null
    }

    const val speechGuidance="Auto-detect Hiligaynon (Ilonggo), Cebuano (Bisaya), Tagalog/Filipino and English, including code-switching within sentences. Preserve the original spoken words and technical terms. Do not translate the transcript or rewrite Ilonggo or Cebuano as Tagalog. Mark uncertain or unintelligible words as [unclear] instead of guessing."

    fun studyInstruction(value: String): String = when(canonicalOutput(value)) {
        outputOptions[1] -> "Write the notes, reviewer, questions, answer choices and explanations in natural Filipino/Tagalog."
        outputOptions[2] -> "Write the notes, reviewer, questions, answer choices and explanations in natural Hiligaynon (Ilonggo), not Cebuano or Tagalog."
        outputOptions[3] -> "Write the notes, reviewer, questions, answer choices and explanations in natural Cebuano (Bisaya), not Hiligaynon or Tagalog."
        else -> "Write the notes, reviewer, questions, answer choices and explanations in English."
    }+" Understand source material in any of these lecture languages and mixed speech. Preserve technical terms and names; explain them in the chosen output language. Do not invent a meaning for [unclear] speech."
}
