# cscc-recommender [![Build Status](https://travis-ci.org/chrisly-bear/cscc-recommender.svg?branch=master)](https://travis-ci.org/chrisly-bear/cscc-recommender) [![Coverage Status](https://coveralls.io/repos/github/chrisly-bear/cscc-recommender/badge.svg?branch=master)](https://coveralls.io/github/chrisly-bear/cscc-recommender?branch=master)

Recommender system for code completion based on CSCC (ASE FS18).

### Maven configuration

Add this to the dependencyManagement section of your pom.xml:

```xml
<repositories>
  <repository>
    <id>tstrass-cscc-recommender</id>
    <url>https://packagecloud.io/tstrass/cscc-recommender/maven2</url>
    <releases>
      <enabled>true</enabled>
    </releases>
    <snapshots>
      <enabled>true</enabled>
    </snapshots>
  </repository>
</repositories>
```
Add this to your dependencies in your pom.xml:

```xml
<dependency>
  <groupId>ch.uzh.ifi.seal.ase</groupId>
  <artifactId>cscc</artifactId>
  <version>0.1</version>
</dependency>
```
### Gradle Configuration

Add this entry anywhere in your build.gradle file:

```gradle
repositories {
    maven {
        url "https://packagecloud.io/tstrass/cscc-recommender/maven2"
    }
}
```

Add this to your dependencies in your build.gradle file:

```gradle
compile 'ch.uzh.ifi.seal.ase:cscc:0.1'
```
