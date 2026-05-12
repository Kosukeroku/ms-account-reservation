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

