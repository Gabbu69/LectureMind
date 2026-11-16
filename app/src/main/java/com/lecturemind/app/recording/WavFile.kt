package com.lecturemind.app.recording

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Crash recovery uses actual bytes; only incomplete 16-bit samples are truncated. */
object WavFile {
    const val SAMPLE_RATE = 16000
    const val BYTES_PER_SECOND = SAMPLE_RATE * 2
    const val SEGMENT_BYTES = BYTES_PER_SECOND * 300
    fun header(dataBytes: Long): ByteArray {
        require(dataBytes in 0..0xffffffffL - 36)
        return ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt((dataBytes + 36).toInt()); put("WAVEfmt ".toByteArray())
            putInt(16); putShort(1); putShort(1); putInt(SAMPLE_RATE); putInt(BYTES_PER_SECOND); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(dataBytes.toInt())
        }.array()
    }
    fun create(file: File): RandomAccessFile { file.parentFile?.mkdirs(); return RandomAccessFile(file, "rw").apply { setLength(0); write(header(0)) } }
    fun repair(file: File): Long {
        if (!file.exists() || file.length() < 44) return 0
        val size = (file.length() - 44) / 2 * 2
        RandomAccessFile(file, "rw").use { it.setLength(size + 44); it.write(header(size)); it.fd.sync() }
        return size * 1000 / BYTES_PER_SECOND
    }
    fun position(partDurations: List<Long>, timestamp: Long): Pair<Int, Long> {
        var remaining = timestamp.coerceAtLeast(0)
        partDurations.forEachIndexed { index, duration ->
            if (remaining < duration || index == partDurations.lastIndex) return index to remaining.coerceAtMost(duration)
            remaining -= duration
        }
        return 0 to 0L
    }
}
