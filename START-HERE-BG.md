# Как да получиш JAR чрез GitHub Actions

Това е продължение на същия Radi's Construct. Java, Gradle и build задачите
вече са настроени. Няма нужда да изпълняваш Gradle на своя компютър.
**Все още няма успешно компилиран JAR.**

## Качване чрез GitHub Desktop

За пълния проект с почти 20 000 референтни файла използвай GitHub Desktop,
за да не качваш файловете един по един през браузъра.

1. Разархивирай ZIP файла. Отвори GitHub Desktop и влез в своя GitHub акаунт.
2. Избери `File → Add local repository` и папката `Radis-Construct`, която
   съдържа `build.gradle`. Ако приложението съобщи, че тя не е Git repository,
   използвай предложеното `create a repository here`. Не избирай нова празна
   папка и не добавяй втори лиценз върху съществуващия.
3. Ако има неприключени промени, направи commit с име `Radi's Construct stage 1 CI`.
   Натисни `Publish repository`. Може да оставиш `Keep this code private` включено.
4. Отвори хранилището в GitHub. На началното ниво трябва да се виждат
   `build.gradle`, `settings.gradle`, `gradlew`, `gradle/`, `src/`, `upstream/`
   и `.github/workflows/build.yml`. Ако всички те са вътре в допълнителна
   папка `Radis-Construct`, workflow няма да бъде открит — качи съдържанието ѝ
   в корена на хранилището.
5. Отвори `Actions → Build Radi's Construct`. Build се стартира при push.
   За повторно изпълнение избери `Run workflow` върху default branch.
   Ако GitHub показва изключени Actions, активирай ги за това хранилище.
6. След зелен успешен run отвори `Artifacts → radisconstruct-mc26.3-…`.
   Разархивирай изтегления artifact. В него трябва да има:
   `radisconstruct-0.1.0-stage1+26.3.jar`, SHA256SUMS.txt и INSTALL-BG.txt.

Ако run е червен, отвори неуспешната стъпка или свали `build-diagnostics-…`.
Предай този лог за поправка; не преименувай ZIP или sources JAR на мод.
Workflow зависи от наличността на GitHub Actions и разрешените минути/политики
на акаунта. Не са нужни допълнителни secrets или ръчна Gradle конфигурация.

## Инсталиране и проверка

- Направи тестова Minecraft Java **26.3** инсталация с **Fabric Loader 0.19.5**,
  **Fabric API 0.162.0+26.3** и Java **25**.
- Постави получения runtime `.jar` в `mods`. Fabric API също трябва да е инсталиран.
- Пусни играта, отвори тестов свят и провери за crash.
- В `logs/latest.log` трябва да има:
  `Radi's Construct initialized: stage 1 foundation (no gameplay modules ported yet).`

Модът е минималната основа. Няма инструменти или Smeltery на този етап.
Зелен build доказва компилация; зареждането се потвърждава с реалния тест в играта.

## Ако вече използваш Git

От директорията със съществуващия проект можеш да commit/push-неш файловете
в собственото си хранилище по обичайния начин. Workflow се стартира автоматично.
Не качвай само ZIP файла — GitHub не го разархивира като проект.

Официални страници:
- https://desktop.github.com/
- https://docs.github.com/en/actions/managing-workflow-runs/manually-running-a-workflow
- https://docs.github.com/en/actions/managing-workflow-runs/downloading-workflow-artifacts
