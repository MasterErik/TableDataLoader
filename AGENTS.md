
---

# TableDataLoader Library & Ecosystem

**TableDataLoader** — это библиотека-оркестратор для управления динамическими запросами к табличным данным. Она выступает промежуточным слоем между внешним API и слоем данных (MyBatis), обеспечивая валидацию, фильтрацию, сортировку и пагинацию.

## 1. Философия Изоляции (Framework Agnostic)

**Критически важное правило:** Библиотека строго изолирована от Spring Framework, Jakarta EE или любых других веб-фреймворков.

* **POJO & Kotlin:** Все компоненты (`MapParam`, `TableDataLoader`) являются чистыми объектами.
* **SPI (Service Provider Interface):** Взаимодействие с внешним миром реализуется через интерфейсы и функциональные ссылки:
* Получение параметров из HTTP-контекста происходит через утилиты (например, `HeaderUtils`), принимающие `Function<String, String>`, а не `HttpServletRequest`.
* Получение мапперов происходит через передачу ссылок на методы (`testMapper::select`), а не через внедрение бинов Spring внутри библиотеки.


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

## 3. Экосистема и Интеграция

`TableDataLoader` работает в связке с соседними библиотеками.

### A. Библиотека `file-import-export` (Import)

Отвечает за парсинг файлов.

* **Абстракция:** `TableDataLoader` вызывает `FileImporter`, не зная деталей формата (CSV, Excel).
* **Контракт:** Возвращает строго типизированный `ImportResultDTO` (uploadId, count), а не `Map`.

### B. Библиотека работы с Архивами (Archives)

Новая библиотека для потоковой обработки ZIP-архивов.

* **Потоковая обработка:** Использует `ArchiveIterator` для обхода файлов без полной распаковки.
* **Правило Именования:** При импорте архива в бизнес-логику передается **имя самого архива**. Имена вложенных файлов используются только технически (для выбора стратегии парсинга).
* **Результат:** Метод `buildArchive` возвращает список результатов, где каждый элемент привязан к одной логической загрузке.

## 4. Бизнес-Правила и Валидация

1. **Правило U007 (Пагинация и Сортировка):**
   Если задана пагинация (`limit` или `offset` != null), запрос **обязан** содержать параметры сортировки (`orderBy`). Иначе `mapParam.check()` выбрасывает исключение.
2. **Master-Detail Правила:**
* Для фильтрации "деталей" по списку "мастеров" используется `masterListId`.
* В MyBatis это должно разворачиваться в условие `WHERE master_id IN <foreach collection="mapParam.masterListId" ...>`.


3. **Безопасность Сортировки:**
   Направление сортировки парсится через `SortDirection.fromString()`. Некорректные значения безопасно превращаются в `ASC`.
4. **Пользователь:** `UserContext` отсутствует. `userId` и `userRoles` хранятся в `filters`.

## 5. Паттерн Использования (Fluent Builder)

Основной способ работы — цепочка вызовов через `TableDataLoader`. Загрузчик проксирует методы настройки параметров в `MapParam` и выполняет оркестрацию вызовов MyBatis.

```java
// 1. Подготовка MapParam (если нужно) или использование встроенного билдера
MapParam mapParam = new MapParam();
mapParam.setUserId(100L); // Пример установки контекста

// 2. Создание и выполнение загрузчика
DataResponse<Map<String, Object>> response = TableDataLoader.<Map<String, Object>>create()
    .setMapParam(mapParam)                   // Внедряем базовые параметры
    .filter("number", 10)                    // Простой фильтр (добавляет в filters)
    .addCriteria("name", "test №1")          // SQL условие (AND name = 'test №1')
    .addCriteria("id", "=", 2, Filter.SqlSuffix.OR) // OR id = 2
    .openBracket()                           // (
        .addCriteria("name", "test №2")      // name = 'test №2'
    .closeBracket()                          // )
    .useToGetData(testMapper::testSelect)    // Ссылка на метод MyBatis для выборки данных
    .useToCount(testMapper::count)           // Ссылка на метод MyBatis для подсчета (Total Count)
    .build();                                // Выполнение запроса, валидация U007 и сборка ответа

// Пример сигнатуры методов Mapper:
// List<Map<String, Object>> testSelect(@Param("mapParam") MapParam mapParam);
// Long count(@Param("mapParam") MapParam mapParam);

```

## 6. Рекомендации для AI-Агентов
* При анализе кода учитывайте, что TableDataLoader делегирует хранение состояния объекту MapParam.
* При генерации SQL XML для MyBatis опирайтесь на коллекцию criteria для блока WHERE и коллекцию filters для простых параметров.
* Никогда не добавляйте зависимости от javax.servlet или spring-web в ядро библиотеки.
1. **Refactoring Safety:** При рефакторинге помните, что `MapParam` — это Kotlin-класс. Используйте геттеры/сеттеры в Java (`setLimit(10)`), но property syntax в Kotlin (`limit = 10`).
2. **SQL Generation:** При анализе генерации SQL (MyBatis):
* `limit`/`offset` берутся из `filters`.
* `criteria` разворачиваются в условия `WHERE`.
* `masterListId` разворачивается в условие `IN (...)`.
3. 
4. **Reflection:** Поле `filters` инициализируется сразу (`new HashMap`), что делает его безопасным для доступа через Reflection в фреймворках.
4. **Enum Access:** Ссылайтесь на направление сортировки как `MapParam.SortDirection.ASC`.
