package com.csbet.backend.skin;

import jakarta.persistence.*;

@Entity
@Table(name = "CSBET_RARITIES", schema = "CSBET")
public class Rarity {
    @Id @Column(length = 80) private String id;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, length = 7) private String color;

    protected Rarity() { }
    public Rarity(String id, String name, String color) { this.id = id; update(name, color); }
    public void update(String name, String color) { this.name = name; this.color = color; }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getColor() { return color; }
}
