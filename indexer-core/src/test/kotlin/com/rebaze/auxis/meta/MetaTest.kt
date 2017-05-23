package com.rebaze.auxis.meta

import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class MetaTest {

    open class Base(p: Int)

    class Derived(f: Int) : Base(f)


    @Test
    fun testMe() {
        val files = File("Test").listFiles()
        println("Result: " + (files?.size ?: "Nope"))

        files?.let {
            println("Not null!")
        }

        val stream = Files.newInputStream(Paths.get("/some/file.txt"))
        stream.buffered().reader().use {

        }


        stream.buffered().reader().use { reader ->
            println(reader.readText())
        }
    }

}
