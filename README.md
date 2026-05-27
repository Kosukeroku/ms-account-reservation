# Account reservation microservice

## UPGRB-12: Оптимизация SQL запросов

### Проблема

#### 1. GET /api/v1/clients/{id}
- Было: 2 запроса (клиент + счета)
- Требование: не более 1-2 запросов

#### 2. GET /api/v1/clients?lastName=Петров
- Было: 1 + N запросов (N = размер страницы)
- Требование: не более 2 запросов

### Решение

#### 1. Оптимизация GET /api/v1/clients/{id}

В `ClientRepository` добавлен метод с `@EntityGraph`:

    @EntityGraph(attributePaths = {"accounts", "accounts.status"})
    Optional<Client> findByIdWithAccounts(UUID id);

В `ClientService` метод `getClientById()` использует этот метод вместо обычного `findById()`.

**Результат:** 1 запрос

#### 2. Оптимизация GET /api/v1/clients

**Шаг 1.** Поиск клиентов с пагинацией:

    Page<Client> findByLastNameContainingIgnoreCase(String lastName, Pageable pageable);

**Шаг 2.** Создан DTO проекции:

    public interface AccountCountProjection {
        UUID getId();
        Long getCount();
    }

**Шаг 3.** Добавлен батч-метод в `ClientRepository`:

    @Query("SELECT c.id as id, COUNT(a) as count FROM Client c LEFT JOIN c.accounts a " +
           "WHERE a.status.name IN :statuses " +
           "AND c.id IN :clientIds GROUP BY c.id")
    List<AccountCountProjection> countActiveAccountsForClients(@Param("clientIds") List<UUID> clientIds,
                                                               @Param("statuses") List<String> statuses);

**Шаг 4.** В `ClientService.searchClients()` изменена логика:

    private static final List<String> ACTIVE_STATUSES = List.of("NEW", "IN_CREATION", "CREATED");

    List<UUID> clientIds = clientPage.getContent().stream()
            .map(Client::getId)
            .toList();

    List<AccountCountProjection> projections = clientRepository.countActiveAccountsForClients(clientIds, ACTIVE_STATUSES);

    Map<UUID, Long> activeAccountsCountMap = projections.stream()
            .collect(Collectors.toMap(AccountCountProjection::getId, AccountCountProjection::getCount));

**Результат:** 2 запроса (поиск + батч-подсчет)

# UPGRB-13: Apache Kafka

## Топик

- Название: client-events
- Количество партиций: 3 (KAFKA_TOPIC_PARTITIONS)
- Фактор репликации: 1 (KAFKA_TOPIC_REPLICATION_FACTOR)
- Ключ партиционирования: clientId

## Формат события

Событие в формате JSON:

- clientId: UUID – ID клиента, который изменился
- eventType: String – тип события (CREATED, UPDATED, DELETED)
- timestamp: Instant - время создания события
- eventId: String - уникальный ID события

## Стратегия идемпотентности

- Таблица idempotent_events в PostgreSQL с уникальным ограничением на event_id
- При получении события consumer проверяет existsByEventId()
- Если запись есть (дубликат), событие пропускается
- Если нет, сохраняется запись и выполняется бизнес-логика

## Настройки Producer
| Параметр | Значение |
|----------|----------|
| acks | all |
| retries | 3 |
| key-serializer | StringSerializer |
| value-serializer | JsonSerializer |

## Настройки Consumer
| Параметр | Значение |
|----------|----------|
| group-id | client-events-group |
| enable-auto-commit | false |
| ack-mode | manual |
| auto-offset-reset | earliest |

## Retry и Dead Letter Queue
| Параметр | Значение |
|----------|----------|
| max-attempts | 3 |
| initial-interval | 1000 |
| multiplier | 2.0 |
| dlt-suffix | .dlt |

При 3 неудачных попытках сообщение отправляется в client-events.dlt.

## Commit policy

- Ручной commit (ack.acknowledge()) вызывается только после успешной обработки
- При ошибке commit не происходит и сообщение будет перечитано
- После 3 неудач сообщение уходит в DLQ