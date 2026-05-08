Account reservation microservice

# UPGRB-12: Оптимизация SQL запросов

## Проблема

### 1. GET /api/v1/clients/{id}
- Было: 2 запроса (клиент + счета)
- Требование: не более 1 запроса

### 2. GET /api/v1/clients?lastName=Петров
- Было: 1 + N запросов (N = размер страницы)
- Требование: не более 2 запросов

## Решение

### 1. Оптимизация GET /api/v1/clients/{id}

В `ClientRepository` добавлен метод с `@EntityGraph`:

    @EntityGraph(attributePaths = {"accounts", "accounts.status"})
    Optional<Client> findByIdWithAccounts(UUID id);

В `ClientService` метод `getClientById()` использует этот метод вместо обычного `findById()`.

Результат: 1 запрос

### 2. Оптимизация GET /api/v1/clients

**Шаг 1.** Поиск клиентов с пагинацией (уже был, не менялся):

    Page<Client> findByLastNameContainingIgnoreCase(String lastName, Pageable pageable);

**Шаг 2.** Новый батч-метод для подсчета активных счетов:

    @Query(value = "SELECT c.id, COUNT(a.id) FROM client c " +
           "LEFT JOIN account a ON c.id = a.client_id " +
           "LEFT JOIN account_status s ON a.status_id = s.id " +
           "WHERE s.name IN ('NEW', 'IN_CREATION', 'CREATED') " +
           "AND c.id IN :clientIds GROUP BY c.id", nativeQuery = true)
    List<Object[]> countActiveAccountsForClients(@Param("clientIds") List<UUID> clientIds);

**Шаг 3.** В `ClientService.searchClients()` изменена логика:

    List<UUID> clientIds = clientPage.getContent().stream()
            .map(Client::getId)
            .toList();

    List<Object[]> results = clientRepository.countActiveAccountsForClients(clientIds);
    Map<UUID, Long> activeAccountsCountMap = new HashMap<>();
    for (Object[] row : results) {
        activeAccountsCountMap.put((UUID) row[0], (Long) row[1]);
    }

Результат: 2 запроса (поиск + батч-подсчет)