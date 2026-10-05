# MyAssistant - Context-Aware Assistant App

A modern, dark-themed mobile assistant application with animated visualizations.

## Features

- **Dark Theme UI**: Sleek black background with vibrant green accents
- **Animated Visualizer**: Dynamic green circular animation indicating active listening
- **Modern Navigation**: Bottom navigation bar with contacts, assistant, and settings
- **Keyboard Input Option**: Switch between voice and keyboard input modes

## Getting Started

### Prerequisites

- Node.js (>=18)
- React Native development environment set up
- Android Studio (for Android) or Xcode (for iOS)

### Installation

1. Install dependencies:
```bash
npm install
```

2. For iOS (Mac only):
```bash
cd ios && pod install && cd ..
```

3. Run the app:
```bash
# For Android
npm run android

# For iOS
npm run ios
```

## Project Structure

```
MyAssistant/
├── App.tsx                 # Main app component
├── src/
│   ├── screens/
│   │   └── HomeScreen.tsx  # Home screen with greeting and visualizer
│   └── components/
│       ├── AnimatedVisualizer.tsx  # Animated green visualizer
│       └── BottomNavigation.tsx    # Bottom navigation bar
└── package.json
```

## Technologies

- React Native 0.73.0
- React Native Reanimated (for smooth animations)
- TypeScript
