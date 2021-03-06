package ru.shvets.blog.models;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name="tag2post")
public class Tag2Post implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private long id;

    @Column(name="post_id", nullable = false)
    private long postId;

    @Column(name="tag_id", nullable = false)
    private long tagId;
}
