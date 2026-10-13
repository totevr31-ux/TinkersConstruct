> Актуализация 13.10.2026: основата се компилира и минава Fabric bootstrap тест.
> Започнат е Етап 2 с cobalt/manyullyn storage content. Registry namespace `tconstruct`
> е запазен за оригиналните материали; mod ID остава `radisconstruct`.
> Долните раздели описват първоначалния анализ; текущият статус е в STAGE1-STATUS.json
> и MATERIALS-PORT-BG.md. Пълните material/tool системи още не са пренесени.

# Hephaestus → Radi's Construct: технически анализ

Изследван източник: GittuMC/Hephaestus, клон 1.20.1, commit
876eabc50c3c318f4dc214e31fcbd87083c09338. Всички 19 850 проследявани файла,
включително 1484 Java файла, са запазени в `upstream/Hephaestus/`.
Пътищата по-долу са относителни към `src/main/java/slimeknights/tconstruct/`
в този оригинален проект. Това е анализ на основните реализации, не твърдение,
че всеки клас е вече прегледан или пренесен.

## Структура и последователност

`TConstruct.onInitialize()` първо конфигурира мода, създава common/material/fluid/gadget
модулите, world модулите, tables/modifiers/parts/tools и smeltery, след което
изпълнява deferred регистрациите и networking setup. `commonSetup()` инициализира
материалите, дефинициите на инструментите и разположението на слотовете.
Клиентът има отделен entrypoint `shared/TinkerClient`. Datagen е отделен entrypoint.

| Система | Реална основа | Нужна адаптация за 26.3 |
|---|---|---|
| Блокове/предмети | `common/TinkerModule`, `TinkerCommons`, `TinkerMaterials`, `TinkerTables`, `TinkerToolParts`, `TinkerTools`, `TinkerSmeltery`; Mantle deferred register extensions и Porting Lib | Избор на реална поддържана регистрационна основа; нови registry key/property изисквания. Запазване на реда на инициализация и зависимостите. |
| Материали | `library/materials/MaterialRegistry`, `definition/MaterialManager`, stats и traits managers | Пренасяне на JSON reload loaders, условия, tags и server→client синхронизация; 26.3 reloadable registry модел. Да се запазят отделните definition/stats/traits. |
| Части | `library/tools/part/MaterialItem`, `ToolPartItem`, `tools/TinkerToolParts` | Материалът е данни върху ItemStack, не отделен Java клас за всеки метал. Пренасяне на компонентите и tooltip/model логиката. |
| Сглобяване | `library/recipe/tinkerstation/building/ToolBuildingRecipe`, `library/tools/definition/ToolDefinition*`, `library/tools/helper/ToolBuildHandler` | Recipe input/serializer и registry lookup API. Оригиналът валидира частите по слотове и строи `ToolStack` от списък материални варианти; този алгоритъм трябва да се запази. |
| Smeltery | `smeltery/block/entity/controller/SmelteryBlockEntity`, controller base classes, structure и module пакети | Multiblock валидиране, тикване, топлина, гориво, топене, сплави и взаимодействие с entities. Не може да се замени с обикновена пещ. |
| Течни метали | `fluids/TinkerFluids`, `smeltery/block/entity/tank/SmelteryTank`, `library/recipe/FluidValues` | Mantle FluidObject/attributes и Porting Lib FluidStack; актуализация на FluidVariant и rendering. Резервоарът е `SlottedStorage` със `SnapshotParticipant` и транзакции; да се пазят количествата, редът и rollback поведението. |
| Modifiers/Abilities | `library/modifiers/ModifierManager`, `tools/TinkerModifiers`, `library/tools/SlotType`, modifier hook/module пакети | Статични и JSON modifiers, hook dispatch, persistence, синхронизация. Ability е тип слот (`abilities`), не отделна магическа система. Има upgrades, defense и souls. |
| Рецепти | `library/recipe/`, `tools/data/`, `smeltery/data/`, `tables/data/`; `src/generated/resources/data/tconstruct/recipes` | Нови serializers/codecs и директории на datapack; запазване на количествата, температурите, времето, tags и условията. Генерираните рецепти се пазят като база за сравнение. |
| GUI | `tables/menu/`, `tables/client/`, `smeltery/client/screen/`, station layouts и Mantle widgets | Menu синхронизация, нов rendering/input API; отделяне на клиентските класове от dedicated server. |
| Текстури/модели | `src/main/resources/assets/tconstruct/`, `src/generated/resources/assets/tconstruct/`, `library/client/`, Mantle model loaders | Динамични материални модели и слоеве, оцветяване, atlas/model pipeline. Нужна е реална адаптация, не само копиране на JSON. |
| Мрежа | `common/network/TinkerNetwork` и пакетите `*/network/`, UpdateMaterials/Stats/Traits/Modifiers | Оригиналният SimpleChannel/Mantle протокол регистрира пакети по клас и посока. Да се пренесе към актуални typed payload codecs; server validation на входа и подходяща нишка за изпълнение. |
| Запазване | `library/tools/nbt/ToolStack`, MaterialNBT/ModifierNBT, tank `read/write`, `library/tools/capability/`, `datafix/schemas/` | Item data components, block/entity persistence и евентуални миграции. `tic_materials`, `tic_upgrades`, `tic_modifiers`, persistent/volatile data, damage и broken не трябва да се загубят. Старите светове не са обявени за съвместими. |

