package com.example.data.model

object VerifiedModelCatalogue {

    val verifiedModels = listOf(
        ModelInfo(
            id = "smollm-135m-instruct",
            name = "SmolLM 135M Instruct",
            publisher = "Hugging Face TB",
            format = ModelFormat.MEDIA_PIPE_TASK,
            licenseName = "Apache-2.0",
            licenseUrl = "https://huggingface.co/HuggingFaceTB/SmolLM-135M-Instruct",
            approxSizeBytes = 98_000_000L, // ~98 MB
            minRamMb = 1024,
            parameterCount = "135M",
            description = "Ultra-compact mobile-friendly language model. Ideal for low-RAM devices (such as Kirin 710 with 3GB RAM) and instantaneous response testing without large downloads.",
            downloadUrl = "https://huggingface.co/litert-community/SmolLM-135M-Instruct-TFLite/resolve/main/smollm-135m-instruct.bin",
            expectedSha256 = null
        ),
        ModelInfo(
            id = "gemma-3-1b-it",
            name = "Gemma 3 1B Instruct",
            publisher = "Google DeepMind",
            format = ModelFormat.MEDIA_PIPE_TASK,
            licenseName = "Gemma Terms of Use",
            licenseUrl = "https://ai.google.dev/gemma/terms",
            approxSizeBytes = 840_000_000L, // ~840 MB
            minRamMb = 2048,
            parameterCount = "1B",
            description = "Google's state-of-the-art 1B instruction-tuned model. Excellent reasoning, coding, and multilingual knowledge in a compact mobile package.",
            downloadUrl = "https://huggingface.co/google/gemma-3-1b-it-litert/resolve/main/gemma-3-1b-it-cpu-int4.bin",
            expectedSha256 = null
        ),
        ModelInfo(
            id = "gemma-2b-it",
            name = "Gemma 2B Instruct",
            publisher = "Google DeepMind",
            format = ModelFormat.MEDIA_PIPE_TASK,
            licenseName = "Gemma Terms of Use",
            licenseUrl = "https://ai.google.dev/gemma/terms",
            approxSizeBytes = 1_350_000_000L, // ~1.35 GB
            minRamMb = 3072,
            parameterCount = "2B",
            description = "High quality 2B instruction-tuned model trained on Google's Gemini architecture. Requires ~3GB RAM for smooth execution.",
            downloadUrl = "https://huggingface.co/google/gemma-2b-it-tflite/resolve/main/gemma-2b-it-cpu-int4.bin",
            expectedSha256 = null
        ),
        ModelInfo(
            id = "phi-2-instruct",
            name = "Phi-2 2.7B Instruct",
            publisher = "Microsoft",
            format = ModelFormat.MEDIA_PIPE_TASK,
            licenseName = "MIT License",
            licenseUrl = "https://huggingface.co/microsoft/phi-2",
            approxSizeBytes = 1_650_000_000L, // ~1.65 GB
            minRamMb = 4096,
            parameterCount = "2.7B",
            description = "Microsoft's 2.7B model showcasing remarkable common-sense reasoning and textbook-quality generation.",
            downloadUrl = "https://huggingface.co/litert-community/phi-2-tflite/resolve/main/phi-2-cpu-int4.bin",
            expectedSha256 = null
        )
    )

    fun getModelById(id: String): ModelInfo? {
        return verifiedModels.find { it.id == id }
    }
}
