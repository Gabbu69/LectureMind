package com.lecturemind.app

import com.lecturemind.app.ai.*
import com.lecturemind.app.data.AudioPart
import com.lecturemind.app.recording.WavFile
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CoreTests {
    private fun study(sources: String="[\"p0s0\"]",answer: Int=0): String = """{"notes":[{"title":"Photosynthesis","body":"Plants use light.","sources":$sources}],"reviewer":[{"title":"Review","body":"What do plants use?","sources":$sources}],"questions":[{"prompt":"What do plants use?","options":["Light","Sound"],"answerIndex":$answer,"explanation":"The lecture says light.","sources":$sources}],"insufficient":false,"reason":""}"""
    @Test fun validatesStudyAndRoundTripsQuestions() {
        val content=ContentCodec.validate(study(),setOf("p0s0"),10)
        assertEquals(1,content.questions.size)
        assertEquals(content.questions,ContentCodec.quiz(ContentCodec.quizJson(content.questions)))
        assertEquals(1,ContentCodec.score(content.questions,listOf(0)))
        assertEquals(0,ContentCodec.score(content.questions,listOf(1)))
        assertEquals(0,ContentCodec.score(content.questions,emptyList()))
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsInventedReferences() { ContentCodec.validate(study("[\"fake\"]"),setOf("p0s0"),10) }
    @Test(expected=IllegalArgumentException::class) fun rejectsMissingReferences() { ContentCodec.validate(study("[]"),setOf("p0s0"),10) }
    @Test(expected=IllegalArgumentException::class) fun rejectsInvalidAnswer() { ContentCodec.validate(study(answer=2),setOf("p0s0"),10) }
    @Test(expected=IllegalArgumentException::class) fun rejectsRepeatedQuestions() { val obj=JSONObject(study());obj.getJSONArray("questions").put(obj.getJSONArray("questions").get(0));ContentCodec.validate(obj.toString(),setOf("p0s0"),10) }
    @Test fun acceptsExplicitInsufficientContent() { val content=ContentCodec.validate("""{"notes":[],"reviewer":[],"questions":[],"insufficient":true,"reason":"No clear speech"}""",emptySet(),10);assertTrue(content.insufficient);assertTrue(content.questions.isEmpty()) }
    @Test fun transcriptPreservesLanguageAndTimestamps() { val segments=listOf(Speech("p0s0",1234,2345,"Ang photosynthesis ay gumagamit ng light."));assertEquals(segments,ContentCodec.speech(ContentCodec.speechJson(segments))) }
    @Test fun malformedOutputProducesActionableError() { val error=runCatching{ContentCodec.parseObject("not JSON")}.exceptionOrNull();assertNotNull(error);assertTrue(error!!.message!!.contains("unreadable")) }
    @Test fun wavHeaderHasCorrectRateAndLength() { val h=ByteBuffer.wrap(WavFile.header(32000)).order(ByteOrder.LITTLE_ENDIAN);assertEquals(32036,h.getInt(4));assertEquals(16000,h.getInt(24));assertEquals(32000,h.getInt(28));assertEquals(32000,h.getInt(40)) }
    @Test fun crashRecoveryRepairsPartialSampleAndDuration() {
        val file=File.createTempFile("lecturemind-recovery",".wav")
        try { WavFile.create(file).use{it.write(ByteArray(32001))};assertEquals(1000,WavFile.repair(file));assertEquals(32044,file.length());val data=file.readBytes();assertEquals(32000,ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt(40)) } finally { file.delete() }
    }
    @Test fun seeksAcrossBoundariesAndClampsAtEnd() { val parts=listOf(300000L,300000L,42000L);assertEquals(0 to 0L,WavFile.position(parts,-100));assertEquals(1 to 0L,WavFile.position(parts,300000));assertEquals(1 to 1234L,WavFile.position(parts,301234));assertEquals(2 to 42000L,WavFile.position(parts,9999999)) }
    @Test fun twoHourFixtureStreamsAndResumesWithoutRepeatingCompletedSections() = runBlocking {
        val fixtureParent=File(System.getenv("LECTUREMIND_BUILD_ROOT") ?: System.getProperty("java.io.tmpdir"),"fixtures").canonicalFile.apply{mkdirs()}
        val root=java.nio.file.Files.createTempDirectory(fixtureParent.toPath(),"lecturemind-fixture-").toFile().canonicalFile
        try {
            val buffer=ByteArray(1024*1024);var total=0L;val parts=mutableListOf<AudioPart>()
            repeat(24) { i -> val file=File(root,"$i.wav");WavFile.create(file).use{out->var remaining=WavFile.SEGMENT_BYTES;while(remaining>0){val count=minOf(remaining,buffer.size);out.write(buffer,0,count);remaining-=count}};total+=WavFile.repair(file);assertEquals(9600044,file.length());parts+=AudioPart("part-$i","fixture",i,file.absolutePath,i*300000L,300000,true) }
            assertEquals(7200000,total)
            assertEquals(23 to 299000L,WavFile.position(List(24){300000L},7199000))
            val saved=mutableMapOf<String,AudioPart>();val calls=mutableListOf<Int>();var failOnce=true
            suspend fun transcribe(part: AudioPart): List<Speech> {
                calls+=part.position
                if(part.position==12 && failOnce){failOnce=false;throw java.io.IOException("Simulated network loss")}
                assertEquals(9600044,File(part.path).length())
                return listOf(Speech("p${part.position}s0",part.startMs,part.startMs+part.durationMs,"Educational fixture section ${part.position}"))
            }
            suspend fun save(part: AudioPart,segments: List<Speech>){saved[part.id]=part.copy(transcript=ContentCodec.speechJson(segments))}
            val first=runCatching{TranscriptPipeline.collect(parts,{},::transcribe,::save)}
            assertTrue(first.isFailure);assertEquals(12,saved.size)
            val resumed=TranscriptPipeline.collect(parts.map{saved[it.id] ?: it},{},::transcribe,::save)
            assertEquals(24,resumed.size);assertEquals(24,saved.size);assertEquals(25,calls.size)
            assertEquals(1,calls.count{it==0});assertEquals(2,calls.count{it==12});assertEquals(7200000,resumed.last().endMs)
        } finally { check(root.canonicalFile.parentFile==fixtureParent && root.name.startsWith("lecturemind-fixture-"));root.deleteRecursively() }
    }
}
