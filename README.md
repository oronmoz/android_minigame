# Android Minigame: Minecraft-Inspired Runner

## Description
A simple Android Minecraft-inspired runner game.
The player controls Steve as he rides a minecart. The aim is to dodge falling Dripstones by moving between three lanes.
The game features a custom UI with Minecraft-style graphics.

## Features
- Three-lane gameplay
- Falling obstacles (dripstones)
- Lives system with heart icons
- Vibration feedback on collisions
- Custom Minecraft-inspired graphics
- Responsive controls for moving between lanes

## Technical Details
- Developed for Android using Java
- Utilizes Android's RelativeLayout for game structure
- Custom game loop implementation for obstacle movement and collision detection
- Implements vibration feedback (requires VIBRATE permission)

## Setup
1. Clone the repository to your local machine.
2. Open the project in Android Studio.
3. Ensure you have the Android SDK installed with a minimum API level of [26].
4. Build and run the project on an Android emulator or physical device.

## How to Play
- Tap the left arrow to move left
- Tap the right arrow to move right
- Avoid colliding with falling dripstones
- The game ends when you lose all three lives

## Project Structure
- `MainActivity.java`: Main game logic and UI handling
- `activity_main.xml`: Layout file for the game UI
- `res/drawable/`: Contains all game graphics
- `res/values/dimens.xml`: Defines dimensions for game elements

## Permissions
- `android.permission.VIBRATE`: Used for vibration feedback on collisions

## Contributors
- [Oron Mozes]

## Acknowledgments
- Inspired by Minecraft, created by Mojang Studios
