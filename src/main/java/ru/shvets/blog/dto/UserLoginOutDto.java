package ru.shvets.blog.dto;

import lombok.Data;

@Data
public class UserLoginOutDto {
    private Long id;
    private String name;
    private String photo;
    private String email;
    private boolean moderation;
    private int moderationCount;
    private boolean settings;
}
