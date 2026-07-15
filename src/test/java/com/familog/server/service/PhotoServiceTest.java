package com.familog.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.familog.server.domain.GenerationStatus;
import com.familog.server.domain.Member;
import com.familog.server.domain.Message;
import com.familog.server.domain.MessageType;
import com.familog.server.domain.Photo;
import com.familog.server.dto.response.PhotoResponse;
import com.familog.server.repository.MemberRepository;
import com.familog.server.repository.MessageRepository;
import com.familog.server.repository.PhotoRepository;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

    @Mock PhotoRepository photoRepository;
    @Mock MemberRepository memberRepository;
    @Mock MessageRepository messageRepository;
    @Mock FileStorageService fileStorageService;

    @InjectMocks PhotoService photoService;

    @Test
    void 갤러리는_직접업로드와_채팅이미지를_병합해_최신순으로_반환한다() {
        Member uploader = member(1L, "엄마");

        Photo photo = photo(10L, uploader, "http://f/photos/a.jpg", "강남역",
                LocalDate.of(2026, 7, 15), LocalDateTime.of(2026, 7, 15, 9, 0));
        Message chatImage = imageMessage(7L, uploader, "http://f/messages/7.jpg",
                LocalDateTime.of(2026, 7, 15, 12, 0));

        when(photoRepository.findByGroupIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(photo));
        when(messageRepository.findByGroupIdAndTypeOrderByCreatedAtDesc(1L, MessageType.IMAGE))
                .thenReturn(List.of(chatImage));

        List<PhotoResponse> result = photoService.list(1L, null);

        // 채팅 이미지(12:00) → 직접 업로드(09:00) 순으로 병합·정렬
        assertThat(result).hasSize(2);
        assertThat(result.get(0).photoId()).isEqualTo(-7L);          // 채팅 유래 = 메시지 id의 음수
        assertThat(result.get(0).imageUrl()).isEqualTo("http://f/messages/7.jpg");
        assertThat(result.get(0).location()).isNull();               // 채팅 유래는 위치 없음
        assertThat(result.get(1).photoId()).isEqualTo(10L);
        assertThat(result.get(1).location()).isEqualTo("강남역");
    }

    @Test
    void date_필터는_채팅이미지에도_적용되고_imageUrl_없는_메시지는_제외한다() {
        Member uploader = member(1L, "아빠");
        LocalDate day = LocalDate.of(2026, 7, 15);

        Message sameDay = imageMessage(7L, uploader, "http://f/messages/7.jpg",
                LocalDateTime.of(2026, 7, 15, 12, 0));
        Message otherDay = imageMessage(8L, uploader, "http://f/messages/8.jpg",
                LocalDateTime.of(2026, 7, 16, 12, 0));
        Message noUrl = imageMessage(9L, uploader, null,
                LocalDateTime.of(2026, 7, 15, 13, 0));

        when(photoRepository.findByGroupIdAndTakenDateOrderByCreatedAtDesc(1L, day)).thenReturn(List.of());
        when(messageRepository.findByGroupIdAndTypeOrderByCreatedAtDesc(1L, MessageType.IMAGE))
                .thenReturn(List.of(sameDay, otherDay, noUrl));

        List<PhotoResponse> result = photoService.list(1L, day);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).photoId()).isEqualTo(-7L);
    }

    // --- fixtures: id·createdAt은 JPA가 채우므로 테스트에선 리플렉션으로 세팅 ---

    private Member member(Long id, String name) {
        Member m = Member.builder().name(name).build();
        setField(m, "id", id);
        return m;
    }

    private Photo photo(Long id, Member uploader, String imageUrl, String location,
                        LocalDate takenDate, LocalDateTime createdAt) {
        Photo p = Photo.builder().uploader(uploader).imageUrl(imageUrl)
                .location(location).takenDate(takenDate).build();
        setField(p, "id", id);
        setField(p, "createdAt", createdAt);
        return p;
    }

    private Message imageMessage(Long id, Member sender, String imageUrl, LocalDateTime createdAt) {
        Message msg = Message.builder().sender(sender).type(MessageType.IMAGE)
                .imageUrl(imageUrl).convertStatus(GenerationStatus.READY).build();
        setField(msg, "id", id);
        setField(msg, "createdAt", createdAt);
        return msg;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Class<?> c = target.getClass();
            Field f = null;
            while (c != null && f == null) {
                try {
                    f = c.getDeclaredField(name);
                } catch (NoSuchFieldException e) {
                    c = c.getSuperclass();
                }
            }
            if (f == null) throw new IllegalStateException("no field " + name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
