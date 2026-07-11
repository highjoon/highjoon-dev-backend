package com.highjoondev.api.post.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.Date;

@Entity
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    Long id;

    String title;
    String slug;
    Long parentId;
    Date createdAt;
    Date updatedAt;
}
