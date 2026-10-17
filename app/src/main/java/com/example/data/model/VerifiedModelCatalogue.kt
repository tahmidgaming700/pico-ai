package com.example.data.model

object VerifiedModelCatalogue {

    val verifiedModels = listOf(
        ModelInfo(
            id = "smollm-135m-instruct",
            name = "SmolLM 135M Instruct",
            publisher = "Hugging Face TB (LiteRT)",
            format = ModelFormat.MEDIA_PIPE_TASK,
            licenseName = "Apache-2.0",
            licenseUrl = "https://huggingface.co/HuggingFaceTB/SmolLM-135M-Instruct",
            approxSizeBytes = 166_754_726L, // ~166 MB
            minRamMb = 1024,
            parameterCount = "135M",
            description = "100% Public & Un-gated. Ultra-compact mobile-optimized language model bundle (.task). Ideal for Huawei Y9 2019 / Kirin 710 devices with 3GB RAM and instant offline responses.",
            downloadUrl = "https://huggingface.co/litert-community/SmolLM-135M-Instruct/resolve/main/SmolLM-135M-Instruct_multi-prefill-seq_q8_ekv1280.task",
            expectedSha256 = null,
            isGated = false
        ),
        ModelInfo(
            id = "qwen-2.5-1.5b-instruct",
            name = "Qwen 2.5 1.5B Instruct",
            publisher = "Qwen Team (LiteRT Community)",
            format = ModelFormat.MEDIA_PIPE_TASK,
            licenseName = "Apache-2.0",
            licenseUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct",
            approxSizeBytes = 1_860_000_000L, // ~1.86 GB
            minRamMb = 2048,
            parameterCount = "1.5B",
            description = "100% Public & Un-gated. High-performance multi-turn instruction model packaged in official MediaPipe .task bundle. Strong code and multilingual reasoning.",
            downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            expectedSha256 = null,
            isGated = false
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
            description = "Google's 1B mobile instruction model. Gated model: requires accepting terms on HuggingFace and adding your free Hugging Face token in Settings.",
            downloadUrl = "https://huggingface.co/google/gemma-3-1b-it-litert/resolve/main/gemma-3-1b-it-cpu-int4.bin",
            expectedSha256 = null,
            isGated = true
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
            description = "Google's 2B instruction model for devices with 3GB+ RAM. Gated model: requires Hugging Face access token with accepted Gemma terms in Settings.",
            downloadUrl = "https://huggingface.co/google/gemma-2b-it-tflite/resolve/main/gemma-2b-it-cpu-int4.bin",
            expectedSha256 = null,
            isGated = true
        )
    )

    fun getModelById(id: String): ModelInfo? {
        return verifiedModels.find { it.id == id }
    }
}
