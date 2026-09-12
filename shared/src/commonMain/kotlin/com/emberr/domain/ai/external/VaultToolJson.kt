// Turns a tool call's JSON arguments into a plain string map, shared by every provider adapter.

package com.emberr.domain.ai.external

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal fun JsonObject.toVaultToolArguments(): Map<String, String> = mapValues { (_, value) ->
    (value as? JsonPrimitive)?.content.orEmpty()
}
