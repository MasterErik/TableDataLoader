
---

# TableDataLoader Library & Ecosystem

**TableDataLoader** — это библиотека-оркестратор для управления динамическими запросами к табличным данным. Она выступает промежуточным слоем между внешним API и слоем данных (MyBatis), обеспечивая валидацию, фильтрацию, сортировку и пагинацию.

## 1. Философия Изоляции (Framework Agnostic)

**Критически важное правило:** Библиотека строго изолирована от Spring Framework, Jakarta EE или любых других веб-фреймворков.

* **POJO & Kotlin:** Все компоненты (`MapParam`, `TableDataLoader`) являются чистыми объектами.
* **Контекст (No Statics):** Используется `DataLoaderContext` для передачи зависимостей (`LoaderRegistry`, `MapParamProvider`). Статическое состояние исключено для обеспечения тестируемости и изоляции.
* **SPI (Service Provider Interface):** Взаимодействие с внешним миром реализуется через стандартный механизм `ServiceLoader`:
* Получение параметров из HTTP-контекста происходит через `MapParamProvider`.
* Регистрация компонентов (лоадеров/экспортеров) происходит через `LoaderDescriptor`.
* **Отсутствие JSON:** Внутри библиотеки (включая DTO) **запрещено** использование аннотаций Jackson/Gson (`@JsonProperty`), так как это нарушает принцип независимости от библиотек сериализации.

## 2. Ядро: MapParam (Data Transfer Object)

`MapParam` — это Kotlin-класс, являющийся "Единым Источником Истины" (Single Source of Truth) для состояния запроса.

### Структура данных

1. **`filters` (`MutableMap<String, Any?>`):**
* Хранит простые параметры (`limit`, `offset`, `userId`, `userRoles`) и кастомные фильтры.
* Инициализируется сразу (EAGER) для безопасного доступа через Reflection в MyBatis.


2. **`criteria` (`ArrayList<Filter>`):**
* Список условий для генерации SQL `WHERE`. Поддерживает цепочки `AND/OR` и скобки.


3. **`orderBy` (`ArrayList<SortParam>`):**
* Список полей сортировки. Использует строгий Enum `SortDirection` (ASC/DESC).


4. **`masterListId` (`List<Any>?`):**
* Список идентификаторов для реализации Master-Detail связей.



### Архитектура

* **Нет наследования:** Классы `Paging` и `Sort` удалены. Вся логика внутри `MapParam`.
* **Properties-Delegates:** Поля `limit`, `offset`, `userId` — это свойства, которые физически читают/пишут в карту `filters`.

## 3. Архитектура Загрузки (Loader Architecture)

Библиотека использует унифицированную архитектуру для импорта и экспорта данных.

### Компоненты
* **`LoaderRegistry`:** Центральный реестр, управляющий жизненным циклом компонентов. Создает экземпляры через рефлексию, требуя стандартные конструкторы.
* **`LoaderDescriptor`:** SPI-интерфейс для регистрации компонентов.
    * `getType()`: Определяет роль (`LOADER` или `EXPORTER`).
    * `getSupportedExtensions()`: Список расширений файлов (csv, zip, и т.д.).
* **`FileLoader` / `FileExporter`:** Единые интерфейсы для реализации логики загрузки и выгрузки.

### Работа с ZIP Архивами
Библиотека поддерживает прозрачную работу с архивами.

#### Импорт (ZipFileLoader)
* Реализует потоковую обработку через `AbstractIterator` и `ZipArchiveIterator`.
* Не распаковывает весь архив в память.
* Поддерживает рекурсивный вызов других лоадеров для файлов внутри архива (через `LoaderRegistry` в контексте).

#### Экспорт (ZipExporter)
* Реализован как декоратор (`ZipExporter`), оборачивающий результат работы любого другого экспортера (например, `CsvFileExporter`).
* Активируется в `TableDataLoader` методом `.archiveResult()`.
* **Пример**: `build(CsvFileExporter.class, "report")` с включенным архивированием создаст `report.zip`, внутри которого будет лежать `report.csv`.

## 4. Экосистема и Интеграция

`TableDataLoader` работает в связке с соседними библиотеками.

### A. Библиотека `file-import-export` (Import)

Отвечает за парсинг файлов.

* **Абстракция:** `TableDataLoader` вызывает `FileImporter`, не зная деталей формата (CSV, Excel).
* **Контракт:** Возвращает строго типизированный `ImportResultDTO` (uploadId, count), а не `Map`.

