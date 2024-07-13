# Minecraft-Inspired Android Game

![icon_image](https://github.com/user-attachments/assets/556c029b-b915-4c85-b7f9-27b25b2cca14.png=250x250)

## Overview
This is a Minecraft-inspired endless runner game for Android. The player controls a character riding a minecart, dodging obstacles, and collecting diamonds across five lanes.

## Features
- 5-lane gameplay
- Two difficulty modes: Normal, Hard
- Two game modes: TwoButton, Sensor
- Obstacle dodging and diamond collection
- Lives system with visual heart indicators
- Score and distance tracking
- Sound effects for crashes
- Vibration feedback
- High score system with leaderboard
- Google Maps integration for high-score locations

## Gameplay Modes
1. Two-Button (Slow): Move left or right with buttons (Normal difficulty)
2. Two-Button (Fast): Move left or right with buttons (Hard difficulty)
3. Sensor: Tilt the phone to move (uses accelerometer)

## Technical Details
- Developed for Android using Java
- Uses AndroidX libraries
- Implements WorkManager for game loop management
- Utilizes Glide for GIF animations
- Integrates Google Maps API for location features

## Setup
1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Run the app on an Android device or emulator

## Dependencies
- AndroidX AppCompat
- AndroidX ConstraintLayout
- Google Material Design Components
- WorkManager
- Glide
- Google Play Services (Maps)

## Permissions
- Vibration
- Location (for high score map feature)

## How to Play
1. Choose a gameplay mode from the main menu
2. Dodge obstacles (dripstones) by moving left or right
3. Collect diamonds to increase your score
4. Try to travel as far as possible without losing all lives

## Contributing
Feel free to fork the repository and submit pull requests for any improvements or bug fixes.

## Acknowledgements
- Minecraft by Mojang
