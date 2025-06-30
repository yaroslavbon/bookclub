package ca.yarbond.bookclub.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("textUtils")
public class TextUtils {

    @Value("${app.ui.book-description-max-length:200}")
    private int bookDescriptionMaxLength;

    /**
     * Truncates book description text to the configured maximum length without cutting words.
     * If the text is longer than the configured limit, it will be truncated at the last complete word
     * and "..." will be appended.
     * 
     * @param text the text to truncate
     * @return the truncated text or original text if it's shorter than the configured limit
     */
    public String truncateBookDescription(String text) {
        if (text == null || text.length() <= bookDescriptionMaxLength) {
            return text;
        }
        
        // Find the last space before the max length to avoid cutting words
        int cutIndex = bookDescriptionMaxLength;
        while (cutIndex > 0 && !Character.isWhitespace(text.charAt(cutIndex))) {
            cutIndex--;
        }
        
        // If no space found, cut at max length (edge case)
        if (cutIndex == 0) {
            cutIndex = Math.min(bookDescriptionMaxLength, text.length());
        }
        
        return text.substring(0, cutIndex).trim() + "...";
    }
}