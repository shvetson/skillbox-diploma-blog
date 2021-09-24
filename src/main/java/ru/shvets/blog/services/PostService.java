package ru.shvets.blog.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.shvets.blog.controllers.ApiAuthController;
import ru.shvets.blog.dto.PostCommentDto;
import ru.shvets.blog.dto.PostCountDto;
import ru.shvets.blog.dto.PostDto;
import ru.shvets.blog.exceptions.NoSuchPostException;
import ru.shvets.blog.models.ModerationStatus;
import ru.shvets.blog.models.Post;
import ru.shvets.blog.models.User;
import ru.shvets.blog.repositories.PostRepository;
import ru.shvets.blog.repositories.UserRepository;
import ru.shvets.blog.utils.MappingUtils;

import javax.servlet.http.HttpSession;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final MappingUtils mappingUtils;
    private final HttpSession httpSession;

    @Autowired
    public PostService(PostRepository postRepository, UserRepository userRepository, MappingUtils mappingUtils, HttpSession httpSession) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.mappingUtils = mappingUtils;
        this.httpSession = httpSession;
    }

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
        long userId = (long) httpSession.getAttribute("user");
        User user = userRepository.findUserById(userId);

        Post post = postRepository.findPostByIdAndAndIsActiveAndModerationStatus(postId, (byte) 1, ModerationStatus.ACCEPTED);

        if (post == null) {
            throw new NoSuchPostException("Записи с id=" + postId + " в базе данных нет.");
        }

        if (user.getIsModerator() != 1 & post.getUser().getId() != userId) {
            increaseViewCount(post);
        }
        return mappingUtils.mapToPostCommentDto(post);
    }

    public void increaseViewCount(Post post) {
        if (post != null) {
            post.setViewCount(post.getViewCount() + 1);
            postRepository.save(post);
        }
    }
}