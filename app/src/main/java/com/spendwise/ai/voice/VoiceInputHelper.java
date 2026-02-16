package com.spendwise.ai.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceInputHelper {

    private static final String TAG = "VoiceInputHelper";

    // SpeechRecognizer codes missing on some API levels as named constants.
    private static final int ERROR_TOO_MANY_REQUESTS = 10;
    private static final int ERROR_SERVER_DISCONNECTED = 11;
    private static final int ERROR_LANGUAGE_NOT_SUPPORTED = 12;
    private static final int ERROR_LANGUAGE_UNAVAILABLE = 13;
    private static final int ERROR_CANNOT_CHECK_SUPPORT = 14;
    private static final int ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS = 15;

    public interface VoiceListener {
        void onVoiceText(String text);

        void onVoiceError(String error);
    }

    private final SpeechRecognizer speechRecognizer;
    private final Intent primaryRecognizerIntent;
    private final Intent noMatchRetryRecognizerIntent;
    private final Intent fallbackRecognizerIntent;
    private final Intent autoLanguageRecognizerIntent;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private VoiceListener listener;
    private String latestPartialTranscript;
    private boolean deliveredFinalTranscript;
    private boolean retryingWithFallbackLanguage;
    private boolean retryingWithAutoLanguage;
    private boolean retryingAfterNoMatch;
    private boolean retryingAfterClientError;

    public VoiceInputHelper(Context context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            String languageTag = Locale.getDefault().toLanguageTag();
            String fallbackLanguage = Locale.getDefault().getLanguage();

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            primaryRecognizerIntent = createRecognizerIntent(languageTag, true,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            noMatchRetryRecognizerIntent = createRecognizerIntent(languageTag, true,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            fallbackRecognizerIntent = createRecognizerIntent(fallbackLanguage, true,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            autoLanguageRecognizerIntent = createRecognizerIntent(null, true,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        } else {
            speechRecognizer = null;
            primaryRecognizerIntent = null;
            noMatchRetryRecognizerIntent = null;
            fallbackRecognizerIntent = null;
            autoLanguageRecognizerIntent = null;
        }
    }

    public void setListener(VoiceListener listener) {
        this.listener = listener;
    }

    public void startListening() {
        if (speechRecognizer == null) {
            if (listener != null)
                listener.onVoiceError("Speech recognition not available on this device");
            return;
        }
        latestPartialTranscript = null;
        deliveredFinalTranscript = false;
        retryingWithFallbackLanguage = false;
        retryingWithAutoLanguage = false;
        retryingAfterNoMatch = false;
        retryingAfterClientError = false;

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onError(int error) {
                String message = getErrorText(error);
                Log.e(TAG, "Error " + error + ": " + message);

                if (shouldFallbackToPartial(error)
                        && !TextUtils.isEmpty(latestPartialTranscript)
                        && !deliveredFinalTranscript) {
                    deliveredFinalTranscript = true;
                    if (listener != null) {
                        listener.onVoiceText(latestPartialTranscript);
                    }
                    return;
                }

                if (shouldRetryWithFallbackLanguage(error) && !retryingWithFallbackLanguage) {
                    retryWithFallbackLanguage(error);
                    return;
                }

                if (shouldRetryWithAutoLanguage(error) && !retryingWithAutoLanguage) {
                    retryWithAutoLanguage(error);
                    return;
                }

                if (shouldRetryAfterNoMatch(error) && !retryingAfterNoMatch) {
                    retryAfterNoMatch(error);
                    return;
                }

                if (shouldRetryAfterClientError(error) && !retryingAfterClientError) {
                    retryAfterClientError(error);
                    return;
                }

                if (deliveredFinalTranscript) {
                    return;
                }

                if (listener != null) {
                    listener.onVoiceError(message);
                }
            }

            @Override
            public void onResults(Bundle results) {
                String bestTranscript = pickBestTranscript(results);
                if (!TextUtils.isEmpty(bestTranscript)) {
                    latestPartialTranscript = bestTranscript;
                    deliveredFinalTranscript = true;
                    if (listener != null) {
                        listener.onVoiceText(bestTranscript);
                    }
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                String partialTranscript = pickBestTranscript(partialResults);
                if (!TextUtils.isEmpty(partialTranscript)) {
                    latestPartialTranscript = partialTranscript;
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });

        if (!startListeningWithIntent(primaryRecognizerIntent)) {
            if (listener != null) {
                listener.onVoiceError("Microphone permission denied or recognition service busy");
            }
        }
    }

    public void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    public void destroy() {
        if (speechRecognizer != null) {
            mainHandler.removeCallbacksAndMessages(null);
            speechRecognizer.destroy();
        }
    }

    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Speech service timeout. Ensure offline speech pack is installed.";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Speech service unavailable. Install language data in system voice settings.";
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_SERVER:
                return "Speech server error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Could not start voice capture. Please try again.";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "Couldn't understand that. Try saying 'Spent 250 on lunch at cafe'.";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Voice service is busy. Please try again.";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech detected. Tap mic and speak clearly.";
            case ERROR_TOO_MANY_REQUESTS:
                return "Too many voice requests. Please wait a moment and retry.";
            case ERROR_SERVER_DISCONNECTED:
                return "Speech service disconnected. Please try again.";
            case ERROR_LANGUAGE_NOT_SUPPORTED:
                return "Selected speech language is not supported on this device.";
            case ERROR_LANGUAGE_UNAVAILABLE:
                return "Speech language data is unavailable. Please download it in system voice settings.";
            case ERROR_CANNOT_CHECK_SUPPORT:
                return "Speech language support check failed.";
            case ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS:
                return "Speech language download is in progress. Please try again shortly.";
            default:
                return "Speech recognition error (" + errorCode + ")";
        }
    }

    private String pickBestTranscript(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches == null || matches.isEmpty()) {
            return null;
        }
        for (String candidate : matches) {
            if (!TextUtils.isEmpty(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    private boolean shouldFallbackToPartial(int errorCode) {
        return errorCode == SpeechRecognizer.ERROR_NO_MATCH
                || errorCode == SpeechRecognizer.ERROR_SPEECH_TIMEOUT;
    }

    private boolean shouldRetryWithFallbackLanguage(int errorCode) {
        return errorCode == ERROR_LANGUAGE_NOT_SUPPORTED
                || errorCode == ERROR_LANGUAGE_UNAVAILABLE;
    }

    private boolean shouldRetryWithAutoLanguage(int errorCode) {
        return retryingWithFallbackLanguage
                && (errorCode == ERROR_LANGUAGE_NOT_SUPPORTED
                || errorCode == ERROR_LANGUAGE_UNAVAILABLE);
    }

    private boolean shouldRetryAfterNoMatch(int errorCode) {
        return errorCode == SpeechRecognizer.ERROR_NO_MATCH;
    }

    private boolean shouldRetryAfterClientError(int errorCode) {
        return errorCode == SpeechRecognizer.ERROR_CLIENT;
    }

    private void retryWithFallbackLanguage(int errorCode) {
        retryingWithFallbackLanguage = true;
        Log.w(TAG, "Retrying voice with fallback language after error " + errorCode);
        retryListeningWithIntent(fallbackRecognizerIntent, "fallback language retry");
    }

    private void retryWithAutoLanguage(int errorCode) {
        retryingWithAutoLanguage = true;
        Log.w(TAG, "Retrying voice with auto language after fallback error " + errorCode);
        retryListeningWithIntent(autoLanguageRecognizerIntent, "auto language retry");
    }

    private void retryAfterNoMatch(int errorCode) {
        retryingAfterNoMatch = true;
        Log.w(TAG, "Retrying voice capture after no-match error " + errorCode);
        retryListeningWithIntent(noMatchRetryRecognizerIntent, "no-match retry");
    }

    private void retryAfterClientError(int errorCode) {
        retryingAfterClientError = true;
        Log.w(TAG, "Retrying voice after client error " + errorCode);
        retryListeningWithIntent(primaryRecognizerIntent, "client-error retry");
    }

    private void retryListeningWithIntent(Intent retryIntent, String reason) {
        try {
            speechRecognizer.cancel();
        } catch (Exception ignored) {
            // no-op
        }

        mainHandler.postDelayed(() -> {
            boolean started = startListeningWithIntent(retryIntent);
            if (!started && listener != null && !deliveredFinalTranscript) {
                listener.onVoiceError("Voice recognition unavailable after " + reason + ". Please try again.");
            }
        }, 150L);
    }

    private boolean startListeningWithIntent(Intent intent) {
        if (speechRecognizer == null || intent == null) {
            return false;
        }
        try {
            speechRecognizer.startListening(intent);
            return true;
        } catch (SecurityException | IllegalStateException e) {
            Log.e(TAG, "Failed to start listening", e);
            return false;
        }
    }

    private Intent createRecognizerIntent(String languageTag, boolean preferOffline, String languageModel) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, languageModel);
        if (!TextUtils.isEmpty(languageTag)) {
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag);
        }
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1200L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L);
        return intent;
    }
}
