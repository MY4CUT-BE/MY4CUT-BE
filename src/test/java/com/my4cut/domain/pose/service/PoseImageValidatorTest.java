package com.my4cut.domain.pose.service;

import com.my4cut.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoseImageValidatorTest {

    private final PoseImageValidator validator = new PoseImageValidator();

    @Test
    void acceptsDecodableJpegPngAndValidWebpContainer() {
        assertThatCode(() -> validator.validate(file("pose.jpg", "image/jpeg", image("jpg"))))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(file("pose.png", "image/png", image("png"))))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(file("pose.webp", "image/webp",
                minimalWebpContainer())))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsEmptyUnsupportedAndSpoofedFiles() {
        assertThatThrownBy(() -> validator.validate(file("empty.jpg", "image/jpeg", new byte[0])))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> validator.validate(file("pose.gif", "image/gif", new byte[]{1, 2, 3})))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> validator.validate(file("fake.jpg", "image/jpeg", new byte[]{1, 2, 3})))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> validator.validate(file("truncated.jpg", "image/jpeg",
                        new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00})))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsFilesLargerThanFiveMegabytes() {
        byte[] oversized = new byte[(int) PoseImageValidator.MAX_IMAGE_SIZE + 1];
        oversized[0] = (byte) 0xff;
        oversized[1] = (byte) 0xd8;
        oversized[2] = (byte) 0xff;

        assertThatThrownBy(() -> validator.validate(file("large.jpg", "image/jpeg", oversized)))
                .isInstanceOf(BusinessException.class);
    }

    private MockMultipartFile file(String name, String contentType, byte[] bytes) {
        return new MockMultipartFile("image", name, contentType, bytes);
    }

    private byte[] image(String format) {
        try {
            BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, format, output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private byte[] minimalWebpContainer() {
        return new byte[]{
                'R', 'I', 'F', 'F',
                14, 0, 0, 0,
                'W', 'E', 'B', 'P',
                'V', 'P', '8', 'X',
                2, 0, 0, 0,
                0, 0
        };
    }
}