## Зависимости на оригинала

Стойностите тук са от оригиналните `build.gradle`, `gradle.properties` и
`fabric.mod.json`. Те НЕ са зависимости, потвърдени за 26.3.

- Minecraft 1.20.1, Java 17, Loader 0.14.21, Fabric API 0.90.4+1.20.1,
  Loom 1.5.+, Wrapper Gradle 8.6, official Mojang mappings.
- Mantle `1.20.1-1.9.269`: регистрация, networking, GUI/книги, модели,
  fluid abstractions и помощни класове. Критична зависимост за реалния порт.
- Porting Lib `2.3.4+1.20.1`: accessors, attributes, base, brewing,
  client_events, core, common, config, data, entity, extensions, items,
  models, model_loader, networking, tags, tool_actions, transfer, fluids,
  lazy_registration, loot, utility. FMJ задава особен минимум 3.0 за tags.
- milk-lib 1.2.58; dripstone-fluid-lib 3.0.1; Star 1.5+1.20.1;
  Architectury Fabric 9.1.12; MixinSquared 0.1.1.
- NightConfig core/toml 3.6.6; jsr305 3.0.2; Lombok 1.18.22;
  test compile dependency Mockito 2.23.4.
- REI 12.0.634 и JEI 15.2.0.23 са интеграции за рецептите.
  Оригиналният JEI runtime ред сочи 1.19.4 — несъответствие за проверка,
  което не е копирано в новия build.
- FMJ изисква reach-entity-attributes 2.4.0 и използва cardinal-components
  entrypoints; не всички тези зависимости са директно декларирани в build.
  Транзитивната им доставка трябва да се изясни при порта.
- Access widener, mixins и MixinSquared интеграцията трябва да се проверяват
  срещу действителните класове/методи на 26.3, не да се пренасят сляпо.

## Решение за Етап 1

Новият runtime съдържа един истински Fabric entrypoint, metadata и лог за
инициализация. Няма изкуствени класове за инструменти или фиктивни машини.
Оригиналът е цялостно запазен като техническа база извън runtime source set.
Това не е завършен порт и не означава, че Hephaestus се компилира за 26.3.
За този ограничен етап са нужни само Loader и Fabric API; старите библиотеки
не се обявяват за съвместими и не се включват в runtime.

## Етап 2 — последователност, без започната имплементация

1. Проверка на Mantle, Porting Lib и останалите библиотеки за 26.3;
   решение кои реализации ще адаптираме и какви лицензи изискват.
2. Регистрационна основа, namespace политика и data components за материал,
   части и състояние на инструмент. Картографиране `tconstruct` → `radisconstruct`
   с явна политика за бъдеща миграция; да не се прави масова текстова замяна.
3. Material definition/stats/traits и tool definitions, reload и синхронизация.
4. Първа пълна верига от оригинала: част → станция → инструмент → ремонт,
   със същите рецепти/показатели и клиентски модел.
5. Modifier slots/hooks и persistence; останалите инструменти.
6. Течности, топене, леене, сплави и multiblock Smeltery с оригиналното поведение.
7. GUI/книги/модели, recipe viewer интеграции, gadgets и worldgen.

Критерии при пренасянето: server authority, без дублиране/загуба на предмети,
round-trip на persistence и network данни, равенство на рецепти/статистики
с оригиналните fixtures. Етап 2 още не е започнат.
