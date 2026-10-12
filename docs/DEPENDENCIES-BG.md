# Проверени версии — 12 октомври 2026

| Компонент | Фиксирана версия | Основание |
|---|---|---|
| Minecraft Java | 26.3 | Mojang version manifest, release, 2026-09-15 |
| Fabric Loader | 0.19.5 | Fabric Meta endpoint за 26.3, stable=true |
| Fabric API | 0.162.0+26.3 | Публикувана версия в Fabric Maven metadata |
| Fabric Loom | 1.17.21 | Публикувана версия/POM в Fabric Maven; серия 1.17 е препоръчана за 26.3 |
| Gradle | 9.6.0 | Официалната препоръка на Fabric за 26.3; distribution SHA-256 от Gradle |
| JDK | Java 25; локално Temurin 25.0.4.1+1 | Mojang 26.3 metadata изисква majorVersion=25; действителен `java -version` |

Minecraft 26.3 е необфускирана: следва се актуалният Fabric example build
с plugin `net.fabricmc.fabric-loom`, `implementation` dependencies и без
стара mappings декларация. Версиите са фиксирани, без `+` wildcard/SNAPSHOT.
Знакът `+26.3` във версията на Fabric API е част от публикуваната версия.

Официални източници:
- https://piston-meta.mojang.com/mc/game/version_manifest_v2.json
- https://meta.fabricmc.net/v2/versions/loader/26.3
- https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml
- https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml
- https://maven.fabricmc.net/net/fabricmc/fabric-loom/1.17.21/fabric-loom-1.17.21.pom
- https://www.fabricmc.net/2026/09/15/263.html
- https://raw.githubusercontent.com/FabricMC/fabric-example-mod/26.3/build.gradle
- https://services.gradle.org/distributions/gradle-9.6.0-bin.zip.sha256
- https://api.adoptium.net/v3/binary/latest/25/ga/linux/x64/jdk/hotspot/normal/eclipse

Копия на metadata и SHA са в `docs/evidence/`. Публикуване в Maven само по
себе си не доказва успешен build/runtime; резултатът от изпълнението е отделно
в отчета. JDK не е включен в архива; за локален build се инсталира JDK 25.
