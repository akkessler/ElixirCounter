# ElixirCounter
Android overlay with speech recognition to count elixir for a popular mobile game.

## Installation
After installing the `.apk` to your device, you must ensure the app has the proper permissions to run.

Navigate to the application's settings and toggle `Draw over other apps` on. For my device running Android 7.0, I get to this through `Settings > Apps > Application Manager > ElixirCounter`. 

Next, launch the app and click allow when asked for permission to record audio.

## Usage

**To start counting,** hit the magenta play button in the center. This will activate the timers and voice recognition systems. 

**To interact with the counter,** speak the following commands:

_Note: Voice recognition works best in a quiet environment!_

| Command       | Result        |
| ------------- |:-------------:|
| pump          | +1            |
| one           | -1            |
| two           | -2            |
| three         | -3            |
| four          | -4            |
| five          | -5            |
| six           | -6            |
| seven         | -7            |
| eight         | -8            |
| nine          | -9            |
| ten           | -10           |

_The results of voice input are displayed along the left side of the screen._

**To stop the counter,** navigate to the notification drawer and hit the `Stop` or `Exit` actions.
