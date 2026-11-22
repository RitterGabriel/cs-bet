package com.csbet.backend.opening;

import jakarta.validation.constraints.*;

public record SimulationPreviewRequest(
        @NotBlank @Size(max = 120) String caseId,
        @tools.jackson.databind.annotation.JsonDeserialize(using = SampleCountDeserializer.class)
        @NotNull @Min(1) @Max(MAX_SAMPLES) Integer samples) {
    public static final int MAX_SAMPLES = 10000;

    /** Rejeita frações e strings, sem alterar as configurações JSON dos demais endpoints. */
    public static final class SampleCountDeserializer extends tools.jackson.databind.ValueDeserializer<Integer> {
        @Override
        public Integer deserialize(tools.jackson.core.JsonParser parser,
                                   tools.jackson.databind.DeserializationContext context) {
            if (!parser.hasToken(tools.jackson.core.JsonToken.VALUE_NUMBER_INT))
                return context.reportInputMismatch(Integer.class, "A quantidade de amostras deve ser um inteiro JSON.");
            return parser.getIntValue();
        }
    }
}
