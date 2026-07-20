package com.my4cut.domain.pose.service;

import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

@Component
public class PoseImageValidator {

    static final long MAX_IMAGE_SIZE = 5L * 1024L * 1024L;
    private static final long MAX_IMAGE_PIXELS = 25_000_000L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    public void validate(MultipartFile image) {
        if (image == null || image.isEmpty() || image.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        try {
            byte[] bytes = image.getBytes();
            if (!matchesContent(bytes, contentType.toLowerCase())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST);
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, exception);
        }
    }

    private boolean matchesContent(byte[] bytes, String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> isJpeg(bytes) && canDecode(bytes, "JPEG");
            case "image/png" -> isPng(bytes) && canDecode(bytes, "PNG");
            case "image/webp" -> isWebp(bytes) && canDecodeWebpIfSupported(bytes);
            default -> false;
        };
    }

    private boolean canDecode(byte[] bytes, String expectedFormat) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                return false;
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return false;
            }

            ImageReader reader = readers.next();
            try {
                String actualFormat = reader.getFormatName().toUpperCase(Locale.ROOT);
                if (!actualFormat.equals(expectedFormat)
                        && !(expectedFormat.equals("JPEG") && actualFormat.equals("JPG"))) {
                    return false;
                }

                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0
                        || height <= 0
                        || (long) width * height > MAX_IMAGE_PIXELS) {
                    return false;
                }

                BufferedImage decoded = reader.read(0);
                return decoded != null;
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && unsigned(bytes[0]) == 0xFF
                && unsigned(bytes[1]) == 0xD8
                && unsigned(bytes[2]) == 0xFF;
    }

    private boolean isPng(byte[] bytes) {
        int[] signature = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (bytes.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (unsigned(bytes[index]) != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean isWebp(byte[] bytes) {
        if (bytes.length < 20
                || bytes[0] != 'R'
                || bytes[1] != 'I'
                || bytes[2] != 'F'
                || bytes[3] != 'F'
                || bytes[8] != 'W'
                || bytes[9] != 'E'
                || bytes[10] != 'B'
                || bytes[11] != 'P') {
            return false;
        }

        long riffPayloadSize = littleEndianUnsignedInt(bytes, 4);
        if (riffPayloadSize != bytes.length - 8L) {
            return false;
        }

        String chunkType = new String(bytes, 12, 4, java.nio.charset.StandardCharsets.US_ASCII);
        if (!Set.of("VP8 ", "VP8L", "VP8X").contains(chunkType)) {
            return false;
        }

        long chunkSize = littleEndianUnsignedInt(bytes, 16);
        long paddedChunkSize = chunkSize + (chunkSize & 1L);
        return 20L + paddedChunkSize <= bytes.length;
    }

    private boolean canDecodeWebpIfSupported(byte[] bytes) {
        if (!ImageIO.getImageReadersByMIMEType("image/webp").hasNext()) {
            return true;
        }
        return canDecode(bytes, "WEBP");
    }

    private long littleEndianUnsignedInt(byte[] bytes, int offset) {
        return (long) unsigned(bytes[offset])
                | (long) unsigned(bytes[offset + 1]) << 8
                | (long) unsigned(bytes[offset + 2]) << 16
                | (long) unsigned(bytes[offset + 3]) << 24;
    }

    private int unsigned(byte value) {
        return Byte.toUnsignedInt(value);
    }
}
