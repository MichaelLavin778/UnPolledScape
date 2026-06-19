# UnPolledScape

A RuneLite external plugin shell for the Plugin Hub.

## Development

RuneLite external plugins target Java 11. After installing a JDK, run the plugin
with:

```powershell
.\gradlew.bat run
```

The run task starts RuneLite in developer mode and loads
`com.unpolledscape.UnPolledScapePlugin`.

## Plugin Hub

Before submitting to the Plugin Hub:

1. Make sure this repository is public.
2. Fill in the final plugin description and tags in
   `runelite-plugin.properties`.
3. Commit and push the plugin repository.
4. In a fork of `runelite/plugin-hub`, create `plugins/unpolledscape` with:

```properties
repository=https://github.com/MichaelLavin778/UnPolledScape.git
commit=<full 40-character commit hash>
```

The Plugin Hub build uses `runelite-plugin.properties`; this project is set to
`build=standard` and has no third-party dependencies.
