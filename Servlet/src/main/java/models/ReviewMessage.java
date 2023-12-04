package models;

import lombok.*;

/**
 * @author xiaorui
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ReviewMessage {
    private int albumId;
    private String message;

}
