# 🎤 SpeechTwin - Your Personal Voice Health Companion

<div align="center">

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![Min SDK](https://img.shields.io/badge/minSdk-24-orange.svg)
![License](https://img.shields.io/badge/license-MIT-purple.svg)

**Transform your voice health with AI-powered analysis and real-time insights**

[Features](#-key-features) • [Screenshots](#-screenshots--demo) • [Installation](#-installation) • [How It Works](#-how-it-works) • [Tech Stack](#-tech-stack)

</div>

---

## 📖 About SpeechTwin

**SpeechTwin** is an innovative Android application that leverages cutting-edge AI and advanced
audio processing to analyze, monitor, and improve your vocal health. Whether you're a professional
speaker, singer, teacher, or simply want to maintain healthy vocal habits, SpeechTwin provides
comprehensive real-time feedback and personalized insights to help you achieve optimal voice
wellness.

Built for the RunAnywhere AI Hackathon, SpeechTwin demonstrates the power of on-device AI inference
combined with sophisticated audio analysis to deliver a complete voice health solution.

---

## ✨ Key Features

### 🔬 **Advanced Voice Analysis**

- **Real-time audio recording** with professional-grade waveform visualization
- **Comprehensive vocal metrics** including pitch, loudness, jitter, and shimmer
- **Health scoring system** (0-100) with color-coded feedback
- **AI-powered insights** with personalized recommendations

### 🎨 **3D Pitch Visualization**

- **Interactive 3D vocal fold simulation** showing real-time pitch dynamics
- **Beautiful circular pitch mapping** with amplitude-based coloring
- **Rotation controls** for exploring your voice from all angles
- **Professional-grade visual feedback** for understanding vocal patterns

### 🎮 **Pitch Matching Game**

- **Fun, gamified vocal training** to improve pitch accuracy
- **Multiple difficulty levels** (Easy, Medium, Hard, Expert)
- **Real-time pitch detection** with visual feedback
- **Score tracking** and performance statistics
- **Unlock new levels** as you improve

### 🏋️ **Vocal Exercises Library**

- **5+ guided exercises** for different vocal skills
- **Breathing techniques** for breath control
- **Humming scales** for pitch stability
- **Lip trills** for vocal cord relaxation
- **Progress tracking** with completion badges

### 📊 **Progress Dashboard**

- **7-day health trend visualization** with interactive charts
- **Daily, weekly, and all-time statistics**
- **Streak tracking** to maintain consistent practice
- **Goal setting** with progress indicators
- **Recording history** with detailed metrics

### 🧠 **Smart Insights**

- **Personalized vocal health tips** based on your analysis
- **Time-of-day recommendations** (morning, afternoon, evening)
- **AI-generated healthy voice suggestions** showing improvement potential
- **Before/after comparisons** with playback controls

### 💾 **Recording Management**

- **Organized recording library** with search and filter
- **Rename and favorite** recordings
- **Detailed metadata** (date, duration, file size, sample rate)
- **Waveform thumbnails** for quick visual identification
- **Export and share** functionality

---

## 🛠️ Tech Stack

### **Core Technologies**

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM with ViewModels
- **Concurrency**: Kotlin Coroutines & Flow

### **Audio Processing**

- **Recording**: Android AudioRecord API (16kHz, Mono, 16-bit PCM)
- **Analysis**: Custom DSP algorithms for pitch detection
- **Format**: WAV file export with proper headers
- **Processing**: Real-time amplitude tracking and FFT analysis

### **AI & ML**

- **RunAnywhere SDK**: On-device LLM inference
- **LlamaCpp Module**: Optimized inference engine with 7 ARM64 variants
- **Model**: Qwen 2.5 0.5B Instruct Q6_K (374 MB)
- **Processing**: Audio feature extraction and health scoring

### **Dependencies**

```kotlin
// RunAnywhere SDK
-RunAnywhereKotlinSDK - release.aar(4.01 MB)
-runanywhere - llm - llamacpp - release.aar(2.12 MB)

// Networking
-Ktor Client (3.0.3)
-OkHttp(4.12.0)
-Retrofit(2.11.0)

// Android Components
-Jetpack Compose BOM
-Material Icons Extended
-WorkManager(2.10.0)
-Room Database (2.6.1)
-Security Crypto (1.1.0-alpha06)
```

### **Voice Analysis Algorithms**

- **Pitch Detection**: Autocorrelation-based fundamental frequency estimation
- **Jitter Analysis**: Cycle-to-cycle frequency variation measurement
- **Shimmer Analysis**: Amplitude variation quantification
- **Health Scoring**: Multi-factor algorithm considering pitch stability, amplitude consistency, and
  vocal strain indicators

---

## 📱 Screenshots & Demo

### Home Screen

<div align="center">
<img src="docs/screenshots/home.png" width="250" alt="Home Screen"/>
<p><i>Clean, modern interface with quick stats and one-tap recording</i></p>
</div>

### Voice Analysis Results

<div align="center">
<img src="docs/screenshots/analysis.png" width="250" alt="Analysis Results"/>
<p><i>Comprehensive metrics with intelligent insights and recommendations</i></p>
</div>

### 3D Pitch Visualization

<div align="center">
<img src="docs/screenshots/3d-viz.png" width="250" alt="3D Visualization"/>
<p><i>Interactive 3D visualization of vocal fold dynamics</i></p>
</div>

### Pitch Matching Game

<div align="center">
<img src="docs/screenshots/game.png" width="250" alt="Pitch Game"/>
<p><i>Fun gamified training to improve pitch accuracy</i></p>
</div>

### Progress Dashboard

<div align="center">
<img src="docs/screenshots/dashboard.png" width="250" alt="Dashboard"/>
<p><i>Track your improvement with detailed statistics and trends</i></p>
</div>

> 🎥 **Video Demo**: [Watch SpeechTwin in Action](https://youtu.be/your-demo-video)

---

## 📥 Installation

### **Option 1: Download APK (Recommended)**

1. Download the latest APK from [Releases](https://github.com/yourusername/speechtwin/releases)
2. Enable "Install from Unknown Sources" in Android settings
3. Install and launch the app

### **Option 2: Build from Source**

#### Prerequisites

- Android Studio Hedgehog or newer
- JDK 17 or higher
- Android SDK 24+
- Gradle 8.0+

#### Steps

```bash
# Clone the repository
git clone https://github.com/yourusername/speechtwin.git
cd speechtwin

# Place SDK AARs in app/libs/
# Download from RunAnywhere SDK releases:
# - RunAnywhereKotlinSDK-release.aar
# - runanywhere-llm-llamacpp-release.aar

# Build the project
./gradlew clean assembleDebug

# Install on device
./gradlew installDebug
```

#### Gradle Build

```bash
# Release build
./gradlew assembleRelease

# Run tests
./gradlew test

# Generate signed APK
./gradlew bundleRelease
```

---

## 🚀 How It Works

### **Step 1: Record Your Voice**

1. Launch SpeechTwin
2. Grant microphone permission when prompted
3. Tap the large microphone button
4. Speak naturally for 10 seconds
5. Watch real-time waveform visualization

### **Step 2: Instant Analysis**

The app automatically processes your recording:

- **Pitch extraction** using autocorrelation
- **Jitter & shimmer** calculation for stability
- **Loudness measurement** in decibels
- **Health score** generation (0-100 scale)

### **Step 3: Review Results**

Comprehensive analysis dialog shows:

- Overall health score with interpretation
- Detailed voice metrics
- Personalized insights and recommendations
- AI-generated healthy voice comparison (if applicable)

### **Step 4: Track Progress**

Navigate to the Dashboard to:

- View 7-day health trends
- Monitor improvement over time
- Set and achieve vocal health goals
- Maintain practice streaks

### **Step 5: Improve with Exercises**

Access the Exercises library:

- Choose from 5+ guided exercises
- Follow visual and audio cues
- Complete exercises for badges
- Track your exercise completion

### **Step 6: Play the Pitch Game**

Challenge yourself:

- Match target pitches in real-time
- Progress through difficulty levels
- Earn high scores and unlock achievements
- Have fun while improving!

---

## 🎯 Features Breakdown

### **Voice Analysis Metrics**

#### 🎵 Pitch (Fundamental Frequency)

- **What it measures**: The primary frequency of your voice
- **Healthy range**:
    - Male: 85-180 Hz
    - Female: 165-255 Hz
- **Interpretation**: Consistent pitch indicates vocal stability

#### 📊 Loudness (Amplitude)

- **What it measures**: Voice volume in decibels
- **Healthy range**: -30 to -15 dB (normalized)
- **Interpretation**: Steady loudness shows breath control

#### 📈 Jitter (Frequency Variation)

- **What it measures**: Cycle-to-cycle pitch fluctuation
- **Healthy range**: < 1.5%
- **Interpretation**: Low jitter = stable vocal cords

#### 🌊 Shimmer (Amplitude Variation)

- **What it measures**: Cycle-to-cycle loudness fluctuation
- **Healthy range**: < 5%
- **Interpretation**: Low shimmer = consistent breath support

#### 💯 Health Score

- **Calculation**: Weighted composite of all metrics
- **85-100**: Excellent vocal health
- **70-84**: Good condition
- **50-69**: Fair, needs attention
- **< 50**: Concern, consult specialist

### **3D Pitch Visualization**

Experience your voice in stunning 3D:

- **Circular trajectory** mapped around central axis
- **Color coding** based on amplitude (quiet → loud)
- **Height variation** representing pitch changes
- **Interactive rotation** for complete 360° view
- **Real-time rendering** with smooth animations

### **Pitch Matching Game**

Train your ear and voice:

- **Target Generation**: Random pitches within comfortable range
- **Real-time Feedback**: Visual indicator shows pitch accuracy
- **Tolerance System**: Difficulty adjusts matching precision
- **Scoring**: Points based on speed and accuracy
- **Progression**: Unlock harder levels as you improve

### **Vocal Exercises**

Scientifically-designed exercises:

1. **Humming Scale** 🎵 (30s, Easy)
    - Improves pitch stability
    - Warms up vocal cords

2. **Sustained "Ah"** 😮 (10s, Medium)
    - Builds breath control
    - Strengthens diaphragm

3. **Lip Trills** 💨 (20s, Easy)
    - Relaxes vocal tension
    - Improves flexibility

4. **Siren Sounds** 🚨 (25s, Medium)
    - Expands vocal range
    - Smooth transitions

5. **Breath Control** 🫁 (15s, Easy)
    - 4-4-4 breathing pattern
    - Improves oxygen flow

### **Progress Dashboard**

Comprehensive analytics:

- **Today's Score**: Latest health score
- **Week Average**: 7-day rolling average
- **All-Time Best**: Personal record
- **Total Recordings**: Complete history count
- **Exercise Count**: Completed exercises
- **Streak**: Consecutive days with recordings
- **Trend Chart**: Visual 7-day progress graph
- **Goal Tracker**: Set and monitor targets

### **Smart Insights**

AI-powered recommendations:

- **Pitch Analysis**: Range classification and advice
- **Stability Warnings**: Jitter/shimmer alerts
- **Volume Guidance**: Loudness optimization tips
- **Daily Tips**: Time-specific vocal care advice
- **Healthy Voice AI**: Before/after comparison for improvement visualization

---

## 🔧 RunAnywhere SDK Integration

SpeechTwin leverages the **RunAnywhere SDK** for on-device AI inference:

### **SDK Components**

- **Core SDK** (4.01 MB): Component architecture and model management
- **LlamaCpp Module** (2.12 MB): Optimized inference with 7 ARM64 CPU variants

### **Key Features Used**

```kotlin
// SDK Initialization
RunAnywhere.initialize(
    context = applicationContext,
    apiKey = "dev",
    environment = SDKEnvironment.DEVELOPMENT
)

// Model Registration
LlamaCppServiceProvider.register()
addModelFromURL(
    url = "https://huggingface.co/...",
    name = "Qwen 2.5 0.5B Instruct Q6_K",
    type = "LLM"
)

// Model Management
RunAnywhere.downloadModel(modelId)
RunAnywhere.loadModel(modelId)
RunAnywhere.scanForDownloadedModels()

// Inference (Future Enhancement)
RunAnywhere.generateStream(prompt).collect { token ->
    // Process streaming response
}
```

### **Performance**

- **On-device inference**: No internet required after download
- **Privacy**: All data stays on device
- **Speed**: Optimized for ARM64 processors
- **Size**: Compact 374 MB model for efficient storage

### **Future AI Enhancements**

- Voice coaching chatbot
- Personalized exercise recommendations
- Speech pattern analysis
- Vocal health predictions
- Natural language voice tips

---

## 👥 Team

### **Developer**

**[Your Name]** - Full Stack Android Developer

- 🔗 [GitHub](https://github.com/yourusername)
- 🐦 [Twitter](https://twitter.com/yourusername)
- 💼 [LinkedIn](https://linkedin.com/in/yourusername)
- 📧 [Email](mailto:your.email@example.com)

### **Special Thanks**

- **RunAnywhere AI** - For the amazing on-device AI SDK
- **Hackathon Organizers** - For hosting this incredible event
- **Beta Testers** - For valuable feedback and bug reports

---

## 🏆 Hackathon Submission Info

### **Event Details**

- **Hackathon**: RunAnywhere AI Startup Hackathon 2025
- **Category**: Healthcare & Wellness
- **Submission Date**: January 2025
- **Project Duration**: 48 hours

### **Innovation Highlights**

✅ **Novel Use Case**: Voice health analysis on mobile  
✅ **On-Device AI**: Complete privacy with local inference  
✅ **Real-time Processing**: Instant feedback and visualization  
✅ **Gamification**: Engaging pitch matching game  
✅ **Professional Quality**: Production-ready UI/UX  
✅ **Comprehensive Features**: 6+ major feature modules

### **Technical Achievements**

- Advanced DSP algorithms for voice analysis
- Real-time 3D graphics with Compose Canvas
- Complex audio processing pipeline
- Sophisticated state management with Coroutines
- Professional-grade recording system
- Efficient caching and data persistence

### **Impact & Future Vision**

SpeechTwin aims to democratize voice health monitoring by providing professional-grade vocal
analysis tools to everyone. Future plans include:

- Integration with health tracking platforms
- Voice disorder early detection
- Telemedicine integration
- Multi-language support
- Cloud backup and sync
- Social features for vocal coaches

---

## 🔗 Resources & Links

### **Documentation**

-
📚 [Quick Start Guide](app/src/main/java/com/runanywhere/startup_hackathon20/QUICK_START_ANDROID.md)
- 📖 [SDK Documentation](https://github.com/RunanywhereAI/runanywhere-sdks/blob/main/CLAUDE.md)
- 🎓 [Voice Health Guide](docs/voice-health-guide.md)

### **External Resources**

- [RunAnywhere SDK Repository](https://github.com/RunanywhereAI/runanywhere-sdks)
- [Voice Science Research](https://www.ncvs.org/)
- [Vocal Health Resources](https://www.nidcd.nih.gov/health/voice-speech-and-language)

### **Community**

- 💬 [Discord Community](https://discord.gg/speechtwin)
- 🐛 [Report Issues](https://github.com/yourusername/speechtwin/issues)
- 💡 [Feature Requests](https://github.com/yourusername/speechtwin/discussions)

---

## 📋 Requirements

### **Device Requirements**

- **OS**: Android 7.0 (API 24) or higher
- **Storage**: 500 MB free space minimum
- **RAM**: 2 GB minimum (4 GB recommended)
- **Processor**: ARMv8 (64-bit) recommended
- **Microphone**: Required for recording

### **Permissions**

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
```

### **Network**

- Required for initial model download (374 MB)
- Offline mode available after model download

---

## 🐛 Troubleshooting

### **App crashes on startup**

- Ensure Android 7.0+ (API 24)
- Check available storage (500+ MB)
- Clear app cache and data
- Reinstall the application

### **Recording not working**

- Grant microphone permission in settings
- Check if another app is using microphone
- Test microphone with another app
- Restart device

### **Analysis takes too long**

- Close background apps to free memory
- Ensure device isn't in battery saver mode
- Try recording in a quieter environment
- Restart the app

### **Poor analysis results**

- Speak closer to microphone (10-15 cm)
- Record in quiet environment
- Speak naturally without shouting
- Ensure proper microphone positioning

### **Models not downloading**
- Check internet connection
- Verify storage space (500+ MB free)
- Try on WiFi instead of cellular
- Restart download from Models screen

---

## 📄 License

```
MIT License

Copyright (c) 2025 SpeechTwin

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 🔗 Resources & Links

### **Documentation**

-
📚 [Quick Start Guide](app/src/main/java/com/runanywhere/startup_hackathon20/QUICK_START_ANDROID.md)
- 📖 [SDK Documentation](https://github.com/RunanywhereAI/runanywhere-sdks/blob/main/CLAUDE.md)
- 🎓 [Voice Health Guide](docs/voice-health-guide.md)

### **External Resources**
- [RunAnywhere SDK Repository](https://github.com/RunanywhereAI/runanywhere-sdks)
- [Voice Science Research](https://www.ncvs.org/)
- [Vocal Health Resources](https://www.nidcd.nih.gov/health/voice-speech-and-language)

### **Community**

- 💬 [Discord Community](https://discord.gg/speechtwin)
- 🐛 [Report Issues](https://github.com/yourusername/speechtwin/issues)
- 💡 [Feature Requests](https://github.com/yourusername/speechtwin/discussions)

---

## 🌟 Acknowledgments

- **RunAnywhere AI Team** - For the groundbreaking on-device AI SDK
- **Hugging Face** - For model hosting infrastructure
- **Jetpack Compose Team** - For the amazing UI framework
- **Open Source Community** - For inspiration and support

---

<div align="center">

### Made with ❤️ for the RunAnywhere AI Hackathon 2025

**If you find SpeechTwin helpful, please consider giving it a ⭐ on GitHub!**

[⬆ Back to Top](#-speechtwin---your-personal-voice-health-companion)

</div>