### B. Управление Ресурсами (ARM)
* **AutoCloseable:** Все ресурсы экспорта (`ExportedFile`, `ExportResource`) реализуют этот интерфейс.
* **Try-With-Resources:** Внутренние механизмы (чтение CSV, ZIP) строго следуют паттерну автоматического управления ресурсами.

## 5. Бизнес-Правила и Валидация

1. **Правило U007 (Пагинация и Сортировка):**
   Если задана пагинация (`limit` или `offset` != null), запрос **обязан** содержать параметры сортировки (`orderBy`). Иначе `mapParam.check()` выбрасывает исключение.
2. **Master-Detail Правила:**
* Для фильтрации "деталей" по списку "мастеров" используется `masterListId`.
* В MyBatis это должно разворачиваться в условие `WHERE master_id IN <foreach collection="mapParam.masterListId" ...>`.


3. **Безопасность Сортировки:**
   Направление сортировки парсится через `SortDirection.fromString()`. Некорректные значения безопасно превращаются в `ASC`.
4. **Пользователь:** `UserContext` отсутствует. `userId` и `userRoles` хранятся в `filters`.

## 6. Паттерн Использования (Fluent Builder)

Основной способ работы — цепочка вызовов через `TableDataLoader`. Загрузчик проксирует методы настройки параметров в `MapParam` и выполняет оркестрацию вызовов MyBatis.

```java
// 1. Подготовка MapParam (если нужно) или использование встроенного билдера
MapParam mapParam = new MapParam();
mapParam.setUserId(100L); // Пример установки контекста

// 2. Создание и выполнение загрузчика
DataResponse<Map<String, Object>> response = TableDataLoader.<Map<String, Object>>create() // Использует дефолтный контекст
    .setMapParam(mapParam)                   // Внедряем базовые параметры
    .filter("number", 10)                    // Простой фильтр (добавляет в filters)
    .addCriteria("name", "test №1")          // SQL условие (AND name = 'test №1')
    .addCriteria("id", "=", 2, Filter.SqlSuffix.OR) // OR id = 2
    .archiveResult()                         // (NEW) Автоматически упаковать результат в ZIP
    .useToGetData(testMapper::testSelect)    // Ссылка на метод MyBatis для выборки данных
    .useToCount(testMapper::count)           // Ссылка на метод MyBatis для подсчета (Total Count)
    .build(CsvFileExporter.class, "report"); // Экспорт в CSV (вернет report.zip)

// Пример сигнатуры методов Mapper:
// List<Map<String, Object>> testSelect(@Param("mapParam") MapParam mapParam);
// Long count(@Param("mapParam") MapParam mapParam);

```

## 7. Рекомендации для AI-Агентов
* При анализе кода учитывайте, что TableDataLoader делегирует хранение состояния объекту MapParam.
* При генерации SQL XML для MyBatis опирайтесь на коллекцию criteria для блока WHERE и коллекцию filters для простых параметров.
* Никогда не добавляйте зависимости от javax.servlet или spring-web в ядро библиотеки.
1. **Context Awareness:** При рефакторинге или создании тестов всегда используйте `DataLoaderContext` для изоляции. Избегайте статических вызовов `resetAndInitRegistries()`, если можно создать новый контекст.
2. **SPI Registration:** Для добавления поддержки нового формата создайте `LoaderDescriptor` и зарегистрируйте его в `META-INF/services`.
3. **Resource Safety:** Следите за закрытием потоков. `ExportResource` должен закрываться вызывающей стороной (например, в контроллере).
4. **Variable Naming:** Используйте только описательные имена переменных. Никаких `is`, `os`, `e`, `it` (кроме лямбд Kotlin).
5. **Refactoring Safety:** При рефакторинге помните, что `MapParam` — это Kotlin-класс. Используйте геттеры/сеттеры в Java (`setLimit(10)`), но property syntax в Kotlin (`limit = 10`).
6. **SQL Generation:** При анализе генерации SQL (MyBatis):
* `limit`/`offset` берутся из `filters`.
* `criteria` разворачиваются в условия `WHERE`.
* `masterListId` разворачивается в условие `IN (...)`.
7.
8. **Reflection:** Поле `filters` инициализируется сразу (`new HashMap`), что делает его безопасным для доступа через Reflection в фреймворках.
9. **Enum Access:** Ссылайтесь на направление сортировки как `MapParam.SortDirection.ASC`.