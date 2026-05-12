package kosukeroku.ms_account_reservation.dto;

import java.util.UUID;

public interface AccountCountProjection {
    UUID getId();
    Long getCount();
}