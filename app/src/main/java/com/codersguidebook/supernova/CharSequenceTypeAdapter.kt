package com.codersguidebook.supernova

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException

class CharSequenceTypeAdapter : TypeAdapter<CharSequence?>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: CharSequence?) {
        if (value == null) {
            out.nullValue()
        } else {
            // Assumes that value complies with CharSequence.toString() contract
            out.value(value.toString())
        }
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): CharSequence? {
        if (`in`.peek() === JsonToken.NULL) {
            // Skip the JSON null
            `in`.skipValue()
            return null
        } else {
            return `in`.nextString()
        }
    }
}