# Task City — prototype v0.1

Android 8.1+ native 3D prototype for the gamified task-city concept.

## Included now
- OpenGL ES 2 procedural miniature city (no external models yet)
- directional sun lighting and projected real-time shadows
- roads, sidewalks, crosswalks, trees, gardens, fences and placeholder houses
- pan + pinch zoom camera
- Arabic RTL task UI
- per-task countdown chosen by the user
- completing a task builds a house with animation
- expired tasks remove a house
- local persistence across app restarts
- Android 8.1 / API 27 CI smoke test

The temporary houses are intentionally procedural. They can later be replaced with GLB/glTF house assets while preserving the task logic and city progression system.
