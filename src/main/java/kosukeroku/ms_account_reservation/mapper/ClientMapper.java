package kosukeroku.ms_account_reservation.mapper;

import kosukeroku.ms_account_reservation.dto.*;
import kosukeroku.ms_account_reservation.model.Account;
import kosukeroku.ms_account_reservation.model.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface ClientMapper {
    @Mapping(target = "createdAt", expression = "java(toOffsetDateTime(client.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toOffsetDateTime(client.getUpdatedAt()))")
    ClientResponse toResponse(Client client);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "accounts", ignore = true)
    Client toEntity(ClientCreateRequest request);

    void updateEntity(ClientUpdateRequest request, @MappingTarget Client client);

    @Mapping(target = "createdAt", expression = "java(toOffsetDateTime(client.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toOffsetDateTime(client.getUpdatedAt()))")
    @Mapping(target = "accounts", source = "client.accounts")
    ClientDetailsResponse toDetailsResponse(Client client);

    @Mapping(target = "status", source = "status.name")
    AccountResponse toAccountResponse(Account account);

    ClientSearchResponse toSearchResponse(Client client);

    default OffsetDateTime toOffsetDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.atOffset(ZoneOffset.UTC) : null;
    }
}