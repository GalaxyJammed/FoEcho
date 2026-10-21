<div align="center">

# FoEcho - Voice Memo Editor

**An offline Android app for recording, organising, searching and splitting voice memos, built with Jetpack Compose**

![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?logo=kotlin&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)
![Min SDK](https://img.shields.io/badge/Min%20SDK-29-blue)
![GitHub release](https://img.shields.io/github/v/release/galaxyjammed/FoEcho?logo=github&label=GitHub%20release)
![License](https://img.shields.io/badge/License-MIT-lightgrey?logo=mit)
![Project Status](https://img.shields.io/badge/Project%20Status-In%20Development-orange)

⭐ **If FoEcho helps you, please star this repository. It helps other users find the project.**

</div>

## What is FoEcho?

FoEcho gathers all your audio in one place: your voice memos, the recordings you make in the app, and any audio files you add. You can sort, search and favourite them, and split a long recording into named parts. Each part is saved as its own file. Everything stays on your device.

<div align="center">
  <img width="100" height="100" alt="FoEcho icon" src="https://github.com/user-attachments/assets/d8384bbc-acbd-4ce7-9add-00ea17468606" />
  <br>
  <em>"Every voice, in its place"</em>
</div>

## Features

### 🎙️ Recording
- Tap **New Recording** to record straight from the app
- **Pause and resume** while recording, with the timer stopped during pauses
- Recordings are saved as `.m4a` in `Recordings/FoEcho`
- After you stop, a prompt lets you **name the recording** or keep the default name
- If audio is playing when you start a recording, you're asked to confirm first

### 📂 Browse and Organise
- **All** shows every audio file on the device
- **Raw** shows recordings and files that haven't been edited
- **Edited** shows parts created in the editor
- **Favorites** shows starred files from any screen
- **Custom categories** let you add your own audio files, with a name of your choice
- Group headers appear when sorting by name, date or length, so files sit under clear sections
- A scrollbar on the right shows where you are in long lists

### 🔍 Sorting and Search
- **Sort by** Date Created, Date Edited, Alphabetical or Length
- The sort choice is saved and restored when you reopen the app
- **Search** opens above the tabs and filters as you type, so "te" already finds "test"
- Search works across All, Raw, Edited, Favorites and custom categories

### ⭐ Favorites
- Hold a file to select it, then tap the star to favourite it
- Mixed selections flip each file: favourites are removed and the rest are added
- A small star appears on the file wherever it shows up

### ▶️ Playback
- Tap a file to play it. The time counts down to 0:00 and resets when the file ends
- Pause and resume from the card
- Each file shows a faint waveform behind its name, so you can recognise it at a glance
- The waveform and length are cached on the device, so files only need to be decoded once

### ✂️ Editor
- Tap the pencil on any file to open the editor
- Tap the waveform to move the playhead, then press **Add Line** to place up to 5 separator lines
- Each area between lines appears as a block. Tap a block to name it and save it as a separate file
- Tap a **Line** block to jump the playhead to that separator
- Saved parts appear under **Categories**, where you can play, rename or delete them
- The original file is never changed

### 🗑️ Deleting
- Long-press to select files, then use the red delete button
- In custom categories, **Remove from category** only unlinks files, while **Delete** removes them from the device
- Every delete asks for confirmation first

### 🎨 Appearance
- Dark and light themes, switched in Settings
- Black and white base colours with teal and yellow accents

## Supported Audio Types

**Legend:** ✅ Supported · 🚧 Planned

| Format | Description | Status |
|---|---|---|
| `.m4a` | AAC audio, the format FoEcho records in | ✅ Play, edit, record |
| `.mp3` | MPEG Layer 3 | ✅ Play and edit |
| `.wav` | Waveform audio | ✅ Play and edit |
| `.ogg`, `.opus` | Ogg Vorbis and Opus | ✅ Play |
| `.aac`, `.3gp`, `.amr` | Common phone and voice formats | ✅ Play |
| Other formats | Anything Android can decode | 🚧 Depends on the device's codecs |

## Setup

1. Download the latest APK from [Releases](../../releases)
2. Install it and open FoEcho
3. Allow **audio access** when prompted, so FoEcho can find your voice memos
4. Allow **microphone access** the first time you tap New Recording

## Permissions and Privacy

- **Audio access** (`READ_MEDIA_AUDIO`, or `READ_EXTERNAL_STORAGE` on older Android versions) is needed to list your audio files.
- **Microphone access** (`RECORD_AUDIO`) is needed only when you record.
- FoEcho **does not request internet access**. It has no network code, analytics or crash reporting.
- Recordings, parts and categories stay on your device.

## Built With

- [Kotlin](https://kotlinlang.org/) and [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
- Android `MediaStore` for finding audio files
- `MediaRecorder` for recording, and `MediaCodec` and `MediaMuxer` for decoding and exporting parts
- `SharedPreferences` for categories, favourites, sort choice and edit records

## ❓ Frequently Asked Questions

### Does FoEcho work offline?
Yes. It needs no internet connection at any point.

### Will editing change my original recording?
No. Each part is saved as a new file. The original stays as it was.

### Why does FoEcho ask for microphone access?
Only for New Recording. It's never used in the background.

### Why are some recordings shown with a different name?
When you name a recording, FoEcho renames the file on your device. The name you choose is what appears in every list.

### Why does the editor take a moment to save a part?
Saving decodes the audio and encodes it again to produce an exact cut. Longer parts take longer. A progress bar shows the status.

### What is the minimum Android version?
Android 10 (API level 29) or newer.

## Bug Reports and Suggestions

- Report bugs or suggest features by opening an [issue](../../issues)
- When reporting a bug, include your Android version, the audio file type, and what the screen shows.

</div>
