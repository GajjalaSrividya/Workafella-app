package com.workafella.booking;

import jakarta.persistence.*;

@Entity
@Table(name = "rooms")
public class Room {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private int capacity;
    private boolean active = true;

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getCapacity() { return capacity; }
    public boolean isActive() { return active; }
}
