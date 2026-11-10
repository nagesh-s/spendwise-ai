# Third-party notices

SpendWise AI uses third-party libraries and bundled model assets. Their names and trademarks belong to their respective owners. This document identifies source and licensing information; it does not grant additional rights.

## Bundled Universal Sentence Encoder model

- **Local file:** `app/src/main/assets/models/text_embedder.tflite`
- **Size:** 28,691,620 bytes
- **SHA-256:** `cf0f989feaea1a9c27e7923e29cec2fcbd2edb6c7321fcc227edf5edce7785cd`
- **Publisher:** TensorFlow / Google
- **License:** [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- **Model card:** [Universal Sentence Encoder Lite](https://www.kaggle.com/models/tensorflow/universal-sentence-encoder/frameworks/TensorFlow1/variations/lite)
- **Unmodified distribution source:** `https://storage.googleapis.com/download.tensorflow.org/models/tflite_support/searcher/text_to_image_blogpost/text_embedder.tflite`
- **Source tutorial:** [Model Maker Text Searcher](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/lite/g3doc/models/modify/model_maker/text_searcher.ipynb), Copyright The AI Edge Authors, Apache-2.0

The local file is renamed for packaging. Its SHA-256 was verified against the official distribution on 2026-11-10. SpendWise uses the model with MediaPipe Text Embedder to compute semantic similarity for bucket suggestions. Model inference runs on-device. Model predictions can be wrong; users should verify extracted or suggested financial data.

The Kaggle model card identifies the model as a copy of Google's Universal Sentence Encoder Lite and lists Apache-2.0 as its license. The original Universal Sentence Encoder is described in [Cer et al., 2018](https://arxiv.org/abs/1803.11175).

## Optional Gemma model

The `offlineLlm` flavor does not include a Gemma model. Users or distributors must obtain the model separately and comply with its applicable terms. Do not assume the app's Apache-2.0 license covers Gemma model weights. See `app/src/offlineLlm/assets/models/README_MODEL_PACKAGING.txt`.

## Dependencies fetched by Gradle

These dependencies are resolved from Google Maven, Maven Central, or JitPack; their full transitive dependency trees are not bundled as source in this repository. Distributors should review the licenses included in their built artifacts and dependency metadata.

| Component | Version | Project |
| --- | --- | --- |
| AndroidX AppCompat | 1.6.1 | https://developer.android.com/jetpack/androidx/releases/appcompat |
| Material Components for Android | 1.12.0 | https://github.com/material-components/material-components-android |
| AndroidX ConstraintLayout | 2.1.4 | https://github.com/androidx/constraintlayout |
| AndroidX Navigation | 2.7.7 | https://developer.android.com/jetpack/androidx/releases/navigation |
| AndroidX Room | 2.6.1 | https://developer.android.com/jetpack/androidx/releases/room |
| AndroidX Lifecycle | 2.8.0 | https://developer.android.com/jetpack/androidx/releases/lifecycle |
| MPAndroidChart | 3.1.0 | https://github.com/PhilJay/MPAndroidChart |
| MaterialCalendarView | 2.0.1 | https://github.com/prolificinteractive/material-calendarview |
| ThreeTenABP | 1.4.4 | https://github.com/JakeWharton/ThreeTenABP |
| AndroidX WorkManager | 2.9.0 | https://developer.android.com/jetpack/androidx/releases/work |
| AndroidX Biometric | 1.1.0 | https://developer.android.com/jetpack/androidx/releases/biometric |
| LiteRT | 1.4.2 | https://github.com/google-ai-edge/LiteRT |
| MediaPipe Tasks Text | 0.10.28 | https://github.com/google-ai-edge/mediapipe |
| MediaPipe Tasks GenAI, `offlineLlm` only | 0.10.27 | https://github.com/google-ai-edge/mediapipe |
| JUnit | 4.13.2 | https://github.com/junit-team/junit4 |
| AndroidX Test JUnit and Espresso | 1.1.5 / 3.5.1 | https://github.com/android/android-test |
| Mockito and Mockito Inline | 5.10.0 / 5.2.0 | https://github.com/mockito/mockito |

The Gradle wrapper is licensed under Apache-2.0; see the notices in `gradlew` and `gradlew.bat`.

App launcher and interface icons are bundled as Android XML drawables. No Font Awesome font binary is distributed in the current source tree. Any screenshots added later must contain only synthetic or carefully redacted financial data.
