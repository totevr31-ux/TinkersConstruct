# Radi's Construct — Етап 1

Автор: Radi4 · Mod ID: `radisconstruct` · Package: `com.radi.radisconstruct`
Цел: Minecraft Java 26.3 / Fabric / Java 25.

**Статус: основата и анализът са създадени; build е блокиран от средата.
Няма готов JAR за игра и няма доказано успешно зареждане. Етап 1 не е завършен.**

## Продължение: готов GitHub Actions workflow

Отвори `START-HERE-BG.md` за качване и получаване на JAR без локален Gradle.
Workflow: `.github/workflows/build.yml`. Точна диагноза: `docs/BUILD-DIAGNOSIS-BG.md`.
CI още не е изпълнен: GitHub връзката не предоставя достъпно хранилище.

## Какво съдържа проектът

- `build.gradle`, `settings.gradle`, фиксирани `gradle.properties`.
- Работещо стартиране на Gradle Wrapper с Gradle 9.6.0 и checksum проверка;
  Loom 1.17.21 действително се изтегля и започва конфигуриране.
- `src/main/java/com/radi/radisconstruct/RadisConstruct.java`: Fabric
  ModInitializer с точния mod ID и лог при инициализация.
- `src/main/resources/fabric.mod.json`: име, автор, entrypoint, MIT и точните
  минимални зависимости. Minecraft е ограничен до 26.3.
- `upstream/Hephaestus/`: целият оригинален source snapshot; не е включен в
  активната компилация. Нито една оригинална механика още не е пренесена.
- `LICENSE`, `NOTICE.md`, `licenses/`: оригинален MIT текст, авторски
  уведомления, attribution за графика/звук и лиценз за Gradle Wrapper.
- `docs/ARCHITECTURE-BG.md`: системи, зависимости, реални source пътища и Етап 2.
- `docs/DEPENDENCIES-BG.md`, `docs/evidence/`: версии и първични източници.
- `docs/logs/gradle-build-failed.log`: действителният Gradle stack trace.
- `docs/upstream-sha256.json`: SHA-256 за всеки оригинален файл.

## Защо няма JAR

Build стигна до конфигуриране на Fabric Loom и приключи с exit code 1:

```
Fabric Loom: 1.17.21
java.lang.ExceptionInInitializerError
Caused by: java.io.UncheckedIOException: java.net.SocketException: Operation not permitted
at net.fabricmc.loom.util.CurrentPlatform.isUnixDomainSocketsSupported(CurrentPlatform.java:95)
```

Loom проверява наличността на Unix domain sockets; средата отказва операцията.
Това се случва преди задачата `compileJava`. Не е установена компилационна
грешка в Java кода — компилаторът още не е достигнат. Не е доказано и че целият
build ще бъде успешен на друга машина. Забраната на средата не е заобикаляна.
Първоначалната мрежова/TLS настройка е решена с наличния proxy и системния
trust store, без изключване на проверката на сертификати; тези специфични
настройки не са записани в проекта.

## Реална следваща стъпка за довършване на Етап 1

В среда с JDK 25, интернет до Maven/Mojang/Gradle и поддръжка на необходимите
локални sockets, от директорията на проекта:

Windows PowerShell:
```
.\gradlew.bat --no-daemon clean build
```
Linux/macOS:
```
./gradlew --no-daemon clean build
```

Само ако build завърши успешно, очакваният runtime файл е
`build/libs/radisconstruct-0.1.0-stage1+26.3.jar`.
Файлът с `-sources.jar` не е мод за инсталиране.

За runtime тест: отделна инсталация Minecraft 26.3 с Fabric Loader 0.19.5,
Fabric API 0.162.0+26.3 и получения runtime JAR. Провери `latest.log` за:
`Radi's Construct initialized: stage 1 foundation (no gameplay modules ported yet).`
Отвори тестов свят и провери за crash при зареждане/излизане. За dedicated
server тест може да се използва `./gradlew runServer`; приемането на Minecraft
EULA е действие на оператора на тестовия сървър. Тези runtime тестове още
НЕ са изпълнени. На този етап няма предмети, рецепти или машини в играта.

Продължението е от този проект. Първо довършваме build и runtime проверката
на Етап 1; след това започваме пренасянето, описано в Етап 2.
