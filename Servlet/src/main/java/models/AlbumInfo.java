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
public class AlbumInfo {
    private String artist;
    private String title;
    private String year;
}
