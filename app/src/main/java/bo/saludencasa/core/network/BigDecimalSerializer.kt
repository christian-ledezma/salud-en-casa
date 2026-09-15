package bo.saludencasa.core.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal

// PostgREST writes a numeric column as a JSON number, and both directions go
// through the literal text of that number: decoding it as a Double first would
// round the amount before any value object could refuse it, and the stored
// scale would be lost on the way in.
//
// It leaves as a JSON string on purpose. Handing kotlinx a JsonPrimitive built
// from a number re-encodes it through Double -- 120.00 goes out as 120.0 --
// and the unquoted literal that would avoid it is an experimental API. The
// server casts the text to the argument type, which is the same path the birth
// date already takes to reach a `date` argument.
object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BigDecimal =
        BigDecimal((decoder as JsonDecoder).decodeJsonElement().jsonPrimitive.content)

    override fun serialize(
        encoder: Encoder,
        value: BigDecimal,
    ) {
        encoder.encodeString(value.toPlainString())
    }
}
