package org.orbit.serial

import org.json.JSONObject

interface Serial {
    fun describe(json: JSONObject)
}

object Serialiser {
    fun serialise(obj: Serial?) : JSONObject {
        return JSONObject().apply {
            obj?.describe(this)
        }
    }
}