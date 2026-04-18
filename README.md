# Stickyfloat 📝✨

**Stickyfloat** is an Android application designed to provide a seamless "Floating Sticky Notes" experience. It allows users to create, manage, and view notes that "float" on top of other applications using a floating bubble interface, ensuring that important information is always within reach.

## 🚀 Key Features

- **Floating Bubble Interface**: Access your notes from anywhere on your Android device via a non-obtrusive floating overlay.
- **Persistent Notes**: Notes are stored locally, ensuring your thoughts and tasks are saved even after closing the app.
- **Lightweight & Fast**: Built with modern Android development practices for optimal performance.
- **Overlay Permissions**: Utilizes `SYSTEM_ALERT_WINDOW` permission to stay on top of other apps.

## 🛠️ Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Frameworks**: 
  - [Jetpack Compose](https://developer.android.com/compose) (Main UI)
  - XML Layouts (Secondary/Legacy components)
- **Architecture**: MVVM (Model-View-ViewModel) - *Work in Progress*
- **Database**: [Room](https://developer.android.com/training/data-storage/room) - *Integration in Progress*
- **Service**: Foreground Service for managing the floating bubble lifecycle.

## 📂 Project Structure

```text
app/src/main/java/ma/project/stickyfloat/
├── data/           # Data access layer (DAO, Database, Repository)
├── model/          # Note data models
├── services/       # FloatingBubbleService for the overlay logic
└── ui/             # User Interface (Activities, Adapters, Compose themes)
```

## 🏗️ Current Status

> [!NOTE]  
> This project is currently in the **initial scaffolding phase**. Most components are currently skeletons or placeholders.

- [x] Project Structure Setup
- [x] Basic Manifest Configuration (Permissions, Services)
- [x] Floating Bubble Logic Implementation
- [x] Room Database Integration for Note Persistence
- [x] Complete Note Editing/Viewing UI

## 🚦 Getting Started

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/Stickyfloat.git
   ```
2. **Open in Android Studio**:
   - Ensure you have the latest version of Flamingo/Giraffe or newer.
   - Recommended AGP version: 8.0+.
3. **Permissions**:
   - The app requires the **"Display over other apps"** permission to function. You will be prompted to grant this on first launch.

## 🤝 Contributing

Contributions are welcome! If you have ideas for new features or want to help with the implementation, feel free to open an issue or submit a pull request.

---
*Created with ❤️ for more productive Android workflows.*
