package com.renaldyhidayatt.split_bill_api.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public final class GroupDtos {

    private GroupDtos() {
    }

    public record CreateGroupRequest(
            @NotBlank(message = "name must not be blank") String name,
            @NotEmpty(message = "participants must contain at least one name") List<@NotBlank String> participants
    ) {
    }

    public record ParticipantResponse(Long id, String name) {
    }

    public record GroupResponse(Long id, String name, Instant createdAt, List<ParticipantResponse> participants) {
    }
}