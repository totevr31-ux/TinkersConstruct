# Продължение на Етап 1 — точна диагноза

Прочетен е целият лог `logs/gradle-build-failed.log` (253 реда).
Допълнително е проверен bytecode на действително изтегления `fabric-loom-1.17.21.jar`
чрез JDK `javap -c -p`; резултатите са в `evidence/loom-1.17.21-*-bytecode.txt`.
Анализът не разчита само на source от подвижен GitHub клон.

## Верига на грешката

`LoomGradlePlugin.apply` → `OneDrive.verify` → `Platform.CURRENT` →
`CurrentPlatform.<clinit>` → конструктор → `isUnixDomainSocketsSupported()` →
`ServerSocketChannel.open(StandardProtocolFamily.UNIX)`.

Отварянето на локалния UNIX socket channel хвърля `SocketException: Operation not permitted`
на source ред 90. Loom обработва `UnsupportedOperationException` като „няма поддръжка“,
но `IOException` (включително тази SocketException) превръща в `UncheckedIOException`
на ред 95. Статичната инициализация завършва с `ExceptionInInitializerError`.

Това е създаване на канал, преди bind/listen, а не връзка към Maven, грешка в
порта на Minecraft сървъра или проблем в Java класа на мода. Наличието на
`OneDrive.verify` в stack trace не означава, че проектът се намира в OneDrive:
самото прочитане на текущата платформа задейства проверката и на Linux.
От Java грешката не може да се определи точно кой системен механизъм налага
забраната (например контейнерна политика); установен е отказът на операцията.

## Възможна ли е конфигурационна поправка?

За фиксирания Loom 1.17.21: constructor-ът изпълнява проверката безусловно.
В този път няма четене на Gradle property или Java system property за
изключването ѝ. Промяна в `loom {}` е твърде късна — plugin apply вече е спрял.

- `--no-daemon` вече присъства в неуспешния опит.
- Gradle worker count, heap, parallel build и file watching не контролират
  тази Loom проверка.
- IPv4/IPv6 и HTTP proxy настройки не управляват `StandardProtocolFamily.UNIX`.
- Предупреждението за `--enable-native-access` е отделно предупреждение и не
  е причината за конкретния отказ в stack trace.
- Промяна на `os.name` не е решение: извикването е безусловно.

Не са правени допълнителни повторни build опити в това продължение.
Не е модифициран Loom, не са подменяни проверки, нито са променяни ограничения
на средата. Няма основание да сменяме проверените зависимости на случаен принцип.

## Подготвен външен build

`.github/workflows/build.yml` ползва GitHub-hosted `ubuntu-latest`, Java 25
Temurin и съществуващия Gradle Wrapper 9.6.0. Не изисква ръчно инсталиране
на Gradle, credentials за Maven или repository secrets.

Build-ва се само root проектът с `src/main`. `upstream/Hephaestus` не е Gradle
subproject и не е добавен към sourceSets. Скрипт проверява всичките 19 850
оригинални файла спрямо SHA-256 inventory; `.gitattributes` пази техните bytes
при checkout и на Windows. Оригиналните вложени workflows остават референтни:
GitHub изпълнява само `.github/workflows/` в root.

CI използва `pipefail`, така че `tee` не скрива неуспешен Gradle exit code.
След успех се проверяват реалният runtime JAR, metadata, bytecode на основния
клас, Java 25 class version, лицензите и липсата на старите Hephaestus класове.
Само този JAR влиза в artifact, заедно с SHA-256 и инструкции; sources JAR не
се предлага за инсталиране. Логовете се качват и при build failure.

Workflow е подготвен, но НЕ е изпълнен. Проверката на GitHub достъпа върна
празни списъци за достъпни repositories и installed accounts. Няма избрано
достъпно хранилище, в което да качим кода и да пуснем build от тази сесия.
Не са направени промени в GitHub.

Успешният CI build ще докаже компилацията и структурата на JAR, но НЕ замества
зареждане на клиента/сървъра. Етап 2 остава незапочнат.
