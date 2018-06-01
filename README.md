# cscc-recommender [![Build Status](https://travis-ci.org/chrisly-bear/cscc-recommender.svg?branch=master)](https://travis-ci.org/chrisly-bear/cscc-recommender) [![Coverage Status](https://coveralls.io/repos/github/chrisly-bear/cscc-recommender/badge.svg?branch=master)](https://coveralls.io/github/chrisly-bear/cscc-recommender?branch=master)

Recommender system for code completion. The algorithm is based on [CSCC: Simple, Efficient, Context Sensitive Code Completion](https://ieeexplore.ieee.org/document/6976073/) by Asaduzzaman, Muhammad, et al.

## Include in your project

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

### Gradle configuration

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

## Getting started

1. Download KaVE data set from [www.kave.cc/datasets](http://www.kave.cc/datasets), unzip, and put them in `Data/Events` and `Data/Contexts`.
2. Download our pre-trained model from the [release page](https://github.com/chrisly-bear/cscc-recommender/releases) and unpack it (`tar -xf cscc-model_z-all_s-6_c-170503.tar.lmza`) to `Data/Model`, or train your own.
3. (_optional_) Adjust parameters in `ch.uzh.ifi.seal.ase.cscc.utils.CSCCConfiguration`.
4. See `ch.uzh.ifi.seal.ase.cscc.RunMe` for sample code on how to get code completions and train your own model.
5. Check the [wiki](https://github.com/chrisly-bear/cscc-recommender/wiki) for more information.

## License

This project is licensed under the Apache License 2.0.
