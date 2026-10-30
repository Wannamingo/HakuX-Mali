package com.rfandango.haku_x.data

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FolderDiscLayoutTest {
  private val content = mapOf("default.xbe" to ByteArray(4097) { (it * 7).toByte() }, "media/voice.bin" to "voice-data".toByteArray())
  private fun layout() = FolderDiscLayout(content.map { (p,b) -> FolderDiscLayout.Source(p,p,b.size.toLong()) })
  private fun read(d: FolderDiscLayout, at: Long, n: Int): ByteArray {
    val bytes = ByteArray(n)
    d.read(at,n,bytes) { f,off,out,start,size ->
      val b = content.getValue(f.uri);val k=minOf(size,b.size-off.toInt())
      b.copyInto(out,start,off.toInt(),off.toInt()+k);k
    }
    return bytes
  }
  private fun entry(d: FolderDiscLayout, sector: Long, size: Int, name: String): Pair<Long,Int> {
    val b=ByteBuffer.wrap(read(d,sector*2048,size)).order(ByteOrder.LITTLE_ENDIAN)
    var at=0
    while (true) {
      val len=b.get(at+13).toInt() and 255
      val n=String(ByteArray(len) { b.get(at+14+it) },Charsets.ISO_8859_1)
      val cmp=name.uppercase().compareTo(n.uppercase())
      if (cmp==0) return (b.getInt(at+4).toLong() and 0xFFFFFFFFL) to b.getInt(at+8)
      val next=b.getShort(at+if(cmp<0)0 else 2).toInt() and 65535
      check(next!=0);at=next*4
    }
  }
  @Test fun nativeCompatibleVolumeAndDirectoryTree() {
    val d=layout();val b=ByteBuffer.wrap(read(d,0x10000,2048)).order(ByteOrder.LITTLE_ENDIAN)
    assertEquals("MICROSOFT*XBOX*MEDIA",String(b.array().copyOfRange(0,20),Charsets.US_ASCII))
    assertEquals("MICROSOFT*XBOX*MEDIA",String(b.array().takeLast(20).toByteArray(),Charsets.US_ASCII))
    val root=b.getInt(20).toLong();val size=b.getInt(24)
    val xbe=entry(d,root,size,"DEFAULT.XBE")
    assertArrayEquals(content.getValue("default.xbe"),read(d,xbe.first*2048,xbe.second))
    val media=entry(d,root,size,"MEDIA")
    val voice=entry(d,media.first,media.second,"VOICE.BIN")
    assertArrayEquals(content.getValue("media/voice.bin"),read(d,voice.first*2048,voice.second))
    assertEquals(0L,d.size%65536)
  }
  @Test fun arbitraryReadsCrossFileEndsAndPadding() {
    val d=layout();val e=d.extents.first { it.source?.path=="default.xbe" }
    val b=read(d,e.start+e.size-3,20)
    assertArrayEquals(content.getValue("default.xbe").takeLast(3).toByteArray(),b.copyOfRange(0,3))
    assertTrue(b.drop(3).all { it==0.toByte() })
    assertEquals(0,d.read(d.size,16,ByteArray(16)) { _,_,_,_,_->error("Past EOF") })
  }
  @Test fun largeFilesUseLongOffsetsAndDoNotAllocatePayloads() {
    val length=3L shl 30
    val d=FolderDiscLayout(listOf(FolderDiscLayout.Source("default.xbe","x",length)))
    assertTrue(d.extents.sumOf { it.bytes?.size ?: 0 }<65536)
    val e=d.extents.first { it.source!=null };val out=ByteArray(25)
    val off=(2L shl 30)+41
    d.read(e.start+off,out.size,out) { _,at,b,start,n ->
      assertTrue(at>Int.MAX_VALUE)
      for(i in 0 until n)b[start+i]=(at+i).toByte();n
    }
    assertArrayEquals(ByteArray(25) { (off+it).toByte() },out)
  }
  @Test fun shortReadsAreCompletedAndTruncationIsReported() {
    val d=layout();val e=d.extents.first { it.source!=null };var calls=0
    val out=ByteArray(100)
    assertEquals(100,d.read(e.start,100,out) { _,_,b,start,n ->calls++;val k=minOf(7,n);b.fill(42,start,start+k);k })
    assertTrue(calls>1);assertTrue(out.all { it==42.toByte() })
    assertThrows(IOException::class.java) { d.read(e.start,100,out) { _,_,_,_,_->-1 } }
  }
  @Test fun rejectsMissingXbeAndCaseCollisions() {
    assertThrows(IllegalArgumentException::class.java) { FolderDiscLayout(emptyList()) }
    assertThrows(IllegalArgumentException::class.java) { FolderDiscLayout(listOf(FolderDiscLayout.Source("default.xbe","a",1),FolderDiscLayout.Source("DEFAULT.XBE","b",1))) }
    assertThrows(IllegalArgumentException::class.java) { FolderDiscLayout(listOf(FolderDiscLayout.Source("default.xbe","a",1),FolderDiscLayout.Source("default.xbe/child","b",1))) }
  }
  @Test fun rejectsUnsafeAndOverlongPaths() {
    for (path in listOf("../x","a/../../x","a\\..\\x","/x","\\x","C:/x","a//x","a/./x","a\u0000x")) {
      assertThrows(path,IllegalArgumentException::class.java) { XboxGamePath.parts(path) }
    }
    assertEquals(listOf("Manhunt","default.xbe"),XboxGamePath.parts("Manhunt\\default.xbe"))
    assertThrows(IllegalArgumentException::class.java) { FolderDiscLayout(listOf(FolderDiscLayout.Source("default.xbe","x",1),FolderDiscLayout.Source("a".repeat(256),"y",1))) }
  }
  @Test fun directoryEntriesNeverCrossSectors() {
    val files=listOf(FolderDiscLayout.Source("default.xbe","x",0))+(0 until 200).map { FolderDiscLayout.Source("file-%03d-".format(it)+"z".repeat(140),"$it",0) }
    val d=FolderDiscLayout(files)
    val root=ByteBuffer.wrap(read(d,0x10000,2048)).order(ByteOrder.LITTLE_ENDIAN)
    val sector=root.getInt(20).toLong();val size=root.getInt(24)
    for(f in files)assertEquals(0,entry(d,sector,size,f.path).second)
  }
}
