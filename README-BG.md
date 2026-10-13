# Radi’s Construct — 0.2.0-materials

Minecraft Java 26.3 · Fabric Loader 0.19.5 · Fabric API 0.162.0+26.3 · Java 25.

Компилацията и зареждането на готовия JAR през Fabric са успешни.
Първи пренесени материали: cobalt и manyullyn — слитъци, късчета и блокове.
Запазени са оригиналните текстури, имена, свойства и 8 рецепти за
преобразуване 9:1 и 1:9. Виж `docs/MATERIALS-PORT-BG.md`.

## Проверки

GitHub Actions run: https://github.com/totevr31-ux/TinkersConstruct/actions/runs/38408424392

- 19 850 оригинални файла са непроменени.
- Gradle build и проверката на JAR минават.
- Истинският Fabric server launcher зарежда пакетирания JAR и регистрира материалите.
- Тестът спира преди EULA/стартиране на свят. Не е клиентски или пълен gameplay тест.

## Използване

Замени стария JAR с `radisconstruct-0.2.0-materials+26.3.jar` в `mods`.
Слитъците/късчетата са в Creative → Ingredients; блоковете — Building Blocks.
Още няма руда в света, сплавяване, топилни, инструменти, станции или модификатори.

## Структура и build

`upstream/Hephaestus` пази пълния оригинал. `src/main/java` съдържа само
мигрирания код. `scripts/port_material_resources.py` преобразува избрани
оригинални ресурси към 26.3; `NOTICE.md` и `licenses/` пазят авторските уведомления.

`bash ./gradlew --no-daemon clean build` изисква Java 25 и Python 3.
CI допълнително изпълнява `verify_reference.py`, `verify_jar.py` и `smoke_server.py`.
Локалната UNIX socket забрана остава; GitHub Actions е работещата build среда.
