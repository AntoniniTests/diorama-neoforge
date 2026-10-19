# Diorama — NeoForge 1.21.1 port

Port of [Diorama](https://github.com/SunkenInTime/minecraft-orthogonal-map) (MIT, © Dara Adedeji)
from Fabric to NeoForge 1.21.1. Client-only.

## Build

    ./gradlew build          # jar in build/libs/
    ./gradlew runClient      # dev client

Needs JDK 21 and internet access (downloads NeoForge, ModDevGradle, Gradle 8.14.3).
If a version in `gradle.properties` / `build.gradle` doesn't resolve, bump `neo_version`
(any 21.1.x) or the `net.neoforged.moddev` plugin version.

## How it was made

The upstream project is a Stonecutter multi-version build that already has a Fabric 1.21.1
target. The sources here are that 1.21.1 variant, resolved to plain Java, with the Fabric
parts replaced:

| Fabric                                   | NeoForge                                   |
|------------------------------------------|--------------------------------------------|
| `ClientModInitializer`                   | `@Mod(dist = CLIENT)` constructor          |
| `KeyBindingHelper`                       | `RegisterKeyMappingsEvent`                 |
| `HudRenderCallback`                      | `RegisterGuiLayersEvent.registerAboveAll`  |
| `ClientTickEvents.END_CLIENT_TICK`       | `ClientTickEvent.Post`                     |
| `ClientChunkEvents.CHUNK_LOAD`           | `ChunkEvent.Load` (client levels only)     |
| `ClientReceiveMessageEvents.GAME`        | `ClientChatReceivedEvent.System`           |
| `ClientCommandRegistrationCallback`      | `RegisterClientCommandsEvent`              |
| `ClientPlayConnectionEvents.DISCONNECT`  | `ClientPlayerNetworkEvent.LoggingOut`      |
| `ClientLifecycleEvents.CLIENT_STOPPING`  | `GameShuttingDownEvent`                    |
| `FabricLoader` config dir                | `FMLPaths.CONFIGDIR`                       |
| `FabricLoader` mapping resolver          | field looked up by type (any mappings)     |
| `fabric.mod.json`                        | `META-INF/neoforge.mods.toml`              |

Rendering code, shaders and the 7 mixins are unchanged from upstream's 1.21.1 build.
The Fabric client-gametest harness is not included.
