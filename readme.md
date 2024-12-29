# AR Cities

AR Cities is an Android application that uses ARCore to create an augmented reality city. The app allows users to view 3D buildings and place cars in their real environment through their device's camera.

## Features

- Real-time AR plane detection
- Interactive car placement system
- Camera permission handling
- Support for horizontal surface detection

## Technical Requirements

- Android device with ARCore support
- Android SDK version that supports ARCore
- OpenGL ES 2.0 or higher
- Camera permissions

## Project Structure

- `MainActivity.kt` - Main entry point of the application, handles activity lifecycle and touch events
- `ARCoreSessionManager.kt` - Manages ARCore session initialization and lifecycle
- `GLRenderer.kt` - Main OpenGL renderer that coordinates all rendering components
- `CameraRenderer.kt` - Renders the camera texture from the arcore camera
- `BuildingRenderer.kt` - Handles rendering of procedurally generated buildings
- `CarRenderer.kt` - Manages car placement and rendering
- `PlaneRenderer.kt` - Renders detected AR planes, used for debugging.
- `CameraPermissionHelper.kt` - Handles camera permission requests and checks

### Rendering System

The rendering system is built on OpenGL ES 2.0 and consists of several components:

- Main GL renderer that coordinates all rendering activities
- Camera background rendering

### Building Generation

- Random heights
- Varied colors
- Proper scaling and positioning relative to detected planes
- Tracks and builds building for 5 detected planes

### Car Placement System

- Touch-based car placement
- Multiple car colors
- Maximum car limit 20

## Setup Instructions

1. Clone the repository
2. Open the project in Android Studio
3. Ensure you have the latest Android SDK and ARCore SDK installed
4. Build and run the application on an ARCore-supported device

## Usage

1. Launch the application and grant camera permissions
2. Point your device at a flat, horizontal surface
3. Wait for plane detection (indicated by on-screen message)
4. Once a plane is detected, the city will automatically appear
5. Tap on the surface to place cars (maximum 20 cars)
6. Move around to view the city from different angles

## Limitations

- Requires ARCore-compatible device
- Buildings are placed when initial plane is detected
- Buildings do not update as plane is updated
- Buildings are spawned to 5 diff planes

## Known Issues

- ArCore Hittest unreliable, falls back on own ray casting to place cars

## Future Improvements

- Add Occlusion
- Add lighting
- Add Collisions between objects
- Add Unit tests, were not added due to time constraint

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Acknowledgments

- Google ARCore team for AR functionality
- OpenGL ES documentation and community
- Android documentation and examples
- https://github.com/google-ar/arcore-android-sdk/tree/main/samples/hello_ar_java
- https://github.com/googlecreativelab/ar-drawing-java/blob/master/app/src/main/java/com/googlecreativelab/drawar/rendering/LineUtils.java

## Imgaes

- ![image 1](/assets/image1.png)
- ![image 2](/assets/image2.png)
- ![image 3](/assets/image3.png)
