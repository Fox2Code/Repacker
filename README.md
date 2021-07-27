# Repacker
1.14.4+ Minecraft open-source deobfuscator using official Mojang mappings

(Work also with snapshots)

## Usage

### Gradle plugin

See [UDK](https://github.com/Fox2Code/UDK)

### Command line

`java -jar Repaker.jar <cacheDir> <version>(-server)`

example:

`java -jar Repacker.jar . 1.15.2` to repack MC 1.15.2 client

and

`java -jar Repacker.jar . 1.15.2-server` to repack MC 1.15.2 server

also you can use

`java -jar Repacker.jar . release` or `java -jar Repacker.jar . snapshot`  
to repack the latest snapshot or release of the game

pre-release version names are VER-preX (Ex: 1.15-pre1)

The id of `20w14∞` is actually `20w14infinite`

### Java

example:

```Java
import com.fox2code.repacker.Repacker;
import com.fox2code.repacker.layout.FlatDirLayout;
import java.io.File;

String version = "1.15.2";
Repacker repacker = new Repacker(new FlatDirLayout(new File("cache")));
repacker.repackClient(version);
File repackedClient = repacker.getClientRemappedFile(version);
```

## Add as library

WIP
