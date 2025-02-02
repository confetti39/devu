package com.devu.backend.service;

import com.devu.backend.api.comment.CommentResponseDto;
import com.devu.backend.api.status.ResponseStatusDto;
import com.devu.backend.common.exception.PostNotFoundException;
import com.devu.backend.common.exception.UserNotFoundException;
import com.devu.backend.common.exception.UserNotMatchException;
import com.devu.backend.config.s3.S3Uploader;
import com.devu.backend.controller.post.PostRequestCreateDto;
import com.devu.backend.controller.post.PostRequestUpdateDto;
import com.devu.backend.controller.post.PostResponseDto;
import com.devu.backend.entity.Image;
import com.devu.backend.entity.PostTag;
import com.devu.backend.entity.Tag;
import com.devu.backend.entity.User;
import com.devu.backend.entity.post.*;
import com.devu.backend.repository.ImageRepository;
import com.devu.backend.repository.post.PostRepository;
import com.devu.backend.repository.post.PostSearch;
import com.devu.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final S3Uploader s3Uploader;
    private final ImageRepository imageRepository;
    private final TagService tagService;

    private List<String> getImageUrl(Post post) {
        List<String> images = new ArrayList<>();
        for (Image image : post.getImages()) {
            images.add(image.getPath());
        }
        return images;
    }

    /*
    * images,tags 엔티티가 생성되지 않은 시점에서 응답으로 필요한 데이터라서 new ArrayList<>로 직접 넣어줘야함
    * 즉,Image 엔티티와 Tag 엔티티가 생성되기 전에 chat 인스턴스에서 접근해서 우선적으로 만드는것
    * likes => list 넣어줄 필요 없음
    * */
    @Transactional
    public PostResponseDto createChat(PostRequestCreateDto requestPostDto) throws IOException {
        User user = userRepository.findByUsername(requestPostDto.getUsername())
                .orElseThrow(UserNotFoundException::new);
        Chat chat = Chat.builder()
                .user(user)
                .title(requestPostDto.getTitle())
                .content(requestPostDto.getContent())
                .hit(0L)
                .images(new ArrayList<>())
                .build();
        addImage(requestPostDto, chat);
        log.info("Create Chat {} By {}",chat.getTitle(),chat.getUser().getUsername());
        postRepository.save(chat);
        user.addPost(chat);
        return PostResponseDto.builder()
                .title(chat.getTitle())
                .url(getImageUrl(chat))
                .username(chat.getUser().getUsername())
                .build();
    }

    @Transactional
    public PostResponseDto createStudy(PostRequestCreateDto requestPostDto) throws IOException {
        List<PostTag> postTags = new ArrayList<>();
        List<Tag> tags = createTags(requestPostDto, postTags);
        User user = userRepository.findByUsername(requestPostDto.getUsername())
                .orElseThrow(UserNotFoundException::new);
        Study study = Study.builder()
                .user(user)
                .title(requestPostDto.getTitle())
                .content(requestPostDto.getContent())
                .studyStatus(StudyStatus.ACTIVE)
                .hit(0L)
                .images(new ArrayList<>())
                .tags(postTags)
                .build();
        addImage(requestPostDto, study);
        setPostOnPostTag(postTags,study);
        log.info("Create Study {} By {}",study.getTitle(),study.getUser().getUsername());
        postRepository.save(study);
        user.addPost(study);
        return PostResponseDto.builder()
                .title(study.getTitle())
                .url(getImageUrl(study))
                .username(study.getUser().getUsername())
                .tags(tags.stream().map(Tag::getName).collect(Collectors.toList()))
                .build();
    }

    @Transactional
    public PostResponseDto createQuestion(PostRequestCreateDto requestPostDto) throws IOException {
        List<PostTag> postTags = new ArrayList<>();
        List<Tag> tags = createTags(requestPostDto, postTags);
        User user = userRepository.findByUsername(requestPostDto.getUsername())
                .orElseThrow(UserNotFoundException::new);
        Question question = Question.builder()
                .user(user)
                .title(requestPostDto.getTitle())
                .content(requestPostDto.getContent())
                .qnaStatus(QuestionStatus.UNSOLVED)
                .hit(0L)
                .images(new ArrayList<>())
                .tags(postTags)
                .build();
        addImage(requestPostDto, question);
        setPostOnPostTag(postTags,question);
        log.info("Create Question {} By {}",question.getTitle(),question.getUser().getUsername());
        postRepository.save(question);
        user.addPost(question);
        return PostResponseDto.builder()
                .title(question.getTitle())
                .url(getImageUrl(question))
                .username(question.getUser().getUsername())
                .tags(tags.stream().map(Tag::getName).collect(Collectors.toList()))
                .build();
    }

    private void setPostOnPostTag(List<PostTag> postTags, Post post) {
        for (PostTag postTag : postTags) {
            postTag.changePost(post);
        }
    }

    private List<Tag> createTags(PostRequestCreateDto requestPostDto, List<PostTag> postTags) {
        List<Tag> tags = tagService.findTags(
                requestPostDto.getTags()
                        .stream().map(String::toUpperCase)
                        .collect(Collectors.toList()));
        for (Tag tag : tags) {
            postTags.add(PostTag.builder().tag(tag).build());
        }
        return tags;
    }

    @Transactional
    public void addImage(PostRequestCreateDto requestPostDto, Post post) throws IOException {
        if (!CollectionUtils.isEmpty(requestPostDto.getImages())) {
            for (MultipartFile file : requestPostDto.getImages()) {
                String url = s3Uploader.upload(file, "static", post);
                log.info("s3 생성 {}", url);
            }
        }
    }

    private String getTagNameFromPostTags(PostTag postTag) {
        return tagService.findTagName(postTag);
    }


    public Page<PostResponseDto> findAllChats(Pageable pageable,String order,String s) {
        PostSearch postSearch = PostSearch.builder()
                .sentence(s)
                .order(order)
                .build();
        return postRepository.findAllChats(pageable, postSearch).map(
                chat -> PostResponseDto
                        .builder()
                        .id(chat.getId())
                        .title(chat.getTitle())
                        .content(chat.getContent())
                        .username(chat.getUser().getUsername())
                        .hit(chat.getHit())
                        .like(chat.getLikes().size())
                        .commentsSize(chat.getComments().size())
                        .createAt(chat.getCreateAt())
                        .lastModifiedAt(chat.getLastModifiedAt())
                        .build()
        );
    }

    public Page<PostResponseDto> findAllStudies(Pageable pageable,StudyStatus status,String order,List<String> tags,String s) {
        PostSearch postSearch = PostSearch.builder()
                .order(order)
                .sentence(s)
                .tagId(Optional.ofNullable(tags).orElseGet(Collections::emptyList).stream().map(tagService::findTagIdByString).collect(Collectors.toList()))
                .studyStatus(status)
                .build();

        return postRepository.findAllStudies(pageable,postSearch).map(
                study -> PostResponseDto
                        .builder()
                        .id(study.getId())
                        .title(study.getTitle())
                        .content(study.getContent())
                        .username(study.getUser().getUsername())
                        .hit(study.getHit())
                        .studyStatus(study.getStudyStatus())
                        .like(study.getLikes().size())
                        .tags(study.getPostTags().stream().map(this::getTagNameFromPostTags).collect(Collectors.toList()))
                        .commentsSize(study.getComments().size())
                        .createAt(study.getCreateAt())
                        .lastModifiedAt(study.getLastModifiedAt())
                        .build()
        );
    }

    public Page<PostResponseDto> findAllQuestions(Pageable pageable,QuestionStatus status,String order,List<String> tags,String s) {
        PostSearch postSearch = PostSearch.builder()
                .order(order)
                .sentence(s)
                .tagId(Optional.ofNullable(tags).orElseGet(Collections::emptyList).stream().map(tagService::findTagIdByString).collect(Collectors.toList()))
                .questionStatus(status)
                .build();
        return postRepository.findAllQuestions(pageable,postSearch).map(
                question -> PostResponseDto
                        .builder()
                        .id(question.getId())
                        .title(question.getTitle())
                        .content(question.getContent())
                        .username(question.getUser().getUsername())
                        .hit(question.getHit())
                        .questionStatus(question.getQuestionStatus())
                        .like(question.getLikes().size())
                        .tags(question.getPostTags().stream().map(this::getTagNameFromPostTags).collect(Collectors.toList()))
                        .commentsSize(question.getComments().size())
                        .createAt(question.getCreateAt())
                        .lastModifiedAt(question.getLastModifiedAt())
                        .build()
        );
    }

    @Transactional
    public PostResponseDto findChatById(Long id) {
        log.info("Selected Chat ID : {}",id);
        Chat chat = postRepository.findChatById(id).orElseThrow(PostNotFoundException::new);
        log.info("Selected Chat Title : {}", chat.getTitle());
        chat.plusHit();
        log.info("Current Hit : {}", chat.getHit());
        log.info("Current Like : {}", chat.getLikes().size());
        return PostResponseDto.builder()
                .id(chat.getId())
                .hit(chat.getHit())
                .username(chat.getUser().getUsername())
                .content(chat.getContent())
                .title(chat.getTitle())
                .like(chat.getLikes().size())
                .comments(
                        chat.getComments().stream()
                                .map(comment -> CommentResponseDto.builder()
                                        .username(comment.getUser().getUsername())
                                        .contents(comment.getContents())
                                        .commentId(comment.getId())
                                        .deleted(comment.isDeleted())
                                        .group(comment.getGroupNum())
                                        .parent(comment.getParent())
                                        .lastModifiedAt(comment.getLastModifiedAt())
                                        .createAt(comment.getCreateAt())
                                        .build()
                                ).collect(Collectors.toList())
                )
                .tags(chat.getPostTags().stream().map(this::getTagNameFromPostTags).collect(Collectors.toList()))
                .createAt(chat.getCreateAt())
                .url(chat.getImages().stream().map(Image::getPath).collect(Collectors.toList()))
                .build();
    }

    @Transactional
    public PostResponseDto findStudyById(Long id) {
        log.info("Selected Study ID : {}",id);
        Study study = postRepository.findStudyById(id).orElseThrow(PostNotFoundException::new);
        log.info("Selected Study Title : {}", study.getTitle());
        study.plusHit();
        log.info("Current Hit : {}", study.getHit());
        log.info("Current Like : {}", study.getLikes().size());
        return PostResponseDto.builder()
                .id(study.getId())
                .hit(study.getHit())
                .username(study.getUser().getUsername())
                .content(study.getContent())
                .title(study.getTitle())
                .studyStatus(study.getStudyStatus())
                .like(study.getLikes().size())
                .comments(
                        study.getComments().stream()
                                .map(comment -> CommentResponseDto.builder()
                                        .username(comment.getUser().getUsername())
                                        .contents(comment.getContents())
                                        .commentId(comment.getId())
                                        .deleted(comment.isDeleted())
                                        .group(comment.getGroupNum())
                                        .parent(comment.getParent())
                                        .createAt(comment.getCreateAt())
                                        .lastModifiedAt(comment.getLastModifiedAt())
                                        .build()
                                ).collect(Collectors.toList())
                )
                .tags(study.getPostTags().stream().map(this::getTagNameFromPostTags).collect(Collectors.toList()))
                .createAt(study.getCreateAt())
                .url(study.getImages().stream().map(Image::getPath).collect(Collectors.toList()))
                .build();
    }

    @Transactional
    public PostResponseDto findQuestionById(Long id) {
        log.info("Selected Question ID : {}",id);
        Question question = postRepository.findQuestionById(id).orElseThrow(PostNotFoundException::new);
        log.info("Selected Question Title : {}", question.getTitle());
        question.plusHit();
        log.info("Current Hit : {}", question.getHit());
        log.info("Current Like : {}", question.getLikes().size());
        return PostResponseDto.builder()
                .id(question.getId())
                .hit(question.getHit())
                .username(question.getUser().getUsername())
                .content(question.getContent())
                .title(question.getTitle())
                .questionStatus(question.getQuestionStatus())
                .like(question.getLikes().size())
                .comments(
                        question.getComments().stream()
                                .map(comment -> CommentResponseDto.builder()
                                        .username(comment.getUser().getUsername())
                                        .contents(comment.getContents())
                                        .commentId(comment.getId())
                                        .deleted(comment.isDeleted())
                                        .group(comment.getGroupNum())
                                        .parent(comment.getParent())
                                        .createAt(comment.getCreateAt())
                                        .lastModifiedAt(comment.getLastModifiedAt())
                                        .build()
                                ).collect(Collectors.toList())
                )
                .tags(question.getPostTags().stream().map(this::getTagNameFromPostTags).collect(Collectors.toList()))
                .createAt(question.getCreateAt())
                .url(question.getImages().stream().map(Image::getPath).collect(Collectors.toList()))
                .build();
    }

    @Transactional
    public void updateImage(Post post, PostRequestUpdateDto updateDto) throws IOException {
        List<Image> dbImages = post.getImages();
        for (Image image : dbImages) {
            s3Uploader.delete(image.getName());
            log.info("{}", image);
            imageRepository.delete(image);
            log.info("업데이트 삭제");
        }
        for (MultipartFile multipartFile : updateDto.getImages()) {
            s3Uploader.upload(multipartFile, "static", post);
            log.info("업데이트 추가");
        }
    }

    @Transactional
    public void updateChat(Long chatId, PostRequestUpdateDto updateDto) throws IOException {
        Chat chat = postRepository.findChatById(chatId).orElseThrow(PostNotFoundException::new);
        if (!updateDto.getImages().isEmpty()) {
            updateImage(chat, updateDto);
        }
        if (!isSameTags(chat.getPostTags(), updateDto.getTags().stream().map(String::toUpperCase).collect(Collectors.toList()))) {
            updateTags(updateDto, chat);
        }
        chat.updatePost(updateDto);
    }

    @Transactional
    public void updateStudy(Long studyId, PostRequestUpdateDto updateDto) throws IOException {
        Study study = postRepository.findStudyById(studyId).orElseThrow(PostNotFoundException::new);
        updateImage(study, updateDto);
        if (!isSameTags(study.getPostTags(), updateDto.getTags().stream().map(String::toUpperCase).collect(Collectors.toList()))) {
            updateTags(updateDto, study);
        }
        study.updatePost(updateDto);
    }

    @Transactional
    public void updateQuestion(Long questionId, PostRequestUpdateDto updateDto) throws IOException {
        Question question = postRepository.findQuestionById(questionId).orElseThrow(PostNotFoundException::new);
        updateImage(question, updateDto);
        if (!isSameTags(question.getPostTags(), updateDto.getTags().stream().map(String::toUpperCase).collect(Collectors.toList()))) {
            updateTags(updateDto, question);
        }
        question.updatePost(updateDto);
    }

    private void updateTags(PostRequestUpdateDto updateDto, Post post) {
        clearPostTagList(post);
        List<Tag> tags = tagService.findTags(updateDto.getTags().stream().map(String::toUpperCase).collect(Collectors.toList()));
        List<PostTag> collect = tags.stream().map(t -> PostTag.builder().tag(t).post(post).build()).collect(Collectors.toList());
        for (PostTag postTag : collect) {
            post.getPostTags().add(postTag);
        }
    }

    private void clearPostTagList(Post post) {
        post.getPostTags().clear();
    }

    private boolean isSameTags(List<PostTag> postTags,List<String> input) {
        for (PostTag postTag : postTags) {
            String tagName = tagService.findTagName(postTag);
            if (!input.contains(tagName)) {
                return false;
            }
        }
        return true;
    }

    @Transactional
    public void deleteChat(Chat chat) {
        deleteImage(chat);
        postRepository.delete(chat);
    }

    @Transactional
    public void deleteImage(Post post) {
        List<Image> dbImages = post.getImages();
        for (Image image : dbImages) {
            s3Uploader.delete(image.getName());
            imageRepository.delete(image);
        }
        log.info("전체 삭제");
    }

    @Transactional
    public void deleteStudy(Study study) {
        deleteImage(study);
        postRepository.delete(study);
    }

    @Transactional
    public void deleteQuestion(Question question) {
        deleteImage(question);
        postRepository.delete(question);
    }

    @Transactional
    public ResponseStatusDto updateStudyStatus(Long studyId,String username) {
        Study study = postRepository.findStudyById(studyId).orElseThrow(PostNotFoundException::new);
        isOwner(study, username);
        if (study.getStudyStatus() == StudyStatus.ACTIVE) {
            study.updateStatus(StudyStatus.CLOSED);
            return ResponseStatusDto.builder()
                    .studyStatus(study.getStudyStatus())
                    .id(study.getId())
                    .build();
        }
        study.updateStatus(StudyStatus.ACTIVE);
        return ResponseStatusDto.builder()
                .studyStatus(study.getStudyStatus())
                .id(study.getId())
                .build();
    }

    @Transactional
    public ResponseStatusDto updateQuestionStatus(Long questionId,String username) {
        Question question = postRepository.findQuestionById(questionId).orElseThrow(PostNotFoundException::new);
        isOwner(question, username);
        if (question.getQuestionStatus() == QuestionStatus.UNSOLVED) {
            question.updateStatus(QuestionStatus.SOLVED);
            return ResponseStatusDto.builder()
                    .questionStatus(question.getQuestionStatus())
                    .id(question.getId())
                    .build();
        }
        question.updateStatus(QuestionStatus.UNSOLVED);
        return ResponseStatusDto.builder()
                .questionStatus(question.getQuestionStatus())
                .id(question.getId())
                .build();
    }

    private void isOwner(Post post, String username) {
        if (!post.getUser().getUsername().equals(username)) {
            throw new UserNotMatchException();
        }
    }

    public List<PostResponseDto> getTop3ChatByHits() {
        return postRepository.findTop3ChatByOrderByHitDesc()
                .orElseThrow(PostNotFoundException::new)
                .stream().map(c -> PostResponseDto.builder()
                        .id(c.getId())
                        .title(c.getTitle())
                        .hit(c.getHit())
                        .content(c.getContent())
                        .like(c.getLikes().size())
                        .tags(c.getPostTags().stream().map(this::getTagNameFromPostTags).collect(Collectors.toList()))
                        .username(c.getUser().getUsername())
                        .build()
                ).collect(Collectors.toList());
    }

    public List<PostResponseDto> getTop3ChatByLikes() {
        return postRepository.findTop3ChatByOrderByLikes().orElseThrow(PostNotFoundException::new)
                .stream().map(c -> PostResponseDto.builder()
                        .id(c.getId())
                        .title(c.getTitle())
                        .hit(c.getHit())
                        .content(c.getContent())
                        .like(c.getLikes().size())
                        .tags(c.getPostTags().stream().map(this::getTagNameFromPostTags).collect(Collectors.toList()))
                        .username(c.getUser().getUsername())
                        .build()
                ).collect(Collectors.toList());
    }

    public List<PostResponseDto> getTop3StudyByHits() {
        return postRepository.findTop3StudyByOrderByHitDesc().orElseThrow(PostNotFoundException::new)
                .stream().map(s -> PostResponseDto.builder()
                        .id(s.getId())
                        .title(s.getTitle())
                        .hit(s.getHit())
                        .content(s.getContent())
                        .like(s.getLikes().size())
                        .tags(s.getPostTags().stream().map(this::getTagNameFromPostTags).collect(Collectors.toList()))
                        .username(s.getUser().getUsername())
                        .build()
                ).collect(Collectors.toList());
    }

    public List<PostResponseDto> getTop3StudyByLikes() {
        return postRepository.findTop3StudyByOrderByLikes().orElseThrow(PostNotFoundException::new)
                .stream().map(s -> PostResponseDto.builder()
                        .id(s.getId())
                        .title(s.getTitle())
                        .hit(s.getHit())
                        .content(s.getContent())
                        .like(s.getLikes().size())
                        .tags(s.getPostTags().stream().map(this::getTagNameFromPostTags).collect(Collectors.toList()))
                        .username(s.getUser().getUsername())
                        .build()
                ).collect(Collectors.toList());
    }

    public List<PostResponseDto> getTop3QuestionByHits() {
        return postRepository.findTop3QuestionByOrderByHitDesc().orElseThrow(PostNotFoundException::new)
                .stream().map(q -> PostResponseDto.builder()
                        .id(q.getId())
                        .title(q.getTitle())
                        .hit(q.getHit())
                        .content(q.getContent())
                        .like(q.getLikes().size())
                        .tags(q.getPostTags().stream().map(this::getTagNameFromPostTags).collect(Collectors.toList()))
                        .username(q.getUser().getUsername())
                        .build()
                ).collect(Collectors.toList());
    }

    public List<PostResponseDto> getTop3QuestionByLikes() {
        return postRepository.findTop3QuestionByOrderByLikes().orElseThrow(PostNotFoundException::new)
                .stream().map(q -> PostResponseDto.builder()
                        .id(q.getId())
                        .title(q.getTitle())
                        .hit(q.getHit())
                        .content(q.getContent())
                        .like(q.getLikes().size())
                        .tags(q.getPostTags().stream().map(this::getTagNameFromPostTags).collect(Collectors.toList()))
                        .username(q.getUser().getUsername())
                        .build()
                ).collect(Collectors.toList());
    }

    public int getAllChatSize() {
        return postRepository.findAllChatsWithoutSorting().size();
    }

    public int getAllStudiesSize() {
        return postRepository.findAllStudiesWithoutSorting().size();
    }

    public int getAllQuestionsSize() {
        return postRepository.findAllQuestionsWithoutSorting().size();
    }

    public List<PostResponseDto> findAllChatsByUser(User user) {
        return postRepository.findAllChatsByUser(user).orElseThrow(PostNotFoundException::new)
                .stream().map(c -> PostResponseDto.builder()
                        .id(c.getId())
                        .hit(c.getHit())
                        .like(c.getLikes().size())
                        .commentsSize(c.getComments().size())
                        .title(c.getTitle())
                        .content(c.getContent())
                        .createAt(c.getCreateAt())
                        .lastModifiedAt(c.getLastModifiedAt())
                        .build()
                ).collect(Collectors.toList());
    }

    public List<PostResponseDto> findAllStudiesByUser(User user) {
        return postRepository.findAllStudiesByUser(user).orElseThrow(PostNotFoundException::new)
                .stream().map(s -> PostResponseDto.builder()
                        .id(s.getId())
                        .hit(s.getHit())
                        .like(s.getLikes().size())
                        .commentsSize(s.getComments().size())
                        .title(s.getTitle())
                        .content(s.getContent())
                        .createAt(s.getCreateAt())
                        .lastModifiedAt(s.getLastModifiedAt())
                        .tags(s.getPostTags().stream().map(pt -> pt.getTag().getName()).collect(Collectors.toList()))
                        .studyStatus(s.getStudyStatus())
                        .build()
                ).collect(Collectors.toList());
    }

    public List<PostResponseDto> findAllQuestionsByUser(User user) {
        return postRepository.findAllQuestionsByUser(user).orElseThrow(PostNotFoundException::new)
                .stream().map(q -> PostResponseDto.builder()
                        .id(q.getId())
                        .hit(q.getHit())
                        .like(q.getLikes().size())
                        .commentsSize(q.getComments().size())
                        .title(q.getTitle())
                        .content(q.getContent())
                        .createAt(q.getCreateAt())
                        .lastModifiedAt(q.getLastModifiedAt())
                        .tags(q.getPostTags().stream().map(pt -> pt.getTag().getName()).collect(Collectors.toList()))
                        .questionStatus(q.getQuestionStatus())
                        .build()
                ).collect(Collectors.toList());
    }

    public List<PostResponseDto> findAllLikeChatsByUser(User user) {
        return postRepository.findAllLikeChatsByUserId(user.getId()).orElseThrow(PostNotFoundException::new)
                .stream().map(c -> PostResponseDto.builder()
                        .id(c.getId())
                        .hit(c.getHit())
                        .like(c.getLikes().size())
                        .commentsSize(c.getComments().size())
                        .title(c.getTitle())
                        .content(c.getContent())
                        .createAt(c.getCreateAt())
                        .lastModifiedAt(c.getLastModifiedAt())
                        .build()).collect(Collectors.toList());
    }

    public List<PostResponseDto> findAllLikeStudiesByUser(User user) {
        return postRepository.findAllLikeStudiesByUserId(user.getId()).orElseThrow(PostNotFoundException::new)
                .stream().map(s -> PostResponseDto.builder()
                        .id(s.getId())
                        .hit(s.getHit())
                        .like(s.getLikes().size())
                        .commentsSize(s.getComments().size())
                        .title(s.getTitle())
                        .content(s.getContent())
                        .createAt(s.getCreateAt())
                        .lastModifiedAt(s.getLastModifiedAt())
                        .studyStatus(s.getStudyStatus())
                        .tags(s.getPostTags().stream().map(pt -> pt.getTag().getName()).collect(Collectors.toList()))
                        .build()).collect(Collectors.toList());
    }

    public List<PostResponseDto> findAllLikeQuestionsByUser(User user) {
        return postRepository.findAllLikeQuestionsByUserId(user.getId()).orElseThrow(PostNotFoundException::new)
                .stream().map(q -> PostResponseDto.builder()
                        .id(q.getId())
                        .hit(q.getHit())
                        .like(q.getLikes().size())
                        .commentsSize(q.getComments().size())
                        .title(q.getTitle())
                        .content(q.getContent())
                        .createAt(q.getCreateAt())
                        .lastModifiedAt(q.getLastModifiedAt())
                        .questionStatus(q.getQuestionStatus())
                        .tags(q.getPostTags().stream().map(pt -> pt.getTag().getName()).collect(Collectors.toList()))
                        .build()).collect(Collectors.toList());
    }
}

