# Repacker
1.14.4+ Minecraft open-source deobfuscator using official Mojang mappings

(Work also with snapshots)

## Usage

### Command line

`java -jar Repaker.jar <cacheDir> <version>(-server)`

example:

`java -jar Repacker.jar . 1.15.2` to repack MC 1.15.2 client

and

`java -jar Repacker.jar . 1.15.2-server` to repack MC 1.15.2 server

pre-release version names are VER-preX (Ex: 1.15-pre1)

### Java

example:

```Java
import com.fox2code.repacker.utils.Repacker;
import java.io.File;

String version = "1.15.2";
Repacker repacker = new Repacker(new DirLayout.FlatDirLayout(new File("cache")));
repacker.repackClient(version);
File repackedClient = repacker.getClientRemappedFile(version);
```

## Add as library

### Gradle

```Groovy
repositories {
    mavenCentral()
    maven {
        url 'http://62.4.29.69/maven'
    }
}

dependencies {
<<<<<<< HEAD
    implementation 'com.fox2code:repacker:1.3.1'
=======
    implementation 'com.fox2code:repacker:1.3.0'
>>>>>>> d8e5d743882e7a4a55b86e754fb893751b75cce8
}
```

### Maven

```XML
	<repositories>
		<repository>
		    <id>puzzle-mod-loader</id>
		    <url>http://62.4.29.69/maven</url>
		</repository>
	</repositories>
  	<dependency>
	    <groupId>com.fox2code</groupId>
	    <artifactId>repacker</artifactId>
<<<<<<< HEAD
	    <version>1.3.1</version>
=======
	    <version>1.3.0</version>
>>>>>>> d8e5d743882e7a4a55b86e754fb893751b75cce8
	</dependency>
```
