# Radi’s Construct — 0.3.0-cobalt

Minecraft Java 26.3 · Fabric Loader 0.19.5 · Fabric API 0.162.0+26.3 · Java 25.

Материали: cobalt, manyullyn, amethyst bronze, rose gold и hepatizon —
слитъци, късчета и блокове. Кобалтова руда в Nether, суров кобалт и суров блок.
Общо 7 блока, 18 предмета и 23 оригинални рецепти. Виж `docs/MATERIALS-PORT-BG.md`.

## Използване

Замени стария JAR с `radisconstruct-0.3.0-cobalt+26.3.jar` в `mods`.
Слитъците/късчетата са в Creative → Ingredients; металните блокове — Building Blocks;
рудата/суровият блок — Natural Blocks. Кобалтът се преработва в blast furnace.
Рудата се добавя само при генериране на нови Nether чанкове.
Останалите сплави засега са Creative съдържание; няма още топилня/сплавяване.
Инструменти, станции и модификатори предстоят.

## Проверки

Текущият резултат и конкретният CI run са в `docs/STAGE1-STATUS.json`.
- 19 850 оригинални файла са запазени непроменени.
- Gradle build, проверка на JAR и оригиналните ресурсни хешове.
- JUnit: Minecraft codecs, реално съвпадение/резултати на крафт рецептите,
  blast furnace, loot таблици и конфигурации на рудата.
- Fabric launcher зарежда пакетирания JAR до EULA gate.
- Няма стартиран свят или клиентски render/gameplay тест.

## Структура и build

`upstream/Hephaestus` пази пълния оригинал. `src/main/java` съдържа мигрирания код.
`scripts/port_material_resources.py` преобразува избраните оригинални ресурси;
`NOTICE.md` и `licenses/` пазят авторските уведомления.

`bash ./gradlew --no-daemon clean build` изисква Java 25 и Python 3.
CI допълнително изпълнява `verify_reference.py`, `verify_jar.py` и `smoke_server.py`.
