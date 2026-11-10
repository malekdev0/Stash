# Stash

A small Android persistence library aiming to make local storage feel like ordinary Kotlin.

## Current status

This is the first development snapshot. It currently provides typed primitive key-value storage backed by Preferences DataStore.

Planned modules:
- `stash-core`: unified public API
- `stash-objects`: serializable object storage
- `stash-room`: collection/table API
- `stash-annotations`: annotations for generated database models
- `stash-processor`: KSP code generation

The Room/KSP modules are intentionally not included yet. We'll add them after the core API is tested.

## Quick start

```kotlin
val stash = Stash.create(context)

stash.put("username", "Malek")
stash.put("coins", 500)

val username = stash.get("username", "Guest")
val coins = stash.get("coins", 0)
```

All read/write operations are suspend functions except `observe`, which returns a Flow.

## Modules

- `:stash-core` — Android library containing the first public API.
- `:app` — tiny sample app demonstrating the API.

## Build

Open this folder in Android Studio, allow Gradle sync, then run the `app` configuration.

Requirements:
- Android Studio with Android Gradle Plugin 8.13.2 support
- JDK 17
- Android SDK 36

## License

Apache License 2.0. Add the standard license text before publishing.
