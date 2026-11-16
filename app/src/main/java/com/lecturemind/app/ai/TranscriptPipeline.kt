package com.lecturemind.app.ai

import com.lecturemind.app.data.AudioPart

/** Persist each successful section before advancing, so interrupted jobs can reuse it. */
object TranscriptPipeline {
    suspend fun collect(
        parts: List<AudioPart>,
        beforePart: suspend (Int) -> Unit,
        transcribe: suspend (AudioPart) -> List<Speech>,
        checkpoint: suspend (AudioPart, List<Speech>) -> Unit
    ): List<Speech> {
        val result=mutableListOf<Speech>()
        parts.forEachIndexed { index,part ->
            beforePart(index)
            val segments=if(part.transcript.isNotBlank()) ContentCodec.speech(part.transcript) else transcribe(part).also { checkpoint(part,it) }
            result+=segments
        }
        return result
    }
}
