# LectureMind privacy notice

Effective 16 November 2026 · Version 1.0.0 early access

LectureMind records lectures only after you confirm you have permission and Android grants microphone access. Obtain permission from your instructor and other participants before recording.

## Data on your phone

Lecture titles, subjects, recordings, bookmarks, transcripts, notes, reviewers and quiz attempts are stored in the app's private Android storage. Audio is not separately encrypted by LectureMind; protection depends on Android's app sandbox and device encryption. Provider API keys are encrypted using AES-GCM with an Android Keystore key. The Settings screen prevents screenshots of key entry. Android cloud backup is disabled.

LectureMind has no account system, ads, analytics or application server. It does not upload lectures automatically for AI processing.

## Optional AI processing

When you confirm **Send & generate**, the app sends lecture audio and/or transcripts and the information needed to generate study materials to Gemini or OpenAI, according to your selection. The provider receives your API credential to authorize the request. Internet access, model access, quota and provider charges may be required.

The app requests `store=false` for Gemini Interactions and OpenAI Responses. This does not guarantee zero provider retention. Review the selected provider's current terms and data policies before sending sensitive content. Cancellation stops local work/network requests where possible; a request already received by a provider may still be processed or billed.

## Deletion and exports

Deleting a lecture removes its local recordings, generated content, quiz attempts and temporary job files. You can remove saved provider keys in Settings. Uninstalling removes the app's local library and keys. Files exported to another app or shared with others are separate copies and must be managed there. Provider-side data is governed by the provider's policies.

## Questions or reports

Use the repository's issue tracker for general questions. GitHub issues are public: do not attach API keys, private audio, personal information or confidential lecture content. This notice describes the current source implementation; device and provider verification limits are listed in [VERIFICATION.md](VERIFICATION.md).
