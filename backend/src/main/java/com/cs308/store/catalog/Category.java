package com.cs308.store.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A row of the categories table (V3, V4). A category without a parent is a top-level menu
 * entry (Women); one with a parent is a subcategory (Women > Tops). Categories are
 * deactivated, never deleted.
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** Used in the URL: /category/women-tops. Unique over all categories. */
    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    /** JPA needs an empty constructor; application code uses the one below. */
    protected Category() {
    }

    public Category(String name, String slug, Category parent) {
        this.name = name;
        this.slug = slug;
        this.parent = parent;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public Category getParent() {
        return parent;
    }

    public boolean isActive() {
        return active;
    }
}
