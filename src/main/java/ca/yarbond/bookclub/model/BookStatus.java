package ca.yarbond.bookclub.model;

public enum BookStatus {
    WISHLIST,   // Book is on someone's wishlist
    CURRENT,    // Currently being read by the club
    NEXT,       // Selected as the next book to read
    COMPLETED;   // Book has been read by the club

    public static BookStatus fromString(String status){
        if(status == null || status.trim().isEmpty()) return null;

        try{
            return valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}