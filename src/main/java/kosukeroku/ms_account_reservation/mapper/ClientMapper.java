package kosukeroku.ms_account_reservation.mapper;

import kosukeroku.ms_account_reservation.dto.ClientCreateRequest;
import kosukeroku.ms_account_reservation.dto.ClientDetailsResponse;
import kosukeroku.ms_account_reservation.dto.ClientResponse;
import kosukeroku.ms_account_reservation.dto.ClientUpdateRequest;
import kosukeroku.ms_account_reservation.model.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;


@Mapper(componentModel = "spring")
public interface ClientMapper {
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ClientResponse toResponse(Client client);

    Client toEntity(ClientCreateRequest request);
    void updateEntity(ClientUpdateRequest request, @MappingTarget Client client);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "hasAccounts", constant = "false")
    ClientDetailsResponse toDetailsResponse(Client client);
}