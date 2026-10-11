package com.thuanbui.foundry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

public class I18nTest {

    private ResourceBundleMessageSource messageSource;

    @BeforeEach
    void setUp() {
        messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setDefaultLocale(Locale.forLanguageTag("vi"));
        messageSource.setFallbackToSystemLocale(false);
    }

    @Test
    void vietnameseMessagesReturnedByDefault() {
        String message = messageSource.getMessage("error.validation", null, Locale.forLanguageTag("vi"));
        assertEquals("Dữ liệu không hợp lệ", message);
    }

    @Test
    void englishMessagesReturnedForAcceptLanguageEn() {
        String message = messageSource.getMessage("error.validation", null, Locale.forLanguageTag("en"));
        assertEquals("Invalid request data", message);
    }

    @Test
    void fallbackToVietnameseForUnsupportedLocale() {
        String message = messageSource.getMessage("error.validation", null, Locale.forLanguageTag("fr"));
        assertEquals("Dữ liệu không hợp lệ", message);
    }

    @Test
    void noteNotFoundMessageResolved() {
        assertEquals("Không tìm thấy ghi chú",
                messageSource.getMessage("error.note.not_found", null, Locale.forLanguageTag("vi")));
        assertEquals("Note not found",
                messageSource.getMessage("error.note.not_found", null, Locale.forLanguageTag("en")));
    }

    @Test
    void validationMessagesResolved() {
        assertEquals("Số điện thoại không đúng định dạng",
                messageSource.getMessage("validation.phone.invalid", null, Locale.forLanguageTag("vi")));
        assertEquals("Invalid phone number format",
                messageSource.getMessage("validation.phone.invalid", null, Locale.forLanguageTag("en")));
    }
}
