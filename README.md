# Valve Meta Aggregator

Консольное Java-приложение, которое опрашивает три открытых REST API (OpenDota, Deadlock, Steam Store), приводит данные к единой модели `AggregatedItem` и сохраняет их в файл (JSON или CSV). Поддерживает чтение файла и фильтрацию по источнику.

## Технологии

- Java 21
- Maven
- Jackson
- Apache Commons CSV
- SLF4J
- Java NIO

## API

| Source | Endpoint | Query params |
|--------|----------|--------------|
| OpenDota | `https://api.opendota.com/api/benchmarks` | `hero_id` |
| Deadlock | `https://api.deadlock-api.com/v1/assets/heroes` | `language` |
| Steam Store | `https://store.steampowered.com/api/appdetails` | `appids`, `cc`, `l` |

## 1. OpenDota Benchmarks

**URL:** `https://api.opendota.com/api/benchmarks`

Ответ — объект `{hero_id, result: {metric: [{percentile, value}, ...]}}`.

**Параметры:**

| Параметр | Тип | Обязательный | Описание |
|----------|-----|:---:|----------|
| `hero_id` | integer | ✅ | ID героя |
| `bracket` | integer | ⬜ | Ранговый брекет (1–8) |
| `role` | string | ⬜ | `core` или `support` |

**По умолчанию:** `hero_id=1`.

**Метрики (10):** `gold_per_min`, `xp_per_min`, `kills_per_min`, `deaths_per_min`, `assists_per_min`, `last_hits_per_min`, `denies_per_min`, `hero_damage_per_min`, `hero_healing_per_min`, `tower_damage`.

**Маппинг:**

- `externalId` ← `resolvedHeroId`
- `name` ← `"Dota2 Hero #<id>"`
- `metrics`: `<metric>_p50`, `<metric>_p90`
- `attributes`: `endpoint=benchmarks`, `hero_id`, `<metric>_points`

**Пример ответа:**

    {
      "hero_id": 55,
      "result": {
        "gold_per_min": [
          {"percentile": 0.5, "value": 486.0},
          {"percentile": 0.9, "value": 636.0}
        ]
      }
    }

**Пример записи:**

    {
      "source": "opendota",
      "entityType": "HERO",
      "externalId": "55",
      "name": "Dota2 Hero #55",
      "fetchedAt": "2026-11-19T18:59:13.156106765Z",
      "metrics": {
        "gold_per_min_p50": 486.0,
        "gold_per_min_p90": 636.0,
        "kills_per_min_p50": 0.14608695652173911,
        "kills_per_min_p90": 0.31537450722733246
      },
      "attributes": {
        "endpoint": "benchmarks",
        "hero_id": "55",
        "gold_per_min_points": 11,
        "kills_per_min_points": 11
      }
    }

## 2. Deadlock API

**URL:** `https://api.deadlock-api.com/v1/assets/heroes`

Ответ — массив объектов с вложенными `description`, `tags`, `popular_items`.

**Параметры:**

| Параметр | Описание |
|----------|----------|
| `language` | Язык описаний (`english`, `russian`, ...) |
| `hero_id` | Клиентский фильтр по id героя |

**По умолчанию:** `language=english`.

**Маппинг:**

- `externalId` ← `id`
- `name` ← `name` (fallback `"Hero #<id>"`)
- `attributes`: `class_name`, `hero_type`, `gender`, `language`, `player_selectable`, `development_state`, `role`, `playstyle`, `lore` (до 280 символов), `tags`
- `metrics`: `early_game_items_count`, `early_game_max_pick_pct`

**Пример ответа:**

    [
      {
        "id": 1,
        "name": "Infernus",
        "class_name": "hero_inferno",
        "hero_type": "mystic",
        "gender": "male",
        "player_selectable": true,
        "development_state": "release",
        "description": {
          "role": "Lights up enemies and watches them burn",
          "playstyle": "Infernus has many ways to deal damage over time...",
          "lore": "Like most teenagers; Infernus was wild..."
        },
        "tags": ["Arsonist", "Explosive", "Burn Rubber"],
        "popular_items": {
          "early_game": [{"id": 10, "pick_pct": 68.275085}]
        }
      }
    ]

**Пример записи:**

    {
      "source": "deadlock",
      "entityType": "HERO",
      "externalId": "1",
      "name": "Infernus",
      "fetchedAt": "2026-11-19T18:59:14.701811708Z",
      "metrics": {
        "early_game_items_count": 12,
        "early_game_max_pick_pct": 68.275085
      },
      "attributes": {
        "class_name": "hero_inferno",
        "hero_type": "mystic",
        "gender": "male",
        "language": "english",
        "player_selectable": true,
        "development_state": "release",
        "role": "Lights up enemies and watches them burn",
        "playstyle": "Infernus has many ways to deal damage over time...",
        "lore": "Like most teenagers; Infernus was wild...",
        "tags": ["Arsonist", "Explosive", "Burn Rubber"]
      }
    }

## 3. Steam Store API

**URL:** `https://store.steampowered.com/api/appdetails`

Ответ — объект `{ "<appid>": { success: true, data: {...} } }`.

**Параметры:**

| Параметр | Тип | Обязательный | Описание |
|----------|-----|:---:|----------|
| `appids` | string | ⬜ | appid через запятую (`570` — Dota 2, `730` — CS2) |
| `cc` | string | ⬜ | Регион: `us`, `ru`, `eu` |
| `l` | string | ⬜ | Язык: `english`, `russian`, `german` |

**По умолчанию:** `appids=570`, `cc=us`, `l=english`.

**Маппинг:**

