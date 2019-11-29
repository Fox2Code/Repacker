![Release](https://jitpack.io/v/Fox2Code/Repacker.svg)
![](https://jitci.com/gh/Fox2Code/Repacker/svg)

# Repacker
1.14.4+ Minecraft open-source deobfuscator using official Mojang mappings

(Work also with snapshots)

## Usage

### Command line

`java -jar Repaker.jar <cacheDir> <version>(-server)`

example:

`java -jar Repacker.jar . 1.14.4` to repack MC 1.14.4 client

and

`java -jar Repacker.jar . 1.14.4-server` to repack MC 1.14.4 server

pre-release version names are VER-preX (Ex: 1.15-pre1)

### Java

example:

```Java
import com.fox2code.repacker.Repacker;
import java.io.File;

String version = "1.14.4";
Repacker repacker = new Repacker(new File("cache"));
repacker.repackClient(version);
File repackedClient = repacker.getClientRemappedFile(version);
```

## Add as library

Repacker use [JitPack](https://jitpack.io) as maven repository

### Gradle

```Groovy
repositories {
    mavenCentral()
    maven {
        url 'https://jitpack.io'
    }
}

dependencies {
    implementation 'com.github.Fox2Code:Repacker:1.2.0'
}
```

### Maven

```XML
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
  	<dependency>
	    <groupId>com.github.Fox2Code</groupId>
	    <artifactId>Repacker</artifactId>
	    <version>1.2.0</version>
	</dependency>
```
