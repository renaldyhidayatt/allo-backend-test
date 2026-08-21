package com.renaldyhidayatt.split_bill_api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bill_groups")
@Getter
@Setter
@NoArgsConstructor
public class BillGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "billGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participant> participants = new ArrayList<>();

    public BillGroup(String name) {
        this.name = name;
    }

    public void addParticipant(Participant participant) {
        participants.add(participant);
        participant.setBillGroup(this);
    }
}