- `externalId` ← ключ верхнего уровня (`appid`)
- `name` ← `data.name` (fallback `"App #<appid>"`)
- `metrics`: `metacritic_score`, `recommendations_total`, `price_final`, `price_initial`, `discount_percent`
- `attributes`: `type`, `is_free`, `required_age`, `short_description`, `website`, `header_image`, `cc`, `language`, `genres`, `categories_count`, `currency`, `price_final_formatted`

**Пример ответа:**

    {
      "570": {
        "success": true,
        "data": {
          "type": "game",
          "name": "Dota 2",
          "is_free": true,
          "required_age": 0,
          "short_description": "Every day, millions of players worldwide enter battle...",
          "website": "http://www.dota2.com/",
          "header_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/570/header.jpg",
          "genres": [
            {"description": "Action"},
            {"description": "Strategy"},
            {"description": "Free To Play"}
          ],
          "categories": [{"id": 1}, {"id": 2}],
          "metacritic": {"score": 90},
          "recommendations": {"total": 14374}
        }
      }
    }

**Пример записи:**

    {
      "source": "steam",
      "entityType": "GAME",
      "externalId": "570",
      "name": "Dota 2",
      "fetchedAt": "2026-11-19T18:59:15.275732268Z",
      "metrics": {
        "metacritic_score": 90,
        "recommendations_total": 14374
      },
      "attributes": {
        "type": "game",
        "is_free": true,
        "required_age": 0,
        "short_description": "Every day, millions of players worldwide enter battle as one of over a hundred Dota heroes...",
        "website": "http://www.dota2.com/",
        "header_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/570/header.jpg",
        "cc": "us",
        "language": "english",
        "genres": ["Action", "Strategy", "Free To Play"],
        "categories_count": 11
      }
    }

## Единая модель

`AggregatedItem` (иммутабельный, Builder):

| Поле | Тип | Описание |
|------|-----|----------|
| `source` | `String` | `opendota`, `deadlock`, `steam` |
| `entityType` | `EntityType` | `HERO`, `GAME`, `UNKNOWN` |
| `externalId` | `String` | ID в источнике |
| `name` | `String` | Имя |
| `fetchedAt` | `Instant` | Время получения (UTC) |
| `metrics` | `Map<String, Object>` | Числовые показатели |
| `attributes` | `Map<String, Object>` | Прочие атрибуты |

## Архитектура

- `config.AppFactory` — сборка зависимостей
- `model` — `AggregatedItem`, `EntityType`
- `source` — `DataSource`, `OpenDotaSource`, `DeadlockSource`, `SteamStoreSource`, `SourceRegistry`
- `http` — `ApiHttpClient`
- `storage` — `DataStorage`, `JsonDataStorage`, `CsvDataStorage`, `StorageFactory`
- `service` — `AggregationService`
- `cli` — парсер и раннер
- `exception` — `AppException` → `HttpException`, `ApiException`, `StorageException`, `ParsingException`

Новый API: класс implements `DataSource` + регистрация в `SourceRegistry`.

## Layout

    com.course.aggregator
    ├── Application
    ├── config.AppFactory
    ├── model          # AggregatedItem, EntityType
    ├── source         # DataSource + implementations + registry
    ├── http           # ApiHttpClient
    ├── storage        # DataStorage + json/csv
    ├── service        # AggregationService
    ├── cli            # parser + runner
    └── exception

## Сборка

    mvn clean package

Артефакт: `target/course-aggregator-<version>.jar`.

Без тестов:

    mvn clean package -DskipTests

## Запуск

**Опрос всех API в JSON:**

    java -jar target/course-aggregator.jar data \
      sources=opendota,deadlock,steam \
      format=json \
      mode=new \
      file=out.json

**Опрос выбранных API в CSV:**

    java -jar target/course-aggregator.jar data \
      sources=opendota,deadlock \
      format=csv \
      mode=new \
      file=out.csv

**Дозапись:**

    java -jar target/course-aggregator.jar data \
      sources=deadlock \
      format=json \
      mode=append \
      file=out.json

**Просмотр всего файла:**

    java -jar target/course-aggregator.jar show file=out.json

**Фильтр по источнику:**

    java -jar target/course-aggregator.jar show file=out.json source=deadlock

**Override параметров:**

    java -jar target/course-aggregator.jar data sources=opendota opendota.hero_id=53 format=json file=out.json

    java -jar target/course-aggregator.jar data sources=deadlock deadlock.language=russian format=json file=out.json

    java -jar target/course-aggregator.jar data sources=steam steam.appids=730 steam.cc=ru steam.l=russian format=json file=out.json

### Параметры `data`

| Параметр | Обязательный | Описание |
|----------|:---:|----------|
| `sources` | ✅ | `opendota`, `deadlock`, `steam` через запятую |
| `format` | ✅ | `json` или `csv` |
| `mode` | ✅ | `new` или `append` |
| `file` | ⬜ | По умолчанию `out.<формат>` |
| `<source>.<param>=<value>` | ⬜ | Override параметров источника |

### Параметры `show`

| Параметр | Обязательный | Описание |
|----------|:---:|----------|
| `file` | ✅ | Путь к `.json` или `.csv` |
| `source` | ⬜ | Фильтр: `opendota`, `deadlock`, `steam` |

## Форматы хранения

**JSON** — массив `AggregatedItem`, pretty-printed, пустые поля не выводятся.

**CSV** — заголовок + строки; `metrics`/`attributes` хранятся как JSON-строка в ячейке; append через `StandardOpenOption.APPEND`.

## Обработка ошибок

Неверные аргументы — сообщение и синтаксис. Недоступное API — лог и продолжение опроса остальных источников.

Иерархия: `AppException` → `ParsingException`, `HttpException`, `ApiException`, `StorageException`.
