package ru.shvets.blog.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.shvets.blog.api.responses.ErrorResponse;
import ru.shvets.blog.components.MapSessions;
import ru.shvets.blog.dto.*;
import ru.shvets.blog.exceptions.NoSuchPostException;
import ru.shvets.blog.models.*;
import ru.shvets.blog.repositories.PostRepository;
import ru.shvets.blog.repositories.UserRepository;
import ru.shvets.blog.utils.MappingUtils;
import ru.shvets.blog.utils.TimeUtils;

import javax.servlet.http.HttpSession;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final TagService tagService;
    private final MappingUtils mappingUtils;
    private final HttpSession httpSession;
    private final MapSessions mapSessions;
    private final TimeUtils timeUtils;

    public Sort sort(String mode) {
        switch (mode) {
            case "popular":
                return Sort.by("comments").descending();
            case "best":
                return Sort.by("votes").descending();
            case "early":
                return Sort.by("time");
            default:
                return Sort.by("time").descending();
        }
    }

    public PostCountDto response(Page<Post> page) {
        PostCountDto dto = new PostCountDto();
        dto.setCount((page != null) ? page.getTotalElements() : 0);
        dto.setPosts((page != null) ? mappingUtils.mapToListPostDto(page.toList()) : List.of(new PostDto[]{}));
        return dto;
    }

    public PostCountDto getAllPosts(int offset, int limit, String mode) {
        if (mode.equals("popular")) {
            return response(postRepository.
                    findAllIsActiveAndIsAcceptedAndComments(PageRequest.of(offset, limit, sort(mode))));
        } else if (mode.equals("best")) {
            return response(postRepository.
                    findAllIsActiveAndIsAcceptedAndVotes(PageRequest.of(offset, limit, sort(mode))));
        } else {
            return response(postRepository.
                    findAllIsActiveAndIsAccepted(PageRequest.of(offset, limit, sort(mode))));
        }
    }

    public PostCountDto getAllPostsByQuery(int offset, int limit, String query) {
        return response(postRepository.findByIsActiveAndModerationStatusAndTitleContaining((byte) 1,
                ModerationStatus.ACCEPTED,
                query,
                PageRequest.of(offset, limit, sort("recent"))));
    }

    public Map<String, Object> getAllPostsByYear(int year) throws ParseException {
        SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, Integer> mapDatePostCount = new HashMap<>();

        LocalDate date = LocalDate.now();
        if (year == 0) {
            year = date.getYear();
        }

        int[] years = postRepository.findAllYears().stream().mapToInt(Number::intValue).toArray();
        List<Object> resultList = postRepository.countPostsByDate(formatDate.parse(year + "-01-01"), formatDate.parse((year + 1) + "-01-01"));

        for (Object item : resultList) {
            Object[] object = (Object[]) item;
            Date datePost = (Date) object[0];
            int countPosts = ((BigInteger) object[1]).intValue();
            mapDatePostCount.put(datePost.toString(), countPosts);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("years", years);
        response.put("posts", mapDatePostCount);
        return response;
    }

    public PostCountDto getAllPostsByDate(int offset, int limit, String date) throws ParseException {
        SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");
        Date byDate;

        if (date != null) {
            byDate = formatDate.parse(date);
        } else {
            byDate = new Date();
        }
        Date dateAfter = byDate;
        LocalDate localDateBefore = new java.sql.Date(byDate.getTime()).toLocalDate();
        Date dateBefore = java.sql.Date.valueOf(localDateBefore.plusDays(1));

        return response(postRepository.findByIsActiveAndModerationStatusAndDate(dateAfter, dateBefore, PageRequest.of(offset, limit, sort("recent"))));
    }

    public PostCountDto getAllPostsByTag(int offset, int limit, String tag) {
        return response(postRepository.findByIsActiveAndModerationStatusAndTag(tag, PageRequest.of(offset, limit, sort("recent"))));
    }

    public PostCommentDto getPostById(Long postId) {
        Post post = postRepository.findPostByIdAndIsActiveAndModerationStatus(postId, (byte) 1, ModerationStatus.ACCEPTED);

        if (post == null) {
            throw new NoSuchPostException("Записи с id=" + postId + " в базе данных нет.");
        }

        String sessionId = httpSession.getId();
        Long userId = mapSessions.getUserId(sessionId);

        if (userId != null) {
            User user = userRepository.findUserById(userId);

            if (user.getIsModerator() != 1 & post.getUser().getId() != userId) {
                increaseViewCount(post);
            }
        }
        return mappingUtils.mapToPostCommentDto(post);
    }

    public void increaseViewCount(Post post) {
        if (post != null) {
            post.setViewCount(post.getViewCount() + 1);
            postRepository.save(post);
        }
    }

    public PostCountDto getAllPostByModerationStatus(int offset, int limit, ModerationStatus moderationStatus) {
        String sessionId = httpSession.getId();
        Long userId = mapSessions.getUserId(sessionId);

        return response(postRepository.findAllByIsActiveAndModerationStatusAndUserId((byte) 1,
                moderationStatus,
                userId,
                PageRequest.of(offset, limit, sort("recent"))));
    }

    public PostCountDto getAllPostByStatus(int offset, int limit, String status) {
        String sessionId = httpSession.getId();
        Long userId = mapSessions.getUserId(sessionId);

        switch (status) {
            case "pending":
                return response(postRepository.findAllByIsActiveAndModerationStatusAndUserId((byte) 1,
                        ModerationStatus.NEW,
                        userId,
                        PageRequest.of(offset, limit, sort("recent"))));
            case "declined":
                return response(postRepository.findAllByIsActiveAndModerationStatusAndUserId((byte) 1,
                        ModerationStatus.DECLINED,
                        userId,
                        PageRequest.of(offset, limit, sort("recent"))));
            case "published":
                return response(postRepository.findAllByIsActiveAndModerationStatusAndUserId((byte) 1,
                        ModerationStatus.ACCEPTED,
                        userId,
                        PageRequest.of(offset, limit, sort("recent"))));
            default:
                return response(postRepository.findAllByIsActiveAndModerationStatusAndUserId((byte) 0,
                        null,
                        userId,
                        PageRequest.of(offset, limit, sort("recent"))));
        }
    }

    //Добавление поста (с проверкой на ввод)
    public ErrorResponse addPost(NewPostDto newPostDto) {
        Post post = mappingUtils.mapNewPostDtoToPost(newPostDto);
        long postId = postRepository.save(post).getId();
        log.info("Добавлен новый пост");

        for (String itemTeg : Arrays.stream(newPostDto.getTags()).collect(Collectors.toList())) {
            Tag tag = tagService.findTagByName(itemTeg);

            //Добавление нового тэга в словарь, если его нет
            if (tag == null) {
                tag = new Tag();
                tag.setName(itemTeg);
                long tagId = tagService.save(tag).getId();
                log.info("Добавлен новый тэг - " + itemTeg);
            }

            //Добавление данных в связывающую таблицу Tag2Post
            Tag2Post tag2Post = new Tag2Post();
            tag2Post.setPostId(postId);
            tag2Post.setTagId(tag.getId());
            tagService.save(tag2Post);
        }

        ErrorResponse response = new ErrorResponse();
        response.setResult(true);
        response.setErrors(null);
        return response;
    }

    public ErrorResponse updatePost(Long postId, NewPostDto newPostDto) {
        Post post = mappingUtils.mapPostDtoUpdateToPost(postId, newPostDto);
        postRepository.save(post);
        ErrorResponse response = new ErrorResponse();
        response.setResult(true);
        response.setErrors(null);
        return response;
    }
}