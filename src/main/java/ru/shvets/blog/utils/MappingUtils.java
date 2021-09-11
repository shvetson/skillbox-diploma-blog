package ru.shvets.blog.utils;

import org.springframework.stereotype.Service;
import ru.shvets.blog.dto.CommentDto;
import ru.shvets.blog.dto.PostCommentDto;
import ru.shvets.blog.dto.UserDto;
import ru.shvets.blog.dto.UserShortDto;
import ru.shvets.blog.models.Post;
import ru.shvets.blog.models.PostComment;
import ru.shvets.blog.models.Tag;
import ru.shvets.blog.models.User;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MappingUtils {
    public PostCommentDto mapToPostCommentDto(Post post) {
        PostCommentDto dto = new PostCommentDto();

        dto.setId(post.getId());
        dto.setTimestamp(post.getTime().getTime() / 1000);
        dto.setActive(post.getIsActive() == 1);
        dto.setUser(mapToUserShortDto(post.getUser()));
        dto.setTitle(post.getTitle());
        dto.setText(post.getText());
        dto.setLikeCount((int) post.getListVotes().stream().filter(item -> item.getValue() == 1).count());
        dto.setDislikeCount((int) post.getListVotes().stream().filter(item -> item.getValue() == -1).count());
        dto.setViewCount(post.getViewCount());
        dto.setComments(mapToListCommentDto(post.getListComments()));
        dto.setTags(post.getTagList().stream().map(Tag::getName).toArray(String[]::new));

        return dto;
    }

    public CommentDto mapToCommentDto(PostComment postComment) {
        CommentDto dto = new CommentDto();

        dto.setId(postComment.getId());
        dto.setTimestamp(postComment.getTime().getTime() / 1000);
        dto.setText(postComment.getText());
        dto.setUser(mapToUserDto(postComment.getUser()));

        return dto;
    }

    public List<CommentDto> mapToListCommentDto(List<PostComment> list) {
        return list.stream().map(this::mapToCommentDto).collect(Collectors.toList());
    }

    public UserShortDto mapToUserShortDto(User user){
        UserShortDto dto = new UserShortDto();

        dto.setId(user.getId());
        dto.setName(user.getName());

        return dto;
    }

    public UserDto mapToUserDto(User user){
        UserDto dto = new UserDto();

        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setPhoto(user.getPhoto());

        return dto;
    }

}
